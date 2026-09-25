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
                    relecture.getRendueAt() == null ? null : relecture.getRendueAt().toString());
        }
    }

    /**
     * Corps de POST /api/relectures/{id}. relecteurId est optionnel (le contrat
     * ne le prévoit pas) : fourni par l'écran relecteur pour la vérification RG3
     * (auto-relecture → 403) en l'absence d'authentification.
     */
    public record RelectureCreation(Integer note, String commentaire, Long relecteurId) {
    }
}
