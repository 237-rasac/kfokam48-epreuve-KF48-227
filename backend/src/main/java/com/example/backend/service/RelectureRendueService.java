package com.example.backend.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.domaine.Etudiant;
import com.example.backend.domaine.Exercice;
import com.example.backend.domaine.Relecture;
import com.example.backend.domaine.RelectureHistorique;
import com.example.backend.erreur.ErreurMetierException;
import com.example.backend.repository.ExerciceRepository;
import com.example.backend.repository.RelectureHistoriqueRepository;
import com.example.backend.repository.RelectureRepository;

/**
 * Rendre une relecture (EF5, RG3, RG6, RG8) et la modifier avant clôture
 * (EF9, RG7) :
 * - note entière 0–20 obligatoire, sinon 400 NOTE_INVALIDE (RG6)
 * - relecteur = auteur → 403 AUTO_RELECTURE (RG3)
 * - premier rendu : 200, exercice passe à RELU
 * - rendu suivant avant clôture : 200 + ligne d'historique (EF9/RG7)
 * - après clôture : 409 RELECTURE_VERROUILLEE (code du contrat)
 */
@Service
public class RelectureRendueService {

    private final RelectureRepository relectures;
    private final ExerciceRepository exercices;
    private final RelectureHistoriqueRepository historiques;
    private final HorlogeMetier horloge;

    public RelectureRendueService(RelectureRepository relectures, ExerciceRepository exercices,
            RelectureHistoriqueRepository historiques, HorlogeMetier horloge) {
        this.relectures = relectures;
        this.exercices = exercices;
        this.historiques = historiques;
        this.horloge = horloge;
    }

    @Transactional
    public Relecture rendre(Long relectureId, Long relecteurId, java.math.BigDecimal note,
            String commentaire) {
        // RG6 : note entière 0–20. Un décimal (15.5) est rejeté avec NOTE_INVALIDE.
        if (note == null || note.stripTrailingZeros().scale() > 0
                || note.compareTo(java.math.BigDecimal.ZERO) < 0
                || note.compareTo(java.math.BigDecimal.valueOf(20)) > 0) {
            throw new ErreurMetierException("NOTE_INVALIDE", 400,
                    "La note doit être un entier entre 0 et 20.");
        }
        int noteEntiere = note.intValueExact();
        if (commentaire == null || commentaire.isBlank()) {
            throw new ErreurMetierException("CHAMP_MANQUANT", 400, "Le commentaire est obligatoire.");
        }

        Relecture relecture = relectures.findById(relectureId)
                .orElseThrow(() -> new ErreurMetierException("RELECTURE_INCONNUE", 404,
                        "Relecture inconnue."));

        Exercice exercice = relecture.getExercice();

        // RG3 : le relecteur assigné ne peut pas être l'auteur de l'exercice
        // (garantie à l'assignation ; vérifiée ici par sécurité)
        Etudiant relecteur = relecture.getRelecteur();
        if (relecteur.getId() != null && relecteur.getId().equals(exercice.getEtudiant().getId())) {
            throw new ErreurMetierException("AUTO_RELECTURE", 403,
                    "Un étudiant ne peut pas relire son propre exercice.");
        }
        // RG3bis : l'appelant (relecteurId du corps, écran relecteur sans auth)
        // doit être le relecteur assigné, et ne peut pas être l'auteur
        if (relecteurId != null) {
            if (exercice.getEtudiant().getId() != null && relecteurId.equals(exercice.getEtudiant().getId())) {
                throw new ErreurMetierException("AUTO_RELECTURE", 403,
                        "Un étudiant ne peut pas relire son propre exercice.");
            }
            if (relecteur.getId() != null && !relecteurId.equals(relecteur.getId())) {
                throw new ErreurMetierException("RELECTURE_NON_ASSIGNEE", 403,
                        "Cette relecture n'est pas assignée à cet étudiant.");
            }
        }

        // EF9/RG7 : après clôture, plus aucune modification (code du contrat)
        if (exercice.getSession().estCloturee()) {
            throw new ErreurMetierException("RELECTURE_VERROUILLEE", 409,
                    "La session est clôturée, la relecture ne peut plus être modifiée.");
        }

        if (relecture.getRendueAt() == null) {
            // Premier rendu (EF5) : RELU seulement quand TOUTES les relectures
            // attendues sont rendues (RG4 modifiée, issue #41) — un exercice à
            // deux relecteurs reste EN_ATTENTE après le premier rendu
            relecture.rendre(noteEntiere, commentaire.trim(), horloge.maintenant());
            long attendues = relectures.findByExerciceId(exercice.getId()).size();
            long rendues = relectures.findByExerciceId(exercice.getId()).stream()
                    .filter(r -> r.getRendueAt() != null)
                    .count();
            if (rendues >= attendues) {
                exercice.setStatut(com.example.backend.domaine.StatutExercice.RELU);
            }
        } else {
            // Modification d'une relecture déjà rendue (EF9/RG7) : historique
            historiques.save(new RelectureHistorique(
                    relecture,
                    relecture.getNote(),
                    relecture.getCommentaire(),
                    noteEntiere,
                    commentaire.trim(),
                    horloge.maintenant()));
            relecture.rendre(noteEntiere, commentaire.trim(), relecture.getRendueAt());
        }
        return relectures.save(relecture);
    }
}
