package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.example.backend.domaine.Exercice;
import com.example.backend.domaine.SessionCours;
import com.example.backend.repository.ExerciceRepository;
import com.example.backend.repository.RelectureRepository;
import com.example.backend.repository.SessionCoursRepository;

/**
 * MODULE 5 — EF3, EF4, EF10, EF11, RG3, RG4, RG9, RG10 : dépôt d'exercice,
 * assignation, remplacement du lien, consultation sans identité du relecteur.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Module5ExercicesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionCoursRepository sessions;

    @Autowired
    private ExerciceRepository exercices;

    @Autowired
    private RelectureRepository relectures;

    @Test
    void depotValideRenvoie201AvecStatutEtAssignation() throws Exception {
        String code = ouvrirSessionAvecDeuxPresents(2, 3);
        Long sessionId = sessions.findByCode(code).orElseThrow().getId();

        MvcResult resultat = mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":2,\"lien\":\"https://github.com/amina/exo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.etudiantId").value(2))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.relecteurAssignee").value(true))
                .andReturn();

        // EF4/RG4 : une relecture créée, relecteur distinct de l'auteur (RG3)
        Long exerciceId = extraireLong(resultat.getResponse().getContentAsString(), "id");
        var relecture = relectures.findByExerciceId(exerciceId).orElseThrow();
        assertThat(relecture.getRelecteur().getId()).isNotEqualTo(2L);
        assertThat(relecture.getRendueAt()).isNull();
    }

    @Test
    void poolVideLeDepotReussitMaisSansRelecteur() throws Exception {
        // Session neuve où seule Amina est présente : elle dépose, personne d'autre (RG8)
        String code = ouvrirSessionAvecDeuxPresents(1, null);
        Long sessionId = sessions.findByCode(code).orElseThrow().getId();

        MvcResult resultat = mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":1,\"lien\":\"https://github.com/amina/exo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relecteurAssignee").value(false))
                .andReturn();

        Long exerciceId = extraireLong(resultat.getResponse().getContentAsString(), "id");
        assertThat(relectures.findByExerciceId(exerciceId)).isEmpty(); // RG8
    }

    @Test
    void secondDepotRenvoie409ExerciceDejaDepose() throws Exception {
        String code = ouvrirSessionAvecDeuxPresents(2, 3);
        Long sessionId = sessions.findByCode(code).orElseThrow().getId();
        deposer(sessionId, 2);

        mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":2,\"lien\":\"https://github.com/amina/exo2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXERCICE_DEJA_DEPOSE"));
    }

    @Test
    void depotApresClotureRenvoie410() throws Exception {
        String code = ouvrirSessionAvecDeuxPresents(2, 3);
        SessionCours session = sessions.findByCode(code).orElseThrow();
        session.setClotureAt(LocalDateTime.now());
        sessions.save(session);
        Long sessionId = session.getId();

        mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":2,\"lien\":\"https://x\"}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    void lienInvalideRenvoie400LienInvalide() throws Exception {
        String code = ouvrirSessionAvecDeuxPresents(2, 3);
        Long sessionId = sessions.findByCode(code).orElseThrow().getId();

        mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":2,\"lien\":\"pas un lien\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
    }

    @Test
    void patchLienAvantRelectureRenvoie200() throws Exception {
        String code = ouvrirSessionAvecDeuxPresents(2, 3);
        Long sessionId = sessions.findByCode(code).orElseThrow().getId();
        Long exerciceId = deposer(sessionId, 2);

        mockMvc.perform(patch("/api/exercices/" + exerciceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lien\":\"https://github.com/amina/exo-v2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lien").value("https://github.com/amina/exo-v2"));
    }

    @Test
    void getExerciceEnAttenteRenvoieNoteNullEtPasDeRelecteur() throws Exception {
        String code = ouvrirSessionAvecDeuxPresents(2, 3);
        Long sessionId = sessions.findByCode(code).orElseThrow().getId();
        Long exerciceId = deposer(sessionId, 2);

        // EF11 : pas d'identité du relecteur dans la réponse
        String corps = mockMvc.perform(get("/api/exercices/" + exerciceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.note").doesNotExist())
                .andExpect(jsonPath("$.commentaire").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(corps).doesNotContain("relecteur");
    }

    @Test
    void getExerciceInconnuRenvoie404() throws Exception {
        mockMvc.perform(get("/api/exercices/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCICE_INCONNU"));
    }

    @Test
    void patchApresClotureRenvoie410() throws Exception {
        String code = ouvrirSessionAvecDeuxPresents(2, 3);
        Long sessionId = sessions.findByCode(code).orElseThrow().getId();
        Long exerciceId = deposer(sessionId, 2);

        // On clôture APRÈS le dépôt (RG10 : le remplacement est bloqué par la clôture)
        SessionCours session = sessions.findById(sessionId).orElseThrow();
        session.setClotureAt(LocalDateTime.now());
        sessions.save(session);

        mockMvc.perform(patch("/api/exercices/" + exerciceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lien\":\"https://x\"}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    // ---- utilitaires -------------------------------------------------------

    /** Ouvre une session et y marque présents les étudiants donnés (voie formateur ; null = ignorer). */
    private String ouvrirSessionAvecDeuxPresents(Integer etudiantA, Integer etudiantB) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Session MODULE 5\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn();
        String code = extraire(resultat.getResponse().getContentAsString(), "code");

        for (Integer etudiantId : new Integer[] { etudiantA, etudiantB }) {
            if (etudiantId == null) {
                continue;
            }
            mockMvc.perform(post("/api/presences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId
                            + ",\"source\":\"FORMATEUR\"}"))
                    .andExpect(status().isCreated());
        }
        return code;
    }

    private Long deposer(Long sessionId, int etudiantId) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + etudiantId
                        + ",\"lien\":\"https://github.com/etudiant/exo\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return extraireLong(resultat.getResponse().getContentAsString(), "id");
    }

    private static String extraire(String json, String champ) {
        var matcher = java.util.regex.Pattern.compile("\"" + champ + "\":\"?([^\",}]*)\"?").matcher(json);
        assertThat(matcher.find()).as("champ %s présent dans %s", champ, json).isTrue();
        return matcher.group(1);
    }

    private static Long extraireLong(String json, String champ) {
        return Long.valueOf(extraire(json, champ));
    }
}
