package com.example.backend.api;

import com.example.backend.domaine.Presence;

/** Réponses présence, alignées sur le schéma Presence du contrat. */
public final class PresenceDtos {

    private PresenceDtos() {
    }

    public record PresenceDto(long id, long sessionId, long etudiantId, String source, String marqueeAt) {

        public static PresenceDto de(Presence presence) {
            return new PresenceDto(
                    presence.getId(),
                    presence.getSession().getId(),
                    presence.getEtudiant().getId(),
                    presence.getSource().name(),
                    SessionDtos.horodatage(presence.getMarqueeAt()));
        }
    }

    /** Corps de POST /api/presences — source optionnelle (EF7 : FORMATEUR pour un ajout manuel). */
    public record PresenceCreation(String code, Long etudiantId, String source) {
    }
}
