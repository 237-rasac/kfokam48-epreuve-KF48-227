package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.backend.domaine.SessionCours;
import com.example.backend.repository.SessionCoursRepository;

/**
 * MODULE 3 — EF2, EF12, RG1, RG2, RG15 : marquer sa présence avec un code.
 * Chaque test utilise un etudiantId distinct pour isoler les compteurs de
 * blocage EF12 (en mémoire, partagés dans le contexte Spring).
 */
@SpringBootTest
@AutoConfigureMockMvc
class Module3PresencesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionCoursRepository sessions;

    @Test
    void codeValideRenvoie201AvecSourceEtudiant() throws Exception {
        String code = ouvrirSession();
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.etudiantId").value(2))
                .andExpect(jsonPath("$.source").value("ETUDIANT"))
                .andExpect(jsonPath("$.marqueeAt").exists());
    }

    @Test
    void codeInconnuRenvoie400CodeInconnu() throws Exception {
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"ZZZZZZ\",\"etudiantId\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INCONNU"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void codeExpireRenvoie410CodeExpire() throws Exception {
        String code = ouvrirSession();
        // On force l'expiration : le code avait été émis il y a plus de 15 minutes (RG1)
        SessionCours session = sessions.findByCode(code).orElseThrow();
        session.setExpirationAt(LocalDateTime.now().minusMinutes(1));
        sessions.save(session);

        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":2}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"));
    }

    @Test
    void dejaPresentRenvoie409DejaPresent() throws Exception {
        String code = ouvrirSession();
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":2}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    @Test
    void sessionClotureeRenvoie410SessionCloturee() throws Exception {
        String code = ouvrirSession();
        SessionCours session = sessions.findByCode(code).orElseThrow();
        session.setClotureAt(LocalDateTime.now());
        sessions.save(session);

        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":2}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    void cinqErreursBlocentLEtudiantMemeAvecUnCodeValide() throws Exception {
        String code = ouvrirSession();
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/presences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"ZZZZZZ\",\"etudiantId\":3}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CODE_INCONNU"));
        }
        // EF12/RG14 : même avec un code valide, l'étudiant est bloqué
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":3}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ETUDIANT_BLOQUE"))
                .andExpect(jsonPath("$.message").value("Trop d'erreurs, réessayez dans 2 minutes."));
    }

    @Test
    void champManquantRenvoie400() throws Exception {
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"etudiantId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    void unePresenceReussieRemetLeCompteurDErreursAZero() throws Exception {
        String code = ouvrirSession();
        // 4 erreurs (sous le seuil de 5)
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/presences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"ZZZZZZ\",\"etudiantId\":5}"))
                    .andExpect(status().isBadRequest());
        }
        // Un code valide remet le compteur à zéro (issue #15)
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":5}"))
                .andExpect(status().isCreated());
        // 4 nouvelles erreurs : pas de blocage (le compteur avait été remis à zéro)
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/presences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"ZZZZZZ\",\"etudiantId\":5}"))
                    .andExpect(status().isBadRequest());
        }
        // Le code de la session est expiré à ce stade ? Non : on vérifie juste que
        // l'étudiant n'est PAS bloqué en envoyant un code expiré → 410 et non 429
        SessionCours session = sessions.findByCode(code).orElseThrow();
        session.setExpirationAt(LocalDateTime.now().minusMinutes(1));
        sessions.save(session);
        MvcResult resultat = mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":5}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"))
                .andReturn();
        assertThat(resultat.getResponse().getContentAsString()).contains("CODE_EXPIRE");
    }

    // ---- utilitaires -------------------------------------------------------

    private String ouvrirSession() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Session MODULE 3\",\"promotionId\":1}"))
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
