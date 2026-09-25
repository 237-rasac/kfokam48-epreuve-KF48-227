package com.example.backend.api;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
                    horodatage(session.getOuvertureAt()),
                    horodatage(session.getExpirationAt()),
                    horodatage(session.getClotureAt()),
                    session.getPromotion().getId());
        }
    }

    /**
     * date-time du contrat (RFC 3339) : secondes toujours présentes et décalage horaire explicite,
     * sinon un navigateur dans un autre fuseau que le serveur décale le compte à rebours (RG1).
     */
    static String horodatage(LocalDateTime instant) {
        return instant == null ? null
                : instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /** Corps de POST /api/sessions. */
    public record SessionCreation(String titre, Long promotionId) {
    }
}
