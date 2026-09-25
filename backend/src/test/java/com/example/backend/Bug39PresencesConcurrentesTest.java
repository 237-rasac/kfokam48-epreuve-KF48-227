package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.backend.domaine.SessionCours;
import com.example.backend.repository.PresenceRepository;
import com.example.backend.repository.SessionCoursRepository;

/**
 * Issue #39 — bug remonté par le client (enveloppe étape 3) : « Ils ont tapé le
 * code presque en même temps et il n'y en a qu'un seul qui apparaît dans ma
 * liste. J'ai réessayé une fois, cette fois les deux sont passés. »
 *
 * Cause : le marquage de présence (EF2, RG15) est un check-then-act non
 * atomique — {@code existsBySessionIdAndEtudiantId} puis {@code save}. Quand
 * deux requêtes traversent le contrôle avant que la première n'ait commité,
 * la contrainte uk_presence_session_etudiant (V1) rejette le second insert en
 * DataIntegrityViolationException, traduite en 500 ERREUR_INTERNE au lieu du
 * 409 DEJA_PRESENT du contrat — et la présence est perdue côté formateur.
 *
 * Le test force l'entrelacement exact (pas de course au timing) : une passerelle
 * de test (@Primary, proxy JDK sur le repository) gèle le PREMIER save avant
 * son INSERT ; la deuxième requête passe alors le contrôle RG15 pendant que la
 * première transaction est encore en vol. Ce test est committé AVANT le
 * correctif : l'ordre test rouge → correctif est évalué.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Bug39PresencesConcurrentesTest.PasserelleDeTest.class)
class Bug39PresencesConcurrentesTest {

    private static final Logger log = LoggerFactory.getLogger(Bug39PresencesConcurrentesTest.class);

    /** Drapeaux partagés entre la classe de test et la passerelle (un seul contexte Spring). */
    static final CountDownLatch PREMIER_INSERT_ATTRAPE = new CountDownLatch(1);
    static final CountDownLatch AUTORISER_INSERT = new CountDownLatch(1);
    static final AtomicBoolean GELER_LE_PREMIER_SAVE = new AtomicBoolean(true);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionCoursRepository sessions;

    private ExecutorService pool;

    @BeforeEach
    void preparerPasserelle() {
        PREMIER_INSERT_ATTRAPE.countDown();
        AUTORISER_INSERT.countDown();
        GELER_LE_PREMIER_SAVE.set(true);
        pool = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void relacherEtFermer() {
        AUTORISER_INSERT.countDown();
        pool.shutdownNow();
    }

    @TestConfiguration
    static class PasserelleDeTest {

        @Bean
        @Primary
        PresenceRepository presenceRepositoryGardien(PresenceRepository delegue) {
            return (PresenceRepository) Proxy.newProxyInstance(
                    PresenceRepository.class.getClassLoader(),
                    new Class<?>[] { PresenceRepository.class },
                    (proxy, method, args) -> {
                        if ("save".equals(method.getName())
                                && GELER_LE_PREMIER_SAVE.compareAndSet(true, false)) {
                            // Le contrôle RG15 a répondu « absent » : on gèle la
                            // transaction AVANT l'INSERT, comme un ralentissement réseau
                            PREMIER_INSERT_ATTRAPE.countDown();
                            AUTORISER_INSERT.await(15, TimeUnit.SECONDS);
                        }
                        try {
                            return method.invoke(delegue, args);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
        }
    }

    @Test
    void lePerdantDeLaCourseSortEn409DejaPresentPasEn500() throws Exception {
        String code = ouvrirSession();
        Long sessionId = sessions.findByCode(code).orElseThrow().getId();

        // Requête A : gélée avant son INSERT (contrôle RG15 déjà passé)
        Future<Reponse> a = marquerAsync(code, 1);
        assertThat(PREMIER_INSERT_ATTRAPE.await(5, TimeUnit.SECONDS)).isTrue();

        // Requête B : traverse le contrôle RG15 pendant que A est en vol
        Reponse b = marquerAsync(code, 1).get(15, TimeUnit.SECONDS);

        // On relâche A : son INSERT heurte uk_presence_session_etudiant
        AUTORISER_INSERT.countDown();
        Reponse reponseA = a.get(15, TimeUnit.SECONDS);
        log.info("Requête A → {} {}", reponseA.statut(), reponseA.corps());
        log.info("Requête B → {} {}", b.statut(), b.corps());

        // Contrat : le perdant sort en 409 DEJA_PRESENT, jamais en 500
        assertThat(List.of(reponseA.statut(), b.statut())).containsExactlyInAnyOrder(201, 409);
        Reponse perdante = reponseA.statut() == 201 ? b : reponseA;
        assertThat(perdante.corps()).contains("DEJA_PRESENT");

        // RG15 : exactement une présence dans la liste du formateur
        mockMvc.perform(get("/api/sessions/{id}/presences", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].etudiantId").value(1))
                .andExpect(jsonPath("$[0].source").value("ETUDIANT"));
    }

    @Test
    void deuxEtudiantsEnRafaleApparaissentTousLesDeuxDansLaListe() throws Exception {
        String code = ouvrirSession();
        Long sessionId = sessions.findByCode(code).orElseThrow().getId();

        // L'étudiant 1 est gélé avant son INSERT — comme si sa requête traînait
        Future<Reponse> etudiant1 = marquerAsync(code, 1);
        assertThat(PREMIER_INSERT_ATTRAPE.await(5, TimeUnit.SECONDS)).isTrue();

        // L'étudiant 2 passe pendant ce temps : sa présence doit être comptée
        Reponse etudiant2 = marquerAsync(code, 2).get(15, TimeUnit.SECONDS);

        AUTORISER_INSERT.countDown();
        Reponse reponseEtudiant1 = etudiant1.get(15, TimeUnit.SECONDS);
        log.info("Étudiant 1 → {} {}", reponseEtudiant1.statut(), reponseEtudiant1.corps());
        log.info("Étudiant 2 → {} {}", etudiant2.statut(), etudiant2.corps());

        // Aucune des deux requêtes ne doit échouer — le bug remonté perdait
        // une présence (500 au lieu de 201)
        assertThat(List.of(reponseEtudiant1.statut(), etudiant2.statut()))
                .containsExactlyInAnyOrder(201, 201);

        // Le symptôme vu par le formateur : les DEUX étudiants dans la liste
        mockMvc.perform(get("/api/sessions/{id}/presences", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ---- utilitaires -------------------------------------------------------

    private record Reponse(int statut, String corps) {
    }

    private Future<Reponse> marquerAsync(String code, long etudiantId) {
        return pool.submit(() -> {
            MvcResult resultat = mockMvc.perform(post("/api/presences")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId + "}"))
                    .andReturn();
            return new Reponse(resultat.getResponse().getStatus(),
                    resultat.getResponse().getContentAsString());
        });
    }

    private String ouvrirSession() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Session BUG #39\",\"promotionId\":1}"))
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
