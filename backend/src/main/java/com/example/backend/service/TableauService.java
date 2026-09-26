package com.example.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.domaine.Etudiant;
import com.example.backend.domaine.Exercice;
import com.example.backend.repository.ExerciceRepository;
import com.example.backend.repository.PresenceRepository;
import com.example.backend.repository.RelectureRepository;

/**
 * MODULE 8 — agrégation du tableau de bord (EF6, RG13, RG21).
 * RG21 : la moyenne est null si aucune note reçue (une moyenne de 0 serait fausse ;
 * le front affiche « — »). Sans relecteur = exercice EN_ATTENTE sans relecture.
 * v2 (MODULE 12, issue #41) : la note d'un exercice est la moyenne de SES
 * relectures rendues (RG17) ; la moyenne de l'étudiant est la moyenne de ces
 * notes d'exercices. Elle hérite du caractère provisoire : une moyenne appuyée
 * sur un exercice à relecture unique rendue (ou en attente d'une seconde) est
 * marquée provisoire — l'API n'expose pas les relecteurs au front, le front
 * décide de l'affichage (badge) d'après ce flag.
 */
@Service
public class TableauService {

    /**
     * Ligne du tableau — étend le schéma LigneTableau du contrat
     * (exercicesSansRelecteur, issue #20 ; moyenneProvisoire, issue #41).
     */
    public record LigneTableauDto(long etudiantId, String nom, long presences, long exercicesDeposes,
            Double moyenne, long relecturesEnAttente, long exercicesSansRelecteur,
            boolean moyenneProvisoire) {
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

        List<Exercice> depots = exercices.findByEtudiantId(id);
        long nbExercices = depots.size();

        // RG13/RG17 : moyenne des moyennes d'exercices ; chaque note d'exercice
        // est la moyenne de ses relectures rendues. Null si aucune note reçue (RG21)
        List<Double> notesExercices = depots.stream()
                .map(exercice -> {
                    exercice.setRelectures(relectures.findByExerciceId(exercice.getId()));
                    return exercice;
                })
                .map(Exercice::moyenneDesNotes)
                .filter(java.util.Objects::nonNull)
                .toList();
        Double moyenne = notesExercices.isEmpty() ? null
                : notesExercices.stream().mapToDouble(Double::doubleValue).average().orElse(0d);

        // Héritage du provisoire : au moins un exercice noté est provisoire
        // (une relecture rendue sur deux attendues)
        boolean moyenneProvisoire = moyenne != null && depots.stream()
                .anyMatch(exercice -> {
                    exercice.setRelectures(relectures.findByExerciceId(exercice.getId()));
                    return exercice.moyenneDesNotes() != null && exercice.estProvisoire();
                });

        // Relectures en attente = relectures assignées à l'étudiant, non rendues
        long relecturesEnAttente = relectures.findByRelecteurIdAndRendueAtIsNull(id).size();

        // Issue #20 : exercices de l'étudiant restés sans relecteur (RG8)
        long sansRelecteur = depots.stream()
                .filter(exercice -> relectures.findByExerciceId(exercice.getId()).isEmpty())
                .count();

        return new LigneTableauDto(id, etudiant.getNom(), nbPresences, nbExercices, moyenne,
                relecturesEnAttente, sansRelecteur, moyenneProvisoire);
    }
}
