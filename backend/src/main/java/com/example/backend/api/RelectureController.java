package com.example.backend.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.api.RelectureDtos.RelectureCreation;
import com.example.backend.api.RelectureDtos.RelectureDto;
import com.example.backend.domaine.Relecture;
import com.example.backend.repository.RelectureRepository;
import com.example.backend.service.RelectureRendueService;

/**
 * API relectures (MODULE 6) : rendre une note et un commentaire (EF5) et
 * consulter les relectures assignées à un étudiant (écran relecteur).
 */
@RestController
public class RelectureController {

    private final RelectureRendueService relectureRendueService;
    private final RelectureRepository relectures;

    public RelectureController(RelectureRendueService relectureRendueService,
            RelectureRepository relectures) {
        this.relectureRendueService = relectureRendueService;
        this.relectures = relectures;
    }

    @PostMapping("/api/relectures/{id}")
    public RelectureDto rendreRelecture(@PathVariable Long id,
            @RequestBody RelectureCreation creation) {
        // Pas d'authentification au périmètre : l'écran relecteur passe l'id de
        // l'étudiant connecté pour que le service vérifie RG3 (auto-relecture).
        Relecture relecture = relectureRendueService.rendre(id, creation.relecteurId(), creation.note(),
                creation.commentaire());
        return RelectureDto.de(relecture);
    }

    /** Contrat, operationId getRelecture : détail d'une relecture (assignée ou rendue). */
    @GetMapping("/api/relectures/{id}")
    public RelectureDto getRelecture(@PathVariable Long id) {
        Relecture relecture = relectures.findById(id)
                .orElseThrow(() -> new com.example.backend.erreur.ErreurMetierException(
                        "RELECTURE_INCONNUE", 404, "Relecture inconnue."));
        return RelectureDto.de(relecture);
    }

    @GetMapping("/api/etudiants/{id}/relectures")
    public List<RelectureDto> getRelecturesEnAttente(@PathVariable Long id) {
        return relectures.findByRelecteurIdAndRendueAtIsNull(id).stream()
                .map(RelectureDto::de)
                .toList();
    }

    /** Relectures déjà rendues par l'étudiant — bouton « Modifier » (EF9/RG7, MODULE 7). */
    @GetMapping("/api/etudiants/{id}/relectures/rendues")
    public List<RelectureDto> getRelecturesRendues(@PathVariable Long id) {
        return relectures.findByRelecteurIdAndRendueAtIsNotNull(id).stream()
                .map(RelectureDto::de)
                .toList();
    }
}
