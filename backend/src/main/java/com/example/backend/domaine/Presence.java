package com.example.backend.domaine;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Présence d'un étudiant à une session — au plus une par session et par étudiant (RG15, table presence). */
@Entity
@Table(name = "presence")
public class Presence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionCours session;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SourcePresence source;

    @Column(name = "marquee_at", nullable = false)
    private LocalDateTime marqueeAt;

    protected Presence() {
        // JPA
    }

    public Presence(SessionCours session, Etudiant etudiant, SourcePresence source, LocalDateTime marqueeAt) {
        this.session = session;
        this.etudiant = etudiant;
        this.source = source;
        this.marqueeAt = marqueeAt;
    }

    public Long getId() {
        return id;
    }

    public SessionCours getSession() {
        return session;
    }

    public Etudiant getEtudiant() {
        return etudiant;
    }

    public SourcePresence getSource() {
        return source;
    }

    public LocalDateTime getMarqueeAt() {
        return marqueeAt;
    }
}
