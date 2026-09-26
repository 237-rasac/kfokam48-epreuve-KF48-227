package com.example.backend.service;

import java.util.ArrayList;
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
 * Assignation des relecteurs (EF4, RG3, RG4 modifiée, RG5, RG8 — MODULE 12, issue #41) :
 * - chaque exercice est relu par DEUX pairs différents (RG4 modifiée) : deux
 *   relectures créées à l'inscription quand le pool le permet
 * - pool = étudiants présents à la session, hors auteur (RG3), hors étudiants
 *   déjà relecteurs d'un exercice EN_ATTENTE de la même session (répartition)
 * - RG8 : pool insuffisant → une seule relecture, voire aucune : l'exercice
 *   reste EN_ATTENTE ; le formateur peut compléter par assignation manuelle.
 *   Convention §7 du cahier : un exercice à relecteur unique dont la note est
 *   rendue est définitif — le provisoire n'a de sens que si une deuxième
 *   relecture est attendue (voir ExerciceDetail.provisoire)
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
     * Assigne jusqu'à deux relecteurs au hasard parmi les présents éligibles
     * (RG5) : deux distincts entre eux et de l'auteur (RG3/RG4 modifiée).
     * N'assigne que ce que le pool permet (RG8) : deux, une ou aucune.
     */
    @Transactional
    public void assignerSiPossible(Exercice exercice) {
        List<Etudiant> pool = poolEligible(exercice);

        if (pool.isEmpty()) {
            return; // RG8 : exercice EN_ATTENTE sans relecteur
        }
        Etudiant premier = retirerAuHasard(pool);
        relectures.save(new Relecture(exercice, premier, AssignedBy.SYSTEME));

        if (pool.isEmpty()) {
            return; // RG8 : un seul présent hors auteur → une seule relecture
        }
        Etudiant second = retirerAuHasard(pool);
        relectures.save(new Relecture(exercice, second, AssignedBy.SYSTEME));
    }

    /**
     * MODULE 10 (issue #22) — assignation manuelle du relecteur par le formateur.
     * Complétée au MODULE 12 (issue #41) : l'endpoint peut désormais être appelé
     * une seconde fois pour assigner le second relecteur.
     * - relecteurId = auteur → 400 (RG3)
     * - (exercice, relecteur) déjà assigné → 409 RELECTURE_DEJA_ASSIGNEE
     * - deux relectures déjà en place → 409 RELECTURE_DEJA_ASSIGNEE (RG4 modifiée : plafond atteint)
     * - exercice inconnu / relecteur inconnu → 404
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
        List<Relecture> existantes = relectures.findByExerciceId(exerciceId);

        // RG4 (modifiée) : pas deux fois le même relecteur sur un exercice,
        // ni plus de deux relectures en tout
        boolean dejaAssigne = existantes.stream()
                .anyMatch(r -> memeEtudiant(r.getRelecteur(), relecteur));
        if (dejaAssigne || existantes.size() >= 2) {
            throw new ErreurMetierException("RELECTURE_DEJA_ASSIGNEE", 409,
                    "Cet exercice a déjà un relecteur pour cette assignation.");
        }
        // L'issue #22 vise les exercices sans relecteur ; la session clôturée reste
        // une borne : plus aucune action n'est acceptée après clôture (EF8)
        if (exercice.getSession().estCloturee()) {
            throw new ErreurMetierException("SESSION_CLOTUREE", 410, "La session est clôturée.");
        }

        return relectures.save(new Relecture(exercice, relecteur, AssignedBy.FORMATEUR));
    }

    /**
     * Pool d'assignation (RG5) : présents à la session, hors auteur (RG3), hors
     * étudiants déjà relecteurs d'un AUTRE exercice en attente de la même session
     * (répartition entre exercices, issue #17 — les relectures de l'exercice en
     * cours lui-même sont ignorées, sinon le second relecteur serait exclu).
     */
    private List<Etudiant> poolEligible(Exercice exercice) {
        Long sessionId = exercice.getSession().getId();

        Etudiant auteur = exercice.getEtudiant();
        List<Etudiant> presents = presences.findBySessionId(sessionId).stream()
                .map(Presence::getEtudiant)
                .filter(e -> !memeEtudiant(e, auteur))
                .toList();

        List<Etudiant> relecteursActifs = relectures.findBySessionIdAndRendueAtIsNull(sessionId).stream()
                .filter(r -> !estPourExercice(r, exercice))
                .map(Relecture::getRelecteur)
                .toList();

        return presents.stream()
                .filter(e -> relecteursActifs.stream().noneMatch(r -> memeEtudiant(r, e)))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    /** La relecture porte-t-elle sur cet exercice ? (ids null → comparaison par référence, tests unitaires). */
    private static boolean estPourExercice(Relecture relecture, Exercice exercice) {
        Exercice sien = relecture.getExercice();
        if (sien == null) {
            return false;
        }
        if (sien.getId() != null && exercice.getId() != null) {
            return sien.getId().equals(exercice.getId());
        }
        return sien == exercice;
    }

    /** Tire un étudiant au hasard dans le pool et le retire (pas deux fois le même). */
    private Etudiant retirerAuHasard(List<Etudiant> pool) {
        return pool.remove(hasard.indiceAleatoire(pool.size()));
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
