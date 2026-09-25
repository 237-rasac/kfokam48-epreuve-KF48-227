package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.backend.repository.SessionCoursRepository;
import com.example.backend.service.HorlogeMetier;
import com.example.backend.service.SessionService;

/**
 * MODULE 2 — EF1, RG1, RG16 : ouvrir une session et obtenir un code.
 * Le temps est figé pour vérifier exactement expirationAt = ouvertureAt + 15 min (RG1).
 */
@SpringBootTest
@AutoConfigureMockMvc
class Module2SessionsTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionCoursRepository sessions;

    @Autowired
    private HorlogeMetier horloge;

    @Test
    void postValideRenvoie201AvecCodeEtExpirationPlus15Minutes() throws Exception {
        LocalDateTime avant = horloge.maintenant();

        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Cours Architecture\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titre").value("Cours Architecture"))
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.ouvertureAt").exists())
                .andExpect(jsonPath("$.expirationAt").exists())
                .andExpect(jsonPath("$.clotureAt").doesNotExist())
                .andExpect(jsonPath("$.promotionId").value(1))
                .andReturn();

        String corps = resultat.getResponse().getContentAsString();
        LocalDateTime ouverture = LocalDateTime.parse(extraire(corps, "ouvertureAt"), ISO);
        LocalDateTime expiration = LocalDateTime.parse(extraire(corps, "expirationAt"), ISO);

        // RG1 : le code expire 15 minutes après l'ouverture
        assertThat(expiration).isEqualTo(ouverture.plusMinutes(15));
        assertThat(ouverture).isAfter(avant.minusSeconds(5));

        // RG16 : le code est bien persisté
        String code = extraire(corps, "code");
        assertThat(sessions.existsByCode(code)).isTrue();
    }

    @Test
    void deuxSessionsOntDesCodesDifferents() throws Exception {
        String code1 = ouvrirEtExtraireCode("Session A");
        String code2 = ouvrirEtExtraireCode("Session B");
        assertThat(code1).isNotEqualTo(code2);
    }

    @Test
    void titreManquantRenvoie400ChampManquant() throws Exception {
        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"promotionId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void promotionIdManquantRenvoie400ChampManquant() throws Exception {
        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Sans promotion\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    void promotionInconnueRenvoie404() throws Exception {
        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Cours fantôme\",\"promotionId\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    @Test
    void getSessionRenvoieLeDetailEt404SiInconnue() throws Exception {
        String code = ouvrirEtExtraireCode("Session consultable");
        Long id = sessions.findByCode(code).orElseThrow().getId();

        mockMvc.perform(get("/api/sessions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code));

        mockMvc.perform(get("/api/sessions/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    void dureeCodeVautQuinzeMinutes() {
        // RG1 exprimé sur la constante métier : un seul endroit définit la règle
        assertThat(SessionService.DUREE_CODE).isEqualTo(java.time.Duration.ofMinutes(15));
    }

    // ---- utilitaires -------------------------------------------------------

    private String ouvrirEtExtraireCode(String titre) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"" + titre + "\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn();
        return extraire(resultat.getResponse().getContentAsString(), "code");
    }

    private static String extraire(String json, String champ) {
        var matcher = java.util.regex.Pattern
                .compile("\"" + champ + "\":\"?([^\",}]*)\"?")
                .matcher(json);
        assertThat(matcher.find()).as("champ %s présent dans %s", champ, json).isTrue();
        return matcher.group(1);
    }
}
