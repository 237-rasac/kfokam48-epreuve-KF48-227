package com.example.backend.api;

import com.example.backend.domaine.SessionCours;

/** Réponses session, alignées sur le schéma Session du contrat. */
public final class SessionDtos {

    private SessionDtos() {
    }

    public record SessionDto(long id, String titre, String code, String ouvertureAt, String expirationAt,
            String clotureAt, long promotionId) {

        public static SessionDto de(SessionCours session) {
            return new SessionDto(
                    session.getId(),
                    session.getTitre(),
                    session.getCode(),
                    session.getOuvertureAt().toString(),
                    session.getExpirationAt().toString(),
                    session.getClotureAt() == null ? null : session.getClotureAt().toString(),
                    session.getPromotion().getId());
        }
    }

    /** Corps de POST /api/sessions. */
    public record SessionCreation(String titre, Long promotionId) {
    }
}
