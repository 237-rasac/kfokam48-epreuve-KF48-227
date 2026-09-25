package com.example.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.domaine.Etudiant;
import com.example.backend.repository.ExerciceRepository;
import com.example.backend.repository.PresenceRepository;
import com.example.backend.repository.RelectureRepository;

/**
 * MODULE 8 — agrégation du tableau de bord (EF6, RG13, RG21).
 * RG21 : la moyenne est null si aucune note reçue (une moyenne de 0 serait fausse ;
 * le front affiche « — »). Sans relecteur = exercice EN_ATTENTE sans relecture.
 */
@Service
public class TableauService {

    /** Ligne du tableau — étend le schéma LigneTableau du contrat (exercicesSansRelecteur, issue #20). */
    public record LigneTableauDto(long etudiantId, String nom, long presences, long exercicesDeposes,
            Double moyenne, long relecturesEnAttente, long exercicesSansRelecteur) {
    }

    private final PresenceRepository presences;
    private final ExerciceRepository exercices;
    private final RelectureRepository relectures;

    public TableauService(PresenceRepository presences, ExerciceRepository exercices,
            RelectureRepository relectures) {
        this.presences = presences;
        this.exercices = exercices;
        this.relectures = relectures;
    }

    @Transactional(readOnly = true)
    public List<LigneTableauDto> construire(List<Etudiant> etudiants) {
        return etudiants.stream().map(this::ligne).toList();
    }

    private LigneTableauDto ligne(Etudiant etudiant) {
        Long id = etudiant.getId();

        // Présences de l'étudiant sur toutes les sessions
        long nbPresences = presences.findByEtudiantId(id).size();

        List<com.example.backend.domaine.Exercice> depots = exercices.findByEtudiantId(id);
        long nbExercices = depots.size();

        // RG21 : moyenne des notes reçues sur ses exercices relu ; null si aucune
        List<Integer> notes = depots.stream()
                .map(exercice -> relectures.findByExerciceId(exercice.getId()).orElse(null))
                .filter(relecture -> relecture != null && relecture.getRendueAt() != null)
                .map(relecture -> relecture.getNote())
                .filter(java.util.Objects::nonNull)
                .toList();
        Double moyenne = notes.isEmpty() ? null
                : notes.stream().mapToInt(Integer::intValue).average().orElse(0d);

        // Relectures en attente = relectures assignées à l'étudiant, non rendues
        long relecturesEnAttente = relectures.findByRelecteurIdAndRendueAtIsNull(id).size();

        // Issue #20 : exercices de l'étudiant restés sans relecteur (RG8)
        long sansRelecteur = depots.stream()
                .filter(exercice -> relectures.findByExerciceId(exercice.getId()).isEmpty())
                .count();

        return new LigneTableauDto(id, etudiant.getNom(), nbPresences, nbExercices, moyenne,
                relecturesEnAttente, sansRelecteur);
    }
}
