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
 * Ligne d'historique créée à chaque modification d'une relecture déjà rendue
 * (EF9/RG7) : conserve les anciennes et les nouvelles valeurs.
 */
@Entity
@Table(name = "relecture_historique")
public class RelectureHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "relecture_id", nullable = false)
    private Relecture relecture;

    @Column(name = "ancienne_note")
    private Integer ancienneNote;

    @Column(name = "ancien_commentaire", length = 2000)
    private String ancienCommentaire;

    @Column(name = "nouvelle_note", nullable = false)
    private Integer nouvelleNote;

    @Column(name = "nouveau_commentaire", nullable = false, length = 2000)
    private String nouveauCommentaire;

    @Column(name = "modifie_at", nullable = false)
    private LocalDateTime modifieAt;

    protected RelectureHistorique() {
        // JPA
    }

    public RelectureHistorique(Relecture relecture, Integer ancienneNote, String ancienCommentaire,
            Integer nouvelleNote, String nouveauCommentaire, LocalDateTime modifieAt) {
        this.relecture = relecture;
        this.ancienneNote = ancienneNote;
        this.ancienCommentaire = ancienCommentaire;
        this.nouvelleNote = nouvelleNote;
        this.nouveauCommentaire = nouveauCommentaire;
        this.modifieAt = modifieAt;
    }

    public Long getId() {
        return id;
    }

    public Relecture getRelecture() {
        return relecture;
    }

    public Integer getAncienneNote() {
        return ancienneNote;
    }

    public String getAncienCommentaire() {
        return ancienCommentaire;
    }

    public Integer getNouvelleNote() {
        return nouvelleNote;
    }

    public String getNouveauCommentaire() {
        return nouveauCommentaire;
    }

    public LocalDateTime getModifieAt() {
        return modifieAt;
    }
}
