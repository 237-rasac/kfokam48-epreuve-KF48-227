# Rapport — MODULE 1 : Socle technique et données de démonstration

Issue : #13 · PR : #23 (fusionnée sur `main`) · Vérifié le 2026-09-25

## Verdict

**Validé avec réserves.** Le socle backend et frontend fonctionne et tous les tests passent. Deux critères de l'issue ne sont pas encore remplis : le README de démarrage et le lancement du frontend via `docker compose`.

## Ce qui a été implémenté

| Élément de l'issue #13 | État | Où |
|---|---|---|
| Spring Boot, Maven, wrapper `mvnw` | ✅ | `backend/pom.xml` (Java 25, Spring Boot 4.1.1) |
| Flyway, migration des 6 tables | ✅ | `V1__schema_initial.sql` |
| H2 pour les tests, sans base locale (ENF6) | ✅ | `src/test/resources/application.properties` |
| Erreurs `{code, message}` via `@RestControllerAdvice` (ENF4) | ✅ | `erreur/GestionnaireErreurs.java` |
| Données de démo : 1 promotion, 5 étudiants | ✅ | `V2__donnees_demo.sql`, séquences réalignées par `V3` |
| Entités, repositories et API du référentiel | ✅ | `GET /api/promotions`, `GET /api/etudiants?promotionId=` |
| Projet React, build qui passe | ✅ | `npm run build` |
| Couche API dédiée `src/api/` | ✅ | `client.ts`, `endpoints.ts`, `types.ts` |
| Trois écrans : formateur, étudiant, relecteur | ⚠️ En partie | Les trois écrans existent, mais étudiant et relecteur sont encore vides (modules 3, 5 et 6) |
| Critère : le front affiche les 5 étudiants | ✅ | Écran de sélection et espace formateur |
| Critère : `docker compose up` ou 3 commandes dans le README | ❌ | Pas de `README.md` à la racine. Le compose ne lance que la base et le backend. |

## Tests exécutés

| Suite | Résultat |
|---|---|
| Backend `Module1SocleTest` : 5 étudiants chargés, `GET /api/promotions`, `GET /api/etudiants`, erreur 404 au format du contrat | 4/4 ✅ |
| Backend `BackendApplicationTests.contextLoads` | 1/1 ✅ |
| Frontend `ecrans.test.tsx` : sélection, mémorisation de l'étudiant, erreur normalisée | 3/3 ✅ |
| Frontend `client.test.ts` : couche API | 4/4 ✅ |
| `npm run build` | ✅ |
| `npm run lint` | ✅ après correction (2 erreurs au départ) |
| Stack Docker réelle : `GET /api/promotions`, route inconnue | ✅ 200, et 404 `RESSOURCE_INCONNUE` |

## Anomalies trouvées et corrigées

1. **Lint en erreur, `useReferentiel.ts`** : `setErreur(null)` était appelé directement dans le `useEffect`, ce qui déclenche des rendus en cascade (règle `react-hooks/set-state-in-effect`). L'erreur est maintenant effacée dans `recharger()`, au moment du clic sur « Réessayer ».
2. **Lint en erreur, `components/ui/button.tsx`** : l'export de `buttonVariants` enfreint la règle `react-refresh/only-export-components`. C'est la convention shadcn, donc la règle est désactivée sur cette ligne, avec un commentaire.

## Remarques non bloquantes

- Une mauvaise méthode HTTP (`DELETE /api/promotions`) ou un mauvais `Content-Type` renvoie **500 `ERREUR_INTERNE`** au lieu de 405 ou 415, et écrit une fausse erreur dans les logs. Le format `{code, message}` reste respecté. À traiter en ajoutant des gestionnaires pour `HttpRequestMethodNotSupportedException` et `HttpMediaTypeNotSupportedException`.
- Le code `RESSOURCE_INCONNUE` n'est pas dans le catalogue `x-codes-erreur` du contrat. Il faut l'y ajouter.
- Le service `backend` du compose n'a pas de `healthcheck`, donc `docker compose up --wait` rend la main avant que l'API réponde.
- `package.json` s'appelle encore `my-react-app`, et `components.json.bak` est commité.

## Reste à faire pour clore le module

- Écrire le `README.md` racine avec le démarrage en 3 commandes au maximum.
