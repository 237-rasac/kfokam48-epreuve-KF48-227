package com.example.backend.domaine;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Relecture d'un exercice par un pair (table relecture).
 * RG3 : le relecteur n'est jamais l'auteur de l'exercice (garantie applicative).
 * RG4 : un seul relecteur par exercice (contrainte uk_relecture_exercice).
 */
@Entity
@Table(name = "relecture")
public class Relecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "exercice_id", nullable = false)
    private Exercice exercice;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "relecteur_id", nullable = false)
    private Etudiant relecteur;

    /** Note entière 0–20 (RG6), null tant que la relecture n'est pas rendue. */
    @Column
    private Integer note;

    @Column(length = 2000)
    private String commentaire;

    @Column(name = "rendue_at")
    private LocalDateTime rendueAt;

    protected Relecture() {
        // JPA
    }

    public Relecture(Exercice exercice, Etudiant relecteur) {
        this.exercice = exercice;
        this.relecteur = relecteur;
    }

    public Long getId() {
        return id;
    }

    public Exercice getExercice() {
        return exercice;
    }

    public Etudiant getRelecteur() {
        return relecteur;
    }

    public Integer getNote() {
        return note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public LocalDateTime getRendueAt() {
        return rendueAt;
    }

    /** Enregistre la note et le commentaire (EF5) — l'exercice passe alors à RELU. */
    public void rendre(Integer note, String commentaire, LocalDateTime rendueAt) {
        this.note = note;
        this.commentaire = commentaire;
        this.rendueAt = rendueAt;
    }
}
