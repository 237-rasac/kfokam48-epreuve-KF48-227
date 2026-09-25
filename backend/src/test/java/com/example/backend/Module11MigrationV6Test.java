package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * MODULE 11 (issue #40) — migration V6 « deux relectures par exercice ».
 *
 * Critères d'acceptation couverts ici (le reste est prouvé hors test) :
 * - la base déjà remplie (données de démo V2) survit à la migration : la
 *   relecture existante reste valide (cette classe démarre = V1→V6 appliqué
 *   sur H2 avec les données V2 en place) ;
 * - la contrainte uk_relecture_exercice_relecteur est active : un second
 *   relecteur distinct est accepté, le même relecteur est refusé.
 *
 * V1–V5 inchangés : prouvable au git log de la PR. L'application complète sur
 * PostgreSQL 16 (docker compose up) est vérifiée manuellement (ENF5).
 */
@SpringBootTest
class Module11MigrationV6Test {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void lesDonneesDemoV2SurviventALaMigration() {
        // Données V2 intactes après V1→V6 : la relecture existante (exercice 2,
        // relecteur 1, note 15) a survécu — sa note devient simplement provisoire
        // tant que le second relecteur attendu n'a pas rendu (RG4 modifiée, RG17).
        Integer nbRelectures = jdbc.queryForObject("select count(*) from relecture", Integer.class);
        assertThat(nbRelectures).isEqualTo(1);

        Map<String, Object> relecture = jdbc.queryForMap(
                "select exercice_id, relecteur_id, note, commentaire, rendue_at from relecture where id = 1");
        assertThat(((Number) relecture.get("EXERCICE_ID")).longValue()).isEqualTo(2L);
        assertThat(((Number) relecture.get("RELECTEUR_ID")).longValue()).isEqualTo(1L);
        assertThat(((Number) relecture.get("NOTE")).intValue()).isEqualTo(15);
        assertThat(relecture.get("COMMENTAIRE")).isEqualTo("Bon travail, attention aux cas limites.");
        assertThat(relecture.get("RENDUE_AT")).isNotNull();

        // La contrainte unique (exercice_id, relecteur_id) existe bien
        // (upper() : H2 stocke en minuscules, PostgreSQL en minuscules non cité — portable)
        Integer nbContrainte = jdbc.queryForObject(
                "select count(*) from information_schema.table_constraints "
                + "where upper(table_name) = 'RELECTURE' and upper(constraint_name) = 'UK_RELECTURE_EXERCICE_RELECTEUR'",
                Integer.class);
        assertThat(nbContrainte).as("contrainte uk_relecture_exercice_relecteur présente").isEqualTo(1);

        // ... et l'ancienne contrainte uk_relecture_exercice a disparu
        Integer nbAncienne = jdbc.queryForObject(
                "select count(*) from information_schema.table_constraints "
                + "where upper(table_name) = 'RELECTURE' and upper(constraint_name) = 'UK_RELECTURE_EXERCICE'",
                Integer.class);
        assertThat(nbAncienne).as("ancienne contrainte uk_relecture_exercice supprimée").isZero();
    }

    @Test
    void deuxRelecteursDistinctsSontAcceptesSurLeMemeExercice() {
        // La relecture V2 porte sur l'exercice 2 (relecteur 1) : un second
        // relecteur distinct (relecteur 2) doit désormais être accepté.
        jdbc.update("insert into relecture (exercice_id, relecteur_id, assigned_by) "
                + "values (2, 2, 'SYSTEME')");
        Integer nb = jdbc.queryForObject(
                "select count(*) from relecture where exercice_id = 2", Integer.class);
        assertThat(nb).isEqualTo(2);
    }

    @Test
    void leMemeRelecteurNePeutPasEtreAssigneeDeuxFois() {
        // uk_relecture_exercice_relecteur : le relecteur 1 a déjà une relecture
        // sur l'exercice 2 (données V2) — une seconde doit être rejetée (409
        // RELECTURE_DEJA_ASSIGNEE côté backend, MODULE 12).
        assertThatThrownBy(() -> jdbc.update(
                "insert into relecture (exercice_id, relecteur_id, assigned_by) values (2, 1, 'SYSTEME')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laTableRelectureHistoriqueResteFonctionnelle() {
        // V4 référence relecture : la migration ne doit rien avoir cassé en cascade.
        List<Map<String, Object>> lignes = jdbc.queryForList("select id from relecture_historique");
        assertThat(lignes).isEmpty();

        jdbc.update("insert into relecture_historique (relecture_id, ancienne_note, ancien_commentaire, "
                + "nouvelle_note, nouveau_commentaire, modifie_at) "
                + "values (1, 15, 'avant', 16, 'apres', current_timestamp)");
        Integer nb = jdbc.queryForObject("select count(*) from relecture_historique", Integer.class);
        assertThat(nb).isEqualTo(1);
    }
}
