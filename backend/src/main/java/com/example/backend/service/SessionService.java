package com.example.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.backend.domaine.Promotion;
import com.example.backend.domaine.SessionCours;
import com.example.backend.erreur.ErreurMetierException;
import com.example.backend.repository.PromotionRepository;
import com.example.backend.repository.SessionCoursRepository;

/**
 * Règles d'ouverture de session (EF1, RG1, RG16) :
 * - expirationAt = ouvertureAt + 15 minutes (RG1)
 * - code de présence unique parmi toutes les sessions (RG16)
 * - titre et promotionId obligatoires (400 CHAMP_MANQUANT)
 * - promotion inconnue → 404 PROMOTION_INCONNUE
 */
@Service
public class SessionService {

    /** RG1 : durée de validité du code de présence. */
    public static final java.time.Duration DUREE_CODE = java.time.Duration.ofMinutes(15);

    private final SessionCoursRepository sessions;
    private final PromotionRepository promotions;
    private final HorlogeMetier horloge;
    private final GenerateurCode generateur;

    public SessionService(SessionCoursRepository sessions, PromotionRepository promotions,
            HorlogeMetier horloge, GenerateurCode generateur) {
        this.sessions = sessions;
        this.promotions = promotions;
        this.horloge = horloge;
        this.generateur = generateur;
    }

    public SessionCours ouvrir(String titre, Long promotionId) {
        if (titre == null || titre.isBlank()) {
            throw new ErreurMetierException("CHAMP_MANQUANT", 400, "Le titre est obligatoire.");
        }
        if (promotionId == null) {
            throw new ErreurMetierException("CHAMP_MANQUANT", 400, "La promotion est obligatoire.");
        }
        Promotion promotion = promotions.findById(promotionId)
                .orElseThrow(() -> new ErreurMetierException("PROMOTION_INCONNUE", 404, "Promotion inconnue."));

        LocalDateTime now = horloge.maintenant();
        String code = genererCodeUnique();

        SessionCours session = new SessionCours(titre.trim(), code, now, now.plus(DUREE_CODE), promotion);
        return sessions.save(session);
    }

    public SessionCours trouver(Long id) {
        return sessions.findById(id)
                .orElseThrow(() -> new ErreurMetierException("SESSION_INCONNUE", 404, "Session inconnue."));
    }

    /**
     * MODULE 9 — EF8/RG2 : clôturer une session. clotureAt est posé, plus aucune
     * présence (RG2), dépôt (RG9), rendu ou modification de relecture (EF9) n'est
     * accepté ensuite — les services aval vérifient déjà estCloturee().
     * Session déjà clôturée → 409 SESSION_DEJA_CLOTUREE (catalogue du contrat).
     */
    @org.springframework.transaction.annotation.Transactional
    public SessionCours cloturer(Long id) {
        SessionCours session = trouver(id);
        if (session.estCloturee()) {
            throw new ErreurMetierException("SESSION_DEJA_CLOTUREE", 409,
                    "La session est déjà clôturée.");
        }
        session.setClotureAt(horloge.maintenant());
        return sessions.save(session);
    }

    /** Codes déjà pris, pour le générateur (RG16). */
    private String genererCodeUnique() {
        List<String> existants = sessions.findAll().stream().map(SessionCours::getCode).toList();
        return generateur.genererUnique(new java.util.HashSet<>(existants));
    }
}
