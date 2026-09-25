package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.example.backend.repository.EtudiantRepository;

/**
 * MODULE 1 — critères d'acceptation : le contexte démarre et les 5 étudiants
 * de démonstration sont chargés et servis par l'API (sans base locale, ENF6).
 */
@SpringBootTest
@AutoConfigureMockMvc
class Module1SocleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EtudiantRepository etudiants;

    @Test
    void lesCinqEtudiantsDeDemoSontCharges() {
        assertThat(etudiants.count()).isEqualTo(5);
    }

    @Test
    void getPromotionsRenvoieLaPromotionDemo() throws Exception {
        mockMvc.perform(get("/api/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("KFOKAM48"));
    }

    @Test
    void getEtudiantsRenvoieLesCinqEtudiantsDeDemo() throws Exception {
        mockMvc.perform(get("/api/etudiants").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].nom").value("Amina Bello"))
                .andExpect(jsonPath("$[4].nom").value("Emma Fouda"));
    }

    @Test
    void erreurRenvoieLeFormatContractuelCodeMessage() throws Exception {
        mockMvc.perform(get("/api/etudiants").param("promotionId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
