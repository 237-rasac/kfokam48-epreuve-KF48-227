package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * MODULE 4 — EF7, RG11 : le formateur ajoute une présence manuellement,
 * marquée source=FORMATEUR. Chaque test utilise un etudiantId distinct pour
 * isoler les compteurs EF12 partagés du contexte Spring.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Module4PresenceManuelleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionCoursRepository sessions;

    @Test
    void ajoutManuelRenvoie201AvecSourceFormateur() throws Exception {
        String code = ouvrirSession();
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":3,\"source\":\"FORMATEUR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.etudiantId").value(3))
                .andExpect(jsonPath("$.source").value("FORMATEUR"))
                .andExpect(jsonPath("$.marqueeAt").exists());
    }

    @Test
    void sansSourceLaVoieEtudiantSapplique() throws Exception {
        String code = ouvrirSession();
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("ETUDIANT"));
    }

    @Test
    void ajoutManuelApresExpirationDuCodeFonctionne() throws Exception {
        String code = ouvrirSession();
        SessionCours session = sessions.findByCode(code).orElseThrow();
        session.setExpirationAt(LocalDateTime.now().minusMinutes(1));
        sessions.save(session);

        // EF7/RG11 : le formateur rattrape les étudiants qui arrivent après l'expiration
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":4,\"source\":\"FORMATEUR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("FORMATEUR"));
    }

    @Test
    void doublonRenvoie409QuelQueSoitLaSource() throws Exception {
        String code = ouvrirSession();
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":5,\"source\":\"FORMATEUR\"}"))
                .andExpect(status().isCreated());
        // L'étudiant essaie ensuite de se marquer lui-même : RG15 s'applique
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    @Test
    void sessionClotureeRenvoie410MemePourLeFormateur() throws Exception {
        String code = ouvrirSession();
        SessionCours session = sessions.findByCode(code).orElseThrow();
        session.setClotureAt(LocalDateTime.now());
        sessions.save(session);

        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":3,\"source\":\"FORMATEUR\"}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    void etudiantInconnuRenvoie404() throws Exception {
        String code = ouvrirSession();
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":999,\"source\":\"FORMATEUR\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    @Test
    void sourceInvalideRenvoie400() throws Exception {
        String code = ouvrirSession();
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":2,\"source\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    void laPresenceManuelleApparaitDansLesPresencesDeLaSession() throws Exception {
        String code = ouvrirSession();
        mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":3,\"source\":\"FORMATEUR\"}"))
                .andExpect(status().isCreated());

        Long sessionId = sessions.findByCode(code).orElseThrow().getId();
        MvcResult resultat = mockMvc.perform(get("/api/sessions/" + sessionId + "/presences"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(resultat.getResponse().getContentAsString())
                .contains("\"etudiantId\":3")
                .contains("\"source\":\"FORMATEUR\"");
    }

    // ---- utilitaires -------------------------------------------------------

    private String ouvrirSession() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Session MODULE 4\",\"promotionId\":1}"))
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
