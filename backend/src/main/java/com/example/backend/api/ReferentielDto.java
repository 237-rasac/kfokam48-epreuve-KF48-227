package com.example.backend.api;

import com.example.backend.domaine.Etudiant;
import com.example.backend.domaine.Promotion;

/** Réponses du référentiel, alignées sur les schémas Promotion et Etudiant du contrat. */
public final class ReferentielDto {

    private ReferentielDto() {
    }

    public record PromotionDto(long id, String nom) {
        public static PromotionDto de(Promotion promotion) {
            return new PromotionDto(promotion.getId(), promotion.getNom());
        }
    }

    public record EtudiantDto(long id, String nom, long promotionId) {
        public static EtudiantDto de(Etudiant etudiant) {
            return new EtudiantDto(etudiant.getId(), etudiant.getNom(), etudiant.getPromotion().getId());
        }
    }
}
