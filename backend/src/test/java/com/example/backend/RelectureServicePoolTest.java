package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.backend.domaine.Etudiant;
import com.example.backend.domaine.Exercice;
import com.example.backend.domaine.Presence;
import com.example.backend.domaine.Promotion;
import com.example.backend.domaine.Relecture;
import com.example.backend.domaine.SessionCours;
import com.example.backend.domaine.SourcePresence;
import com.example.backend.repository.PresenceRepository;
import com.example.backend.repository.RelectureRepository;
import com.example.backend.service.GenerateurCode;
import com.example.backend.service.RelectureService;

/**
 * MODULE 5 — tests unitaires du pool d'assignation du relecteur
 * (RG3 : jamais l'auteur, RG5 : présent à la session, RG8 : pool vide OK,
 * l'issue #17 demande en plus la non-réassignation d'un relecteur actif).
 */
class RelectureServicePoolTest {

    private final Promotion promotion = new Promotion("KFOKAM48");

    private Presence presence(SessionCours session, Etudiant etudiant) {
        return new Presence(session, etudiant, SourcePresence.ETUDIANT, java.time.LocalDateTime.now());
    }

    @Test
    void rg3_leRelecteurNestJamaisLAuteur() {
        SessionCours session = new SessionCours("S", "AAAAAA", java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now().plusMinutes(15), promotion);
        Etudiant auteur = new Etudiant("Amina", promotion);
        PresenceRepository presences = org.mockito.Mockito.mock(PresenceRepository.class);
        RelectureRepository relectures = org.mockito.Mockito.mock(RelectureRepository.class);
        org.mockito.Mockito.when(presences.findBySessionId(1L))
                .thenReturn(List.of(presence(session, auteur))); // seul l'auteur est présent

        RelectureService service = new RelectureService(presences, relectures, new GenerateurCode());
        Exercice exercice = new Exercice(session, auteur, "https://x", java.time.LocalDateTime.now());

        service.assignerSiPossible(exercice);

        // RG8 : aucun autre présent → aucune relecture créée
        org.mockito.Mockito.verify(relectures, org.mockito.Mockito.never())
                .save(org.mockito.Mockito.any(Relecture.class));
    }

    @Test
    void rg5_leRelecteurEstParmiLesPresents() {
        SessionCours session = new SessionCours("S", "AAAAAA", java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now().plusMinutes(15), promotion);
        Etudiant auteur = new Etudiant("Amina", promotion);
        Etudiant present1 = new Etudiant("Boris", promotion);
        Etudiant present2 = new Etudiant("Clarisse", promotion);
        PresenceRepository presences = org.mockito.Mockito.mock(PresenceRepository.class);
        RelectureRepository relectures = org.mockito.Mockito.mock(RelectureRepository.class);
        // Les entités non persistées ont des ids null : on stubbe avec any()
        org.mockito.Mockito.when(presences.findBySessionId(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(presence(session, auteur), presence(session, present1),
                        presence(session, present2)));
        org.mockito.Mockito.when(relectures.save(org.mockito.Mockito.any()))
                .thenAnswer(appel -> appel.getArgument(0));
        org.mockito.Mockito.when(relectures.findBySessionIdAndRendueAtIsNull(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        RelectureService service = new RelectureService(presences, relectures, new GenerateurCode());
        Exercice exercice = new Exercice(session, auteur, "https://x", java.time.LocalDateTime.now());

        service.assignerSiPossible(exercice);

        var captor = org.mockito.ArgumentCaptor.forClass(Relecture.class);
        org.mockito.Mockito.verify(relectures).save(captor.capture());
        // RG3 : jamais l'auteur — le relecteur est Boris ou Clarisse (noms, ids null hors JPA)
        assertThat(captor.getValue().getRelecteur().getNom()).isIn("Boris", "Clarisse");
    }

    @Test
    void unRelecteurActifNestPasReassigne() {
        SessionCours session = new SessionCours("S", "AAAAAA", java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now().plusMinutes(15), promotion);
        Etudiant auteur = new Etudiant("Amina", promotion);
        Etudiant boris = new Etudiant("Boris", promotion);
        Etudiant clarisse = new Etudiant("Clarisse", promotion);

        Exercice autreExercice = new Exercice(session, boris, "https://y", java.time.LocalDateTime.now());
        Relecture enCours = new Relecture(autreExercice, boris); // Boris déjà relecteur actif

        PresenceRepository presences = org.mockito.Mockito.mock(PresenceRepository.class);
        RelectureRepository relectures = org.mockito.Mockito.mock(RelectureRepository.class);
        org.mockito.Mockito.when(presences.findBySessionId(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(presence(session, auteur), presence(session, boris),
                        presence(session, clarisse)));
        org.mockito.Mockito.when(relectures.findBySessionIdAndRendueAtIsNull(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(enCours));
        org.mockito.Mockito.when(relectures.save(org.mockito.Mockito.any()))
                .thenAnswer(appel -> appel.getArgument(0));

        RelectureService service = new RelectureService(presences, relectures, new GenerateurCode());
        Exercice exercice = new Exercice(session, auteur, "https://x", java.time.LocalDateTime.now());

        service.assignerSiPossible(exercice);

        var captor = org.mockito.ArgumentCaptor.forClass(Relecture.class);
        org.mockito.Mockito.verify(relectures).save(captor.capture());
        // Boris est exclu (déjà relecteur actif) → Clarisse est choisie
        assertThat(captor.getValue().getRelecteur().getNom()).isEqualTo("Clarisse");
    }

    @Test
    void aucunEtudiantNonAuteurPresentNeBloquePasLeDepot() {
        SessionCours session = new SessionCours("S", "AAAAAA", java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now().plusMinutes(15), promotion);
        Etudiant auteur = new Etudiant("Amina", promotion);
        PresenceRepository presences = org.mockito.Mockito.mock(PresenceRepository.class);
        RelectureRepository relectures = org.mockito.Mockito.mock(RelectureRepository.class);
        org.mockito.Mockito.when(presences.findBySessionId(1L))
                .thenReturn(List.of(presence(session, auteur)));

        RelectureService service = new RelectureService(presences, relectures, new GenerateurCode());
        Exercice exercice = new Exercice(session, auteur, "https://x", java.time.LocalDateTime.now());

        // RG8 : ne lève pas, ne crée rien — l'exercice reste EN_ATTENTE sans relecteur
        service.assignerSiPossible(exercice);
        org.mockito.Mockito.verify(relectures, org.mockito.Mockito.never())
                .save(org.mockito.Mockito.any(Relecture.class));
    }
}
