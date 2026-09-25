package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.example.backend.service.CompteurErreursCode;

/** RG14 : après 5 erreurs de code, l'étudiant est bloqué 2 minutes. */
class CompteurErreursCodeTest {

    @Test
    void aucunBlocageSansErreur() {
        CompteurErreursCode compteur = new CompteurErreursCode();
        assertThat(compteur.estBloque(1L)).isFalse();
    }

    @Test
    void quatreErreursNeBlocentPas() {
        CompteurErreursCode compteur = new CompteurErreursCode();
        for (int i = 0; i < 4; i++) {
            compteur.noterEchec(1L);
        }
        assertThat(compteur.estBloque(1L)).isFalse();
    }

    @Test
    void cinqErreursBlocentDeuxMinutes() {
        CompteurErreursCode compteur = new CompteurErreursCode();
        for (int i = 0; i < 5; i++) {
            compteur.noterEchec(1L);
        }
        assertThat(compteur.estBloque(1L)).isTrue();
        assertThat(CompteurErreursCode.DUREE_BLOCAGE).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void lesCompteursSontIndependantsParEtudiant() {
        CompteurErreursCode compteur = new CompteurErreursCode();
        for (int i = 0; i < 5; i++) {
            compteur.noterEchec(1L);
        }
        assertThat(compteur.estBloque(2L)).isFalse();
    }

    @Test
    void unSuccesRemetLeCompteurAZero() {
        CompteurErreursCode compteur = new CompteurErreursCode();
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
