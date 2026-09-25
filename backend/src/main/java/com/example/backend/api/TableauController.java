package com.example.backend.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.domaine.Etudiant;
import com.example.backend.erreur.ErreurMetierException;
import com.example.backend.repository.EtudiantRepository;
import com.example.backend.repository.ExerciceRepository;
import com.example.backend.repository.PresenceRepository;
import com.example.backend.repository.PromotionRepository;
import com.example.backend.repository.RelectureRepository;
import com.example.backend.repository.SessionCoursRepository;
import com.example.backend.service.TableauService;

/**
 * MODULE 8 — EF6, RG13, RG21 : tableau de bord du formateur par promotion.
 * Une ligne par étudiant : présences, exercices déposés, moyenne (null si
 * aucune note reçue), relectures en attente, exercices sans relecteur.
 */
@RestController
public class TableauController {

    private final PromotionRepository promotions;
    private final EtudiantRepository etudiants;
    private final TableauService tableauService;

    public TableauController(PromotionRepository promotions, EtudiantRepository etudiants,
            TableauService tableauService) {
        this.promotions = promotions;
        this.etudiants = etudiants;
        this.tableauService = tableauService;
    }

    @GetMapping("/api/tableau")
    public List<TableauService.LigneTableauDto> getTableau(
            @RequestParam(name = "promotionId") Long promotionId) {
        if (promotions.findById(promotionId).isEmpty()) {
            throw new ErreurMetierException("PROMOTION_INCONNUE", 404, "Promotion inconnue.");
        }
        List<Etudiant> etudiantsPromotion = etudiants.findByPromotionId(promotionId);
        return tableauService.construire(etudiantsPromotion);
    }
}
