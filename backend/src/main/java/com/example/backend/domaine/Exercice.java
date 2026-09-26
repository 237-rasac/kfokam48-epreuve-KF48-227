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

/** Exercice déposé par un étudiant pour une session — au plus un par session et par étudiant (table exercice). */
@Entity
@Table(name = "exercice")
public class Exercice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionCours session;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @Column(nullable = false, length = 500)
    private String lien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatutExercice statut;

    @Column(name = "depose_at", nullable = false)
    private LocalDateTime deposeAt;

    /** Non persisté : relectures attachées en lecture pour EF11 (moyenne + commentaires, sans relecteurs). */
    @jakarta.persistence.Transient
    private java.util.List<Relecture> relectures = new java.util.ArrayList<>();

    protected Exercice() {
        // JPA
    }

    public Exercice(SessionCours session, Etudiant etudiant, String lien, LocalDateTime deposeAt) {
        this.session = session;
        this.etudiant = etudiant;
        this.lien = lien;
        this.statut = StatutExercice.EN_ATTENTE;
        this.deposeAt = deposeAt;
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

    public String getLien() {
        return lien;
    }

    public void setLien(String lien) {
        this.lien = lien;
    }

    public StatutExercice getStatut() {
        return statut;
    }

    public void setStatut(StatutExercice statut) {
        this.statut = statut;
    }

    public LocalDateTime getDeposeAt() {
        return deposeAt;
    }

    public java.util.List<Relecture> getRelectures() {
        return relectures;
    }

    public void setRelectures(java.util.List<Relecture> relectures) {
        this.relectures = relectures == null ? new java.util.ArrayList<>() : relectures;
    }

    /** Relectures déjà rendues (EF11 v2). */
    public java.util.List<Relecture> relecturesRendues() {
        return relectures.stream()
                .filter(r -> r.getRendueAt() != null)
                .toList();
    }

    /**
     * RG17/RG8 : le provisoire n'a de sens que si une deuxième relecture est
     * attendue et pas encore rendue. Un exercice à relecteur unique dont la
     * note est rendue est définitif (convention §7 du cahier).
     */
    public boolean estProvisoire() {
        long rendues = relecturesRendues().size();
        return !relectures.isEmpty() && rendues > 0 && rendues < relectures.size();
    }

    /** Moyenne des notes des relectures rendues, sur 20 (RG17) — null si aucune. */
    public Double moyenneDesNotes() {
        java.util.List<Integer> notes = relecturesRendues().stream()
                .map(Relecture::getNote)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (notes.isEmpty()) {
            return null;
        }
        return notes.stream().mapToInt(Integer::intValue).average().orElse(0d);
    }
}
