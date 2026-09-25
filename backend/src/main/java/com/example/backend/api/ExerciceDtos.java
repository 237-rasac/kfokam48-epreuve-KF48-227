package com.example.backend.api;

import com.example.backend.domaine.Exercice;
import com.example.backend.domaine.Relecture;

/** Réponses exercice, alignées sur les schémas Exercice et ExerciceDetail du contrat. */
public final class ExerciceDtos {

    private ExerciceDtos() {
    }

    /** Réponse de POST /api/exercices et PATCH /api/exercices/{id}. */
    public record ExerciceDto(long id, long sessionId, long etudiantId, String lien, String statut,
            String deposeAt, boolean relecteurAssignee) {

        public static ExerciceDto de(Exercice exercice) {
            return new ExerciceDto(
                    exercice.getId(),
                    exercice.getSession().getId(),
                    exercice.getEtudiant().getId(),
                    exercice.getLien(),
                    exercice.getStatut().name(),
                    // date-time du contrat (RFC 3339), comme SessionDtos
                    exercice.getDeposeAt().atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    exercice.getRelecture() != null);
        }
    }

    /**
     * EF11 : vue étudiant — statut, note et commentaire, sans identité du relecteur.
     * Note/commentaire null tant que la relecture n'est pas rendue.
     */
    public record ExerciceDetailDto(long id, String lien, String statut, Integer note,
            String commentaire) {

        public static ExerciceDetailDto de(Exercice exercice) {
            Relecture r = exercice.getRelecture();
            boolean rendue = r != null && r.getRendueAt() != null;
            return new ExerciceDetailDto(
                    exercice.getId(),
                    exercice.getLien(),
                    rendue ? com.example.backend.domaine.StatutExercice.RELU.name() : exercice.getStatut().name(),
                    rendue ? r.getNote() : null,
                    rendue ? r.getCommentaire() : null);
        }
    }

    /** Corps de POST /api/exercices. */
    public record ExerciceCreation(Long sessionId, Long etudiantId, String lien) {
    }

    /** Corps de PATCH /api/exercices/{id}. */
    public record LienRemplacement(String lien) {
    }
}
