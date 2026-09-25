package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.backend.service.CompteurErreursCode;
import com.example.backend.service.HorlogeMetier;

/** RG14 : après 5 erreurs de code, l'étudiant est bloqué 2 minutes. */
class CompteurErreursCodeTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 3, 12, 8, 0, 0);

    /** Horloge pilotée par le test : on avance le temps sans attendre 2 vraies minutes. */
    private LocalDateTime maintenant;
    private HorlogeMetier horloge;

    @BeforeEach
    void preparer() {
        maintenant = T0;
        horloge = mock(HorlogeMetier.class);
        when(horloge.maintenant()).thenAnswer(appel -> maintenant);
    }

    private CompteurErreursCode nouveauCompteur() {
        return new CompteurErreursCode(horloge);
    }

    @Test
    void leBlocageDureExactementDeuxMinutesPuisSeLeve() {
        CompteurErreursCode compteur = nouveauCompteur();
        for (int i = 0; i < 5; i++) {
            compteur.noterEchec(1L);
        }
        maintenant = T0.plusMinutes(1).plusSeconds(59);
        assertThat(compteur.estBloque(1L)).isTrue();

        maintenant = T0.plusMinutes(2);
        assertThat(compteur.estBloque(1L)).isFalse();

        // Après la levée, l'étudiant repart avec un compteur vierge : 4 erreurs ne bloquent pas
        for (int i = 0; i < 4; i++) {
            compteur.noterEchec(1L);
        }
        assertThat(compteur.estBloque(1L)).isFalse();
    }

    @Test
    void aucunBlocageSansErreur() {
        CompteurErreursCode compteur = nouveauCompteur();
        assertThat(compteur.estBloque(1L)).isFalse();
    }

    @Test
    void quatreErreursNeBlocentPas() {
        CompteurErreursCode compteur = nouveauCompteur();
        for (int i = 0; i < 4; i++) {
            compteur.noterEchec(1L);
        }
        assertThat(compteur.estBloque(1L)).isFalse();
    }

    @Test
    void cinqErreursBlocentDeuxMinutes() {
        CompteurErreursCode compteur = nouveauCompteur();
        for (int i = 0; i < 5; i++) {
            compteur.noterEchec(1L);
        }
        assertThat(compteur.estBloque(1L)).isTrue();
        assertThat(CompteurErreursCode.DUREE_BLOCAGE).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void lesCompteursSontIndependantsParEtudiant() {
        CompteurErreursCode compteur = nouveauCompteur();
        for (int i = 0; i < 5; i++) {
            compteur.noterEchec(1L);
        }
        assertThat(compteur.estBloque(2L)).isFalse();
    }

    @Test
    void unSuccesRemetLeCompteurAZero() {
        CompteurErreursCode compteur = nouveauCompteur();
        compteur.noterEchec(1L);
        compteur.noterEchec(1L);
        compteur.noterEchec(1L);
        compteur.noterEchec(1L);
        compteur.noterSucces(1L);
        for (int i = 0; i < 4; i++) {
            compteur.noterEchec(1L);
        }
        // 4 erreurs après reset : pas de blocage
        assertThat(compteur.estBloque(1L)).isFalse();
        // la 5e déclenche le blocage
        compteur.noterEchec(1L);
        assertThat(compteur.estBloque(1L)).isTrue();
    }
}
