package com.example.backend.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

/**
 * Horloge injectable : l'application tourne sur l'horloge système, les tests
 * peuvent figer le temps (utile pour RG1 — expiration à 15 minutes).
 */
@Component
public class HorlogeMetier {

    private final Clock clock;

    public HorlogeMetier() {
        this(Clock.systemDefaultZone());
    }

    /** Constructeur pour les tests : Clock.fixed(...). */
    HorlogeMetier(Clock clock) {
        this.clock = clock;
    }

    public LocalDateTime maintenant() {
        return LocalDateTime.now(clock);
    }

    /** Constructeur pratique pour les tests : horloge figée à un instant donné. */
    public static HorlogeMetier figee(LocalDateTime instant) {
        return new HorlogeMetier(Clock.fixed(instant.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()));
    }
}
