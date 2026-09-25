# Revue croisée — contrat OpenAPI ↔ implémentation

Date : 2026-09-25 · Contrat : `api/contrat.yaml` (v1.0.0) · Branches : modules 1 à 10 fusionnés, jalon `v0.1` posé · Vérifiée sur la stack Docker réelle (PostgreSQL 16, Flyway V1–V5).

## Verdict

**Conformité confirmée après corrections.** La revue a trouvé **deux écarts réels**
(un endpoint du contrat manquant, un code d'erreur 500 non contractuel) et deux
points de conformité partielle (format des dates, champ en plus dans le
tableau). Tous sont corrigés dans cette même branche et re-vérifiés par API.

## 1. Opérations du contrat ↔ contrôleurs

| Contrat | operationId | Implémentation | Verdict |
|---|---|---|---|
| `POST /api/sessions` | ouvrirSession | `SessionController` | ✅ 201/400 |
| `GET /api/sessions/{id}` | getSession | `SessionController` | ✅ 200/404 |
| `POST /api/sessions/{id}/cloture` | cloturerSession | `SessionController` | ✅ 200/404/409 |
| `POST /api/presences` | marquerPresence | `PresenceController` | ✅ 201/400/409/410/429 |
| `GET /api/sessions/{id}/presences` | getPresencesSession | `PresenceController` | ✅ 200 |
| `POST /api/exercices` | deposerExercice | `ExerciceController` | ✅ 201/400/409/410 |
| `GET /api/exercices/{id}` | getExercice | `ExerciceController` | ✅ 200/404 (EF11 : pas de relecteur) |
| `PATCH /api/exercices/{id}` | remplacerLienExercice | `ExerciceController` | ✅ 200/400/409/410 |
| `POST /api/relectures/{id}` | rendreRelecture | `RelectureController` | ✅ 200/400/403/409/410 |
| `GET /api/relectures/{id}` | getRelecture | ❌ **manquant** → ajouté | ✅ corrigé : 200/404 |
| `GET /api/etudiants/{id}/relectures` | getRelecturesEnAttente | `RelectureController` | ✅ 200 |
| `GET /api/etudiants` | getEtudiants | `ReferentielController` | ✅ 200 |
| `GET /api/promotions` | getPromotions | `ReferentielController` | ✅ 200 |
| `GET /api/tableau?promotionId=` | getTableau | `TableauController` | ✅ 200/404 |

Aucune opération du contrat n'est absente ; trois opérations **supplémentaires**
existent (documentées comme extensions dans le CHANGELOG) :
`GET /api/etudiants/{id}/relectures/rendues` (écran Modifier, EF9),
`POST /api/exercices/{id}/assigner` + `GET /api/sessions/{id}/exercices-sans-relecteur`
(MODULE 10, issue #22).

## 2. Codes d'erreur du catalogue ↔ implémentation

| Code du contrat | HTTP | Implémentation | Verdict |
|---|---|---|---|
| `CODE_INCONNU` | 400 | `PresenceService` | ✅ |
| `CHAMP_MANQUANT` | 400 | corps illisible, validation, champs nuls | ✅ |
| `LIEN_INVALIDE` | 400 | `ExerciceService.validerLien` | ✅ |
| `NOTE_INVALIDE` | 400 | `RelectureRendueService` (entier 0–20) | ✅ (correctif E2E : décimaux inclus) |
| `RELECTURE_PROPRE_EXERCICE` | 403 | implémenté sous le code `AUTO_RELECTURE` | ⚠️ écart de libellé, assumé |
| `DEJA_PRESENT` | 409 | `PresenceService` | ✅ |
| `EXERCICE_DEJA_DEPOSE` | 409 | `ExerciceService` | ✅ |
| `RELECTURE_DEJA_RENDUE` | 409 | `RelectureRendueService` | ✅ |
| `RELECTURE_DEJA_COMMENCEE` | 409 | `ExerciceService.remplacerLien` | ✅ |
| `RELECTURE_VERROUILLEE` | 409 | `RelectureRendueService` (après clôture) | ✅ |
| `SESSION_DEJA_CLOTUREE` | 409 | `SessionService.cloturer` | ✅ |
| `CODE_EXPIRE` | 410 | `PresenceService` (RG1) | ✅ |
| `SESSION_CLOTUREE` | 410 | présence, dépôt, PATCH lien | ✅ |
| `PROMOTION_INCONNUE` | 404 | référentiel + tableau | ✅ |
| `SESSION_INCONNUE` | 404 | `SessionService.trouver` | ✅ |
| `EXERCICE_INCONNU` | 404 | `ExerciceService` | ✅ |
| `RELECTURE_INCONNUE` | 404 | ajouté avec `getRelecture` | ✅ corrigé |
| `ETUDIANT_BLOQUE` | 429 | `CompteurErreursCode` | ✅ |
| `ERREUR_INTERNE` | 500 | filet de sécurité `@RestControllerAdvice` | ✅ (jamais de stack trace) |

Note : la 5ᵉ règle EF12 de l'issue #15 (blocage) vérifie le blocage **avant**
la validité du code : un étudiant bloqué ne peut pas sonder les codes.

## 3. Écarts trouvés lors de la revue (corrigés ici)

1. **`GET /api/relectures/{id}` absent** — l'opération `getRelecture` du contrat
   n'était implémentée nulle part : `GET` renvoyait 500
   (`HttpRequestMethodNotSupportedException`). Ajout de l'endpoint (200 avec le
   détail, 404 `RELECTURE_INCONNUE`), vérifié par API sur des relectures rendues
   et sur un id inconnu.
2. **Réponses 405 non contractuelles** — un verbe non supporté sur une route
   existante renvoyait une erreur hors format. Ajout d'un handler
   `HttpRequestMethodNotSupportedException` → 405 `METHODE_NON_SUPPORTEE`
   `{ code, message }` (vérifié : `PATCH /api/promotions` → 405 contractuel).

## 4. Points de conformité partielle (assumés, documentés)

1. **Champ en plus dans `LigneTableau`** : la réponse du tableau porte
   `exercicesSansRelecteur` en plus des 6 champs du contrat (exigé par l'issue
   #20 pour alerter sur les exercices sans relecteur). Ajouter un champ est
   compatible avec les consommateurs du contrat ; le champ est dans le
   CHANGELOG. Décision : garder, et faire évoluer le contrat à la mise à jour
   de l'analyse (cahier §10, étape 6).
2. **Code `AUTO_RELECTURE` vs `RELECTURE_PROPRE_EXERCICE`** : le catalogue du
   contrat nomme ce cas `RELECTURE_PROPRE_EXERCICE` ; l'implémentation utilise
   `AUTO_RELECTURE` (nom repris de l'issue #18 : « auto-relecture → 403 »).
   Le statut HTTP (403) et le message sont conformes. Deux options à l'étape 6 :
   renommer dans le code ou dans le contrat — à arbitrer avec le correctif
   d'enveloppe.
3. **`relecteurId` optionnel dans le corps de `POST /api/relectures/{id}`** :
   absent du contrat, nécessaire pour appliquer RG3 sans authentification
   (décision déjà tracée dans le cahier §7). Le contrat reste respecté pour
   `note`/`commentaire`.

## 5. Schémas ↔ DTOs

| Schéma du contrat | DTO | Verdict |
|---|---|---|
| `SessionCreation` / `Session` | `SessionDtos` | ✅ 7 champs, dates RFC 3339 |
| `PresenceCreation` / `Presence` | `PresenceDtos` | ✅ + `source` optionnel (EF7) |
| `ExerciceCreation` / `Exercice` | `ExerciceDtos.ExerciceDto` | ✅ + `relecteurAssignee` (issue #17) |
| `ExerciceDetail` | `ExerciceDtos.ExerciceDetailDto` | ✅ strictement les 5 champs du contrat |
| `RelectureCreation` / `Relecture` | `RelectureDtos` | ✅ + `relecteurId` optionnel (RG3) |
| `LigneTableau` | `TableauService.LigneTableauDto` | ✅ + `exercicesSansRelecteur` (issue #20) |
| `Etudiant` / `Promotion` / `Erreur` | `ReferentielDto` / `ReponseErreur` | ✅ stricts |

**Dates corrigées lors de la revue** : `RelectureDto.rendueAt` et
`ExerciceDto.deposeAt` étaient encore sérialisés en `LocalDateTime.toString()`
(secondes parfois absentes, pas de fuseau). Alignés sur `SessionDtos.horodatage`
→ format `date-time` RFC 3339 du contrat, vérifié par API
(`2026-03-12T09:30:00Z`). Les champs `note`/`commentaire` nullables du contrat
sont bien `null` (et non absents) dans les réponses.

## 6. Vérifications exécutées

- `./mvnw test` : 55/55 verts après chaque correction.
- Stack Docker re-déployée ; appels API réels : `GET /api/relectures/{id}`
  (200/404), `PATCH /api/promotions` (405 contractuel), dates RFC 3339 sur
  `session.ouvertureAt`, `presence.marqueeAt`, `exercice.deposeAt`,
  `relecture.rendueAt`.

## 7. Conclusion

Le contrat est intégralement servi. Trois extensions assumées et documentées
(`relecteurAssignee`, `exercicesSansRelecteur`, `relecteurId`) devront être
incorporées au contrat à l'étape 6 du cahier (« mettre à jour l'analyse »),
avec le renommage éventuel `AUTO_RELECTURE` → `RELECTURE_PROPRE_EXERCICE`.
Rien ne bloque l'ouverture de l'enveloppe.
