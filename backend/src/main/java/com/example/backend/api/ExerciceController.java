package com.example.backend.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.api.ExerciceDtos.ExerciceCreation;
import com.example.backend.api.ExerciceDtos.ExerciceDetailDto;
import com.example.backend.api.ExerciceDtos.ExerciceDto;
import com.example.backend.api.ExerciceDtos.LienRemplacement;
import com.example.backend.domaine.Exercice;
import com.example.backend.service.ExerciceService;

/**
 * API exercices (MODULE 5) : dépôt avec assignation du relecteur (EF3, EF4),
 * remplacement du lien (EF10/RG10), consultation sans identité du relecteur (EF11).
 */
@RestController
public class ExerciceController {

    private final ExerciceService exerciceService;

    public ExerciceController(ExerciceService exerciceService) {
        this.exerciceService = exerciceService;
    }

    @PostMapping("/api/exercices")
    public ResponseEntity<ExerciceDto> deposerExercice(@RequestBody ExerciceCreation creation) {
        Exercice exercice = exerciceService.deposer(creation.sessionId(), creation.etudiantId(),
                creation.lien());
        return ResponseEntity.status(HttpStatus.CREATED).body(ExerciceDto.de(exercice));
    }

    @GetMapping("/api/exercices/{id}")
    public ExerciceDetailDto getExercice(@PathVariable Long id) {
        return ExerciceDetailDto.de(exerciceService.consulterAvecNote(id));
    }

    @PatchMapping("/api/exercices/{id}")
    public ExerciceDto remplacerLien(@PathVariable Long id, @RequestBody LienRemplacement corps) {
        exerciceService.remplacerLien(id, corps.lien());
        // On re-consulte pour rattacher la relecture éventuelle au flag relecteurAssignee
        return ExerciceDto.de(exerciceService.consulterAvecNote(id));
    }
}
