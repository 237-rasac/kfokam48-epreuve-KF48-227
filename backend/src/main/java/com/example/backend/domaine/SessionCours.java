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
 * Session de cours : ouverture, code de présence, expiration (RG1) et clôture.
 * Table {@code session} — nommée SessionCours pour éviter l'ambiguïté avec la notion HTTP.
 */
@Entity
@Table(name = "session")
public class SessionCours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    /** Code de présence, unique par session (RG16). */
    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "ouverture_at", nullable = false)
    private LocalDateTime ouvertureAt;

    /** Code valable 15 minutes après l'ouverture (RG1). */
    @Column(name = "expiration_at", nullable = false)
    private LocalDateTime expirationAt;

    /** Non null une fois la session clôturée (EF8). */
    @Column(name = "cloture_at")
    private LocalDateTime clotureAt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    protected SessionCours() {
        // JPA
    }

    public SessionCours(String titre, String code, LocalDateTime ouvertureAt, LocalDateTime expirationAt,
            Promotion promotion) {
        this.titre = titre;
        this.code = code;
        this.ouvertureAt = ouvertureAt;
        this.expirationAt = expirationAt;
        this.promotion = promotion;
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getOuvertureAt() {
        return ouvertureAt;
    }

    public LocalDateTime getExpirationAt() {
        return expirationAt;
    }

    public LocalDateTime getClotureAt() {
        return clotureAt;
    }

    public void setClotureAt(LocalDateTime clotureAt) {
        this.clotureAt = clotureAt;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    /** La session est clôturée dès que clotureAt est renseigné (EF8). */
    public boolean estCloturee() {
        return clotureAt != null;
    }
}
