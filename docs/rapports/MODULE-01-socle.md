# Rapport — MODULE 1 : Socle technique et données de démonstration

Issue : #13 · Réf. ENF4, ENF5, ENF6 · PR : #23 (fusionnée sur `main`) · Vérifié le 2026-09-25

## Verdict

**Validé.** Le socle backend et les écrans front sont en place, les données de démonstration sont chargées et servies.

## Ce qui a été implémenté

| Élément de l'issue #13 | État | Où |
|---|---|---|
| Projet Spring Boot (web, validation, data-jpa), wrapper `mvnw` commité | ✅ | préexistant, complété |
| Entités JPA conformes au schéma V1 (`ddl-auto=validate`) | ✅ | `domaine/` — Promotion, Etudiant, SessionCours, Presence, Exercice, Relecture |
| Format d'erreur commun `{ code, message }` via `@RestControllerAdvice`, jamais de stack trace (ENF4) | ✅ | `erreur/GestionnaireErreurs` + `ErreurMetierException` |
| Données de démonstration : 1 promotion, 5 étudiants | ✅ | migration V2 (préexistante), servies par l'API |
| `GET /api/promotions`, `GET /api/etudiants` | ✅ | `ReferentielController` |
| Couche API front dédiée avec erreurs `{ code, message }` | ✅ | `src/api/` (client.ts, types.ts, endpoints.ts) — préexistante |
| Trois écrans : formateur, étudiant, relecteur | ✅ | `pages/` + sélection d'identité (pas d'auth, cahier §3) |
| Test : le contexte Spring démarre | ✅ | `BackendApplicationTests` |
| Test : les 5 étudiants de démonstration sont chargés | ✅ | `Module1SocleTest` |
| `docker compose up` suffit à lancer l'application | ✅ | vérifié à chaque E2E |

## Tests exécutés

| Suite | Résultat |
|---|---|
| Backend `Module1SocleTest` : 5 étudiants chargés, promotions servies, étudiants servies, 404 `PROMOTION_INCONNUE` au format du contrat | 4/4 ✅ |
| Frontend `ecrans.test.tsx` : sélection d'identité, accès formateur, persistance du choix, erreur contractuelle | 3/3 ✅ |

## Remarque

Le repository Spring Data a été introduit dès ce module pour les six tables : les méthodes requises par les modules suivants (`existsByCode`, `existsBySessionIdAndEtudiantId`, `findByRelecteurIdAndRendueAtIsNull`…) sont déclarées à l'avance.
