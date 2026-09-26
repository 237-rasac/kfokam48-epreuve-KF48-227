package com.example.backend.api;

import java.util.List;

import com.example.backend.domaine.Exercice;
import com.example.backend.domaine.Relecture;
import com.example.backend.domaine.StatutExercice;

/**
 * Réponses exercice, alignées sur les schémas Exercice et ExerciceDetail v2.0.0
 * du contrat (MODULE 12, issue #41).
 */
public final class ExerciceDtos {

    private ExerciceDtos() {
    }

    /** Réponse de POST /api/exercices et PATCH /api/exercices/{id}. */
    public record ExerciceDto(long id, long sessionId, long etudiantId, String lien, String statut,
            String deposeAt, boolean relecteurAssignee, int relecturesAttendues) {

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
                    !exercice.getRelectures().isEmpty(),
                    exercice.getRelectures().size());
        }
    }

    /**
     * EF11 v2 (RG17) : vue étudiant — statut, moyenne des relectures rendues,
     * flag provisoire, compteurs et commentaires, sans identité des relecteurs.
     * note null tant qu'aucune relecture n'est rendue.
     */
    public record ExerciceDetailDto(long id, String lien, String statut, Double note,
            boolean provisoire, int relecturesAttendues, int relecturesRendues,
            List<String> commentaires) {

        public static ExerciceDetailDto de(Exercice exercice) {
            List<Relecture> rendues = exercice.relecturesRendues();
            // EF5 : RELU quand toutes les relectures attendues sont rendues
            String statut = !exercice.getRelectures().isEmpty()
                    && rendues.size() == exercice.getRelectures().size()
                            ? StatutExercice.RELU.name()
                            : StatutExercice.EN_ATTENTE.name();
            return new ExerciceDetailDto(
                    exercice.getId(),
                    exercice.getLien(),
                    statut,
                    exercice.moyenneDesNotes(),
                    exercice.estProvisoire(),
                    exercice.getRelectures().size(),
                    rendues.size(),
                    rendues.stream().map(Relecture::getCommentaire).toList());
        }
    }

    /** Corps de POST /api/exercices. */
    public record ExerciceCreation(Long sessionId, Long etudiantId, String lien) {
    }

    /** Corps de PATCH /api/exercices/{id}. */
    public record LienRemplacement(String lien) {
    }
}
