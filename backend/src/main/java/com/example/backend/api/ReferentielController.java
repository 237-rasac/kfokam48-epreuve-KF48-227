package com.example.backend.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.domaine.Etudiant;
import com.example.backend.domaine.Promotion;
import com.example.backend.erreur.ErreurMetierException;
import com.example.backend.repository.EtudiantRepository;
import com.example.backend.repository.PromotionRepository;

/**
 * Référentiel : liste des promotions et des étudiants (opérations du contrat).
 * Les IDs inconnus renvoient 404 avec le format d'erreur du contrat (ENF4).
 */
@RestController
public class ReferentielController {

    private final PromotionRepository promotions;
    private final EtudiantRepository etudiants;

    public ReferentielController(PromotionRepository promotions, EtudiantRepository etudiants) {
        this.promotions = promotions;
        this.etudiants = etudiants;
    }

    @GetMapping("/api/promotions")
    public List<ReferentielDto.PromotionDto> getPromotions() {
        return promotions.findAll().stream()
                .map(ReferentielDto.PromotionDto::de)
                .toList();
    }

    @GetMapping("/api/etudiants")
    public List<ReferentielDto.EtudiantDto> getEtudiants(
            @RequestParam(name = "promotionId", required = false) Long promotionId) {
        if (promotionId == null) {
            return etudiants.findAll().stream()
                    .map(ReferentielDto.EtudiantDto::de)
                    .toList();
        }
        Promotion promotion = promotions.findById(promotionId)
                .orElseThrow(() -> new ErreurMetierException("PROMOTION_INCONNUE", 404, "Promotion inconnue."));
        List<Etudiant> liste = etudiants.findByPromotionId(promotion.getId());
        return liste.stream()
                .map(ReferentielDto.EtudiantDto::de)
                .toList();
    }
}
