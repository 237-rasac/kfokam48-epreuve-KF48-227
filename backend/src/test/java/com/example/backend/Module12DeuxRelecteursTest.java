package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.backend.repository.RelectureRepository;
import com.example.backend.repository.SessionCoursRepository;

/**
 * MODULE 12 (issue #41) — contrat v2 : deux relecteurs, moyenne et note
 * provisoire (EF4, EF5, EF11, RG3, RG4 modifiée, RG5, RG8, RG17).
 */
@SpringBootTest
@AutoConfigureMockMvc
class Module12DeuxRelecteursTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionCoursRepository sessions;

    @Autowired
    private RelectureRepository relectures;

    @Test
    void unSeulRenduNoteProvisoireEtExerciceEnAttente() throws Exception {
        // 2 relecteurs assignés ; un seul rend → moyenne affichée, provisoire
        // true, statut EN_ATTENTE (EF5 : RELU seulement quand TOUT est rendu)
        Long exerciceId = deposerAvecDeuxRelecteurs();

        rendre(1);

        mockMvc.perform(get("/api/exercices/" + exerciceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.note").value(16.0)) // moyenne d'une seule relecture
                .andExpect(jsonPath("$.provisoire").value(true))
                .andExpect(jsonPath("$.relecturesAttendues").value(2))
                .andExpect(jsonPath("$.relecturesRendues").value(1))
                .andExpect(jsonPath("$.commentaires.length()").value(1))
                .andExpect(jsonPath("$.commentaires[0]").value("Très bon travail."));
    }

    @Test
    void deuxRendusMoyenneDefinitiveEtStatutReplu() throws Exception {
        Long exerciceId = deposerAvecDeuxRelecteurs();

        rendre(1); // note 16 → provisoire
        rendre(2); // note 12 → moyenne (16+12)/2 = 14, définitive, RELU

        mockMvc.perform(get("/api/exercices/" + exerciceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RELU"))
                .andExpect(jsonPath("$.note").value(14.0))
                .andExpect(jsonPath("$.provisoire").value(false))
                .andExpect(jsonPath("$.relecturesAttendues").value(2))
                .andExpect(jsonPath("$.relecturesRendues").value(2))
                .andExpect(jsonPath("$.commentaires.length()").value(2));
    }

    @Test
    void lesCommentairesNExposentJamaisLIdentiteDesRelecteurs() throws Exception {
        Long exerciceId = deposerAvecDeuxRelecteurs();

        rendre(1);
        rendre(2);

        String corps = mockMvc.perform(get("/api/exercices/" + exerciceId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // EF11 : ni relecteurId, ni relecteur, ni les noms de la promotion V2
        assertThat(corps).doesNotContain("relecteur").doesNotContain("Boris").doesNotContain("Clarisse");
    }

    @Test
    void assignerManuellementLeSecondRelecteurPuis409AuTroisieme() throws Exception {
        // Pool insuffisant : auteur + 1 présent → une seule relecture auto (RG8)
        String code = ouvrirSessionAvecPresents(1, 2);
        Long sessionId = sessions.findByCode(code).orElseThrow().getId();
        Long exerciceId = deposer(sessionId, 1);
        assertThat(relectures.findByExerciceId(exerciceId)).hasSize(1);

        // Le formateur complète vers le second relecteur (issue #41)
        mockMvc.perform(post("/api/exercices/" + exerciceId + "/assigner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"relecteurId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relecteurId").value(3));
        assertThat(relectures.findByExerciceId(exerciceId)).hasSize(2);

        // RG4 modifiée : pas un troisième relecteur ni un doublon → 409
        mockMvc.perform(post("/api/exercices/" + exerciceId + "/assigner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"relecteurId\":4}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_ASSIGNEE"));
        mockMvc.perform(post("/api/exercices/" + exerciceId + "/assigner")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"relecteurId\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_ASSIGNEE"));
    }

    @Test
    void poolDUnSeulPresentHorsAuteurNoteRendueDefinitive() throws Exception {
        // Auteur (1) + un seul autre présent (2) : une seule relecture assignée ;
        // sa note rendue est DÉFINITIVE (convention §7 : pas de provisoire sans
        // deuxième relecture attendue)
        String code = ouvrirSessionAvecPresents(1, 2);
        Long sessionId = sessions.findByCode(code).orElseThrow().getId();
        Long exerciceId = deposer(sessionId, 1);

        Long relectureId = relectures.findByExerciceId(exerciceId).get(0).getId();
        mockMvc.perform(post("/api/relectures/" + relectureId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":15,\"commentaire\":\"Bien.\",\"relecteurId\":2}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/exercices/" + exerciceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RELU"))
                .andExpect(jsonPath("$.note").value(15.0))
                .andExpect(jsonPath("$.provisoire").value(false))
                .andExpect(jsonPath("$.relecturesAttendues").value(1))
                .andExpect(jsonPath("$.relecturesRendues").value(1));
    }

    // ---- utilitaires -------------------------------------------------------

    /** Dépose un exercice dans une session à 3 présents hors auteur → 2 relectures. */
    private Long deposerAvecDeuxRelecteurs() throws Exception {
        String code = ouvrirSessionAvecPresents(1, 2, 3, 4);
        Long sessionId = sessions.findByCode(code).orElseThrow().getId();
        Long exerciceId = deposer(sessionId, 1);
        assertThat(relectures.findByExerciceId(exerciceId)).hasSize(2);
        return exerciceId;
    }

    /** Rend la i-ème relecture (1 ou 2) de l'exercice précédent avec note fixe. */
    private void rendre(int position) throws Exception {
        // Les relectures de l'exercice viennent d'être créées : id croissants.
        // On retrouve l'exercice via la dernière relecture enregistrée.
        var toutes = relectures.findAll();
        var lesDeuxDernieres = toutes.stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .limit(2)
                .toList();
        var cible = lesDeuxDernieres.get(lesDeuxDernieres.size() - position);
        int note = position == 1 ? 16 : 12;
        String commentaire = position == 1 ? "Très bon travail." : "Correct mais incomplet.";
        mockMvc.perform(post("/api/relectures/" + cible.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":" + note + ",\"commentaire\":\"" + commentaire
                        + "\",\"relecteurId\":" + cible.getRelecteur().getId() + "}"))
                .andExpect(status().isOk());
    }

    private String ouvrirSessionAvecPresents(Integer... etudiantsPresents) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Session MODULE 12\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andReturn();
        String code = extraire(resultat.getResponse().getContentAsString(), "code");
        for (Integer etudiantId : etudiantsPresents) {
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
        return Long.valueOf(extraire(resultat.getResponse().getContentAsString(), "id"));
    }

    private static String extraire(String json, String champ) {
        var matcher = java.util.regex.Pattern.compile("\"" + champ + "\":\"?([^\",}]*)\"?").matcher(json);
        assertThat(matcher.find()).as("champ %s présent dans %s", champ, json).isTrue();
        return matcher.group(1);
    }
}
