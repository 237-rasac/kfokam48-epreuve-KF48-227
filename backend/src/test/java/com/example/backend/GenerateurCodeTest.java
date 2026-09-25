package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.backend.service.GenerateurCode;

/** RG16 : le code de présence est unique par session. */
class GenerateurCodeTest {

    @Test
    void lesCodesGeneresSontTousUniquesEtAuBonFormat() {
        GenerateurCode generateur = new GenerateurCode();
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String code = generateur.generer();
            codes.add(code);
            assertThat(code).hasSize(6).matches("[A-Z0-9]{6}");
        }
        // 1000 tirages parmi ~1 milliard : les collisions restent extrêmement improbables
        assertThat(codes).hasSize(1000);
    }

    @Test
    void genererUniqueEviteLesCodesExistants() {
        GenerateurCode generateur = new GenerateurCode();
        Set<String> pris = GenerateurCode.echantillon(50);
        String code = generateur.genererUnique(pris);
        assertThat(code).hasSize(6);
        assertThat(pris).doesNotContain(code);
    }
}
