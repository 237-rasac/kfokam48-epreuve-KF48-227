package com.example.backend.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.domaine.Etudiant;
import com.example.backend.domaine.Exercice;
import com.example.backend.domaine.Relecture;
import com.example.backend.domaine.StatutExercice;
import com.example.backend.erreur.ErreurMetierException;
import com.example.backend.repository.ExerciceRepository;
import com.example.backend.repository.RelectureRepository;

/**
 * Rendre une relecture (EF5, RG3, RG6, RG8) :
 * - note entière 0–20 obligatoire, sinon 400 NOTE_INVALIDE (RG6)
 * - relecteur = auteur → 403 AUTO_RELECTURE (RG3)
 * - relecture déjà rendue → 409 RELECTURE_DEJA_RENDUE
 * - session clôturée → 410 SESSION_CLOTUREE
 * - à la réussite : statut de l'exercice passe à RELU
 */
@Service
public class RelectureRendueService {

    private final RelectureRepository relectures;
    private final ExerciceRepository exercices;
    private final HorlogeMetier horloge;

    public RelectureRendueService(RelectureRepository relectures, ExerciceRepository exercices,
            HorlogeMetier horloge) {
        this.relectures = relectures;
        this.exercices = exercices;
        this.horloge = horloge;
    }

    @Transactional
    public Relecture rendre(Long relectureId, Long relecteurId, Integer note, String commentaire) {
        if (note == null || note < 0 || note > 20) {
            throw new ErreurMetierException("NOTE_INVALIDE", 400,
                    "La note doit être un entier entre 0 et 20.");
        }
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

        // La clôture gèle la relecture
        if (exercice.getSession().estCloturee()) {
            throw new ErreurMetierException("SESSION_CLOTUREE", 410, "La session est clôturée.");
        }

        // Deuxième rendu interdit (l'issue #19 traitera la modification avant clôture)
        if (relecture.getRendueAt() != null) {
            throw new ErreurMetierException("RELECTURE_DEJA_RENDUE", 409,
                    "Cette relecture a déjà été rendue.");
        }

        relecture.rendre(note, commentaire.trim(), horloge.maintenant());
        exercice.setStatut(StatutExercice.RELU);
        return relectures.save(relecture);
    }
}
