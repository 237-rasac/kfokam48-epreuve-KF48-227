package com.example.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.domaine.Etudiant;
import com.example.backend.domaine.Exercice;
import com.example.backend.domaine.Presence;
import com.example.backend.domaine.Relecture;
import com.example.backend.repository.ExerciceRepository;
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
    private final GenerateurCode hasard;

    public RelectureService(PresenceRepository presences, RelectureRepository relectures,
            GenerateurCode hasard) {
        this.presences = presences;
        this.relectures = relectures;
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
        relectures.save(new Relecture(exercice, elu));
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
