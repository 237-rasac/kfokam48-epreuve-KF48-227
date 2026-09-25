package com.example.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.domaine.AssignedBy;
import com.example.backend.domaine.Etudiant;
import com.example.backend.domaine.Exercice;
import com.example.backend.domaine.Presence;
import com.example.backend.domaine.Relecture;
import com.example.backend.erreur.ErreurMetierException;
import com.example.backend.repository.ExerciceRepository;
import com.example.backend.repository.EtudiantRepository;
import com.example.backend.repository.PresenceRepository;
import com.example.backend.repository.RelectureRepository;

/**
 * Assignation d'un relecteur (EF4, RG3, RG4, RG5) :
 * - pool = étudiants présents à la session, hors auteur (RG3), hors étudiants
 *   déjà relecteurs d'un exercice EN_ATTENTE de la même session (répartition)
 * - RG8 : pool vide → l'exercice reste EN_ATTENTE sans relecture (aucune ligne
 *   relecture créée) ; le formateur le voit dans son tableau (MODULE 8)
 * - RG4 : un seul relecteur par exercice (contrainte uk_relecture_exercice)
 */
@Service
public class RelectureService {

    private final PresenceRepository presences;
    private final RelectureRepository relectures;
    private final ExerciceRepository exercices;
    private final EtudiantRepository etudiants;
    private final GenerateurCode hasard;

    public RelectureService(PresenceRepository presences, RelectureRepository relectures,
            ExerciceRepository exercices, EtudiantRepository etudiants, GenerateurCode hasard) {
        this.presences = presences;
        this.relectures = relectures;
        this.exercices = exercices;
        this.etudiants = etudiants;
        this.hasard = hasard;
    }

    /**
     * Assigne un relecteur au hasard parmi les présents éligibles (RG5).
     * Ne fait rien si le pool est vide (RG8) : l'exercice reste sans relecteur.
     */
    @Transactional
    public void assignerSiPossible(Exercice exercice) {
        Long sessionId = exercice.getSession().getId();

        // Le relecteur ne peut pas être l'auteur (RG3)
        Etudiant auteur = exercice.getEtudiant();
        List<Etudiant> presents = presences.findBySessionId(sessionId).stream()
                .map(Presence::getEtudiant)
                .filter(e -> !memeEtudiant(e, auteur))
                .toList();

        // Répartition : un étudiant déjà relecteur d'un exercice en attente de la
        // même session n'est pas réassigné (l'issue #17 demande ce filtrage)
        List<Etudiant> relecteursActifs = relectures.findBySessionIdAndRendueAtIsNull(sessionId).stream()
                .map(Relecture::getRelecteur)
                .toList();

        List<Etudiant> pool = presents.stream()
                .filter(e -> relecteursActifs.stream().noneMatch(r -> memeEtudiant(r, e)))
                .toList();

        if (pool.isEmpty()) {
            return; // RG8 : exercice EN_ATTENTE sans relecteur
        }

        Etudiant elu = pool.get(hasard.indiceAleatoire(pool.size()));
        relectures.save(new Relecture(exercice, elu, AssignedBy.SYSTEME));
    }

    /**
     * MODULE 10 (issue #22) — assignation manuelle du relecteur par le formateur :
     * débloque un exercice resté sans relecteur (RG8).
     * - relecteurId = auteur → 400 (RG3)
     * - exercice déjà avec relecteur → 409 EXERCICE_DEJA_DEPOSE ? Non : 409 dédié
     * - exercice inconnu / relecteur inconnu → 404
     * L'exercice reste EN_ATTENTE (statut contractuel) ; la relecture créée porte
     * assignedBy = FORMATEUR.
     */
    @Transactional
    public Relecture assignerManuellement(Long exerciceId, Long relecteurId) {
        Exercice exercice = exercices.findById(exerciceId)
                .orElseThrow(() -> new ErreurMetierException("EXERCICE_INCONNU", 404, "Exercice inconnu."));
        Etudiant relecteur = etudiants.findById(relecteurId)
                .orElseThrow(() -> new ErreurMetierException("ETUDIANT_INCONNU", 404, "Étudiant inconnu."));

        // RG3 : le relecteur ne peut pas être l'auteur (400, cf. issue #22)
        if (memeEtudiant(relecteur, exercice.getEtudiant())) {
            throw new ErreurMetierException("AUTO_RELECTURE", 400,
                    "Le relecteur ne peut pas être l'auteur de l'exercice.");
        }
        // RG4 : un seul relecteur par exercice
        if (relectures.findByExerciceId(exerciceId).isPresent()) {
            throw new ErreurMetierException("RELECTURE_DEJA_ASSIGNEE", 409,
                    "Cet exercice a déjà un relecteur.");
        }
        // L'issue #22 vise les exercices sans relecteur ; la session clôturée reste
        // une borne : plus aucune action n'est acceptée après clôture (EF8)
        if (exercice.getSession().estCloturee()) {
            throw new ErreurMetierException("SESSION_CLOTUREE", 410, "La session est clôturée.");
        }

        return relectures.save(new Relecture(exercice, relecteur, AssignedBy.FORMATEUR));
    }

    /** Même étudiant : par id si les deux sont persistés, sinon par référence (tests unitaires). */
    private static boolean memeEtudiant(Etudiant a, Etudiant b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return a == b;
    }
}
