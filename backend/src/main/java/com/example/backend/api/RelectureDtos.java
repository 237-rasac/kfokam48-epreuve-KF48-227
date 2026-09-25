package com.example.backend.api;

import com.example.backend.domaine.Relecture;

/** Réponses relecture, alignées sur le schéma Relecture du contrat. */
public final class RelectureDtos {

    private RelectureDtos() {
    }

    public record RelectureDto(long id, long exerciceId, long relecteurId, Integer note,
            String commentaire, String rendueAt) {

        public static RelectureDto de(Relecture relecture) {
            return new RelectureDto(
                    relecture.getId(),
                    relecture.getExercice().getId(),
                    relecture.getRelecteur().getId(),
                    relecture.getNote(),
                    relecture.getCommentaire(),
                    // date-time du contrat (RFC 3339), comme SessionDtos
                    relecture.getRendueAt() == null ? null
                            : relecture.getRendueAt().atZone(java.time.ZoneId.systemDefault())
                                    .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }

    /**
     * Corps de POST /api/relectures/{id}. relecteurId est optionnel (le contrat
     * ne le prévoit pas) : fourni par l'écran relecteur pour la vérification RG3
     * (auto-relecture → 403) en l'absence d'authentification.
     * note est un BigDecimal pour qu'un décimal (15.5) atteigne la validation
     * métier et renvoie NOTE_INVALIDE au lieu d'un 400 de désérialisation.
     */
    public record RelectureCreation(java.math.BigDecimal note, String commentaire, Long relecteurId) {
    }
}
