package com.example.backend.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.domaine.Exercice;
import com.example.backend.domaine.Relecture;
import com.example.backend.repository.ExerciceRepository;
import com.example.backend.repository.RelectureRepository;
import com.example.backend.service.RelectureService;

/**
 * MODULE 10 (issue #22) : le formateur débloque un exercice sans relecteur en
 * un clic. POST /api/exercices/{id}/assigner { relecteurId } → 200 Relecture.
 * GET /api/sessions/{id}/exercices-sans-relecteur alimente l'écran formateur.
 */
@RestController
public class AssignationController {

    private final RelectureService relectureService;
    private final ExerciceRepository exercices;
    private final RelectureRepository relectures;

    public AssignationController(RelectureService relectureService, ExerciceRepository exercices,
            RelectureRepository relectures) {
        this.relectureService = relectureService;
        this.exercices = exercices;
        this.relectures = relectures;
    }

    /** Corps de POST /api/exercices/{id}/assigner. */
    public record AssignationCreation(Long relecteurId) {
    }

    /** Exercice resté sans relecteur (RG8), pour la liste du formateur. */
    public record ExerciceSansRelecteurDto(long id, long sessionId, long etudiantId, String lien,
            String deposeAt) {

        static ExerciceSansRelecteurDto de(Exercice exercice) {
            return new ExerciceSansRelecteurDto(exercice.getId(), exercice.getSession().getId(),
                    exercice.getEtudiant().getId(), exercice.getLien(),
                    exercice.getDeposeAt().toString());
        }
    }

    @PostMapping("/api/exercices/{id}/assigner")
    public RelectureDtos.RelectureDto assigner(@PathVariable Long id,
            @RequestBody AssignationCreation creation) {
        Relecture relecture = relectureService.assignerManuellement(id, creation.relecteurId());
        return RelectureDtos.RelectureDto.de(relecture);
    }

    /** Exercices de la session sans relecteur : le formateur les voit et peut assigner. */
    @GetMapping("/api/sessions/{id}/exercices-sans-relecteur")
    public List<ExerciceSansRelecteurDto> exercicesSansRelecteur(@PathVariable Long id) {
        return exercices.findBySessionId(id).stream()
                .filter(exercice -> relectures.findByExerciceId(exercice.getId()).isEmpty())
                .map(ExerciceSansRelecteurDto::de)
                .toList();
    }
}
