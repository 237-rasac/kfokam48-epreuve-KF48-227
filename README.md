# KFOKAM48 — Présence et relecture d'exercices

Application web fullstack pour la direction de la formation KFOKAM48 : suivi des
sessions de cours par **code de présence** et **relecture d'exercices entre pairs**.

- Le **formateur** ouvre une session, obtient un code valable 15 minutes, ajoute
  des présences manuellement, suit le tableau de bord par promotion et clôture la session.
- L'**étudiant** marque sa présence avec le code, dépose le lien de son exercice,
  remplace son lien tant que personne n'a relu, et voit la moyenne de ses
  relectures (badge « note provisoire » tant qu'un relecteur sur deux n'a pas
  rendu) avec les commentaires — sans jamais connaître le nom des relecteurs.
- Le **relecteur** est tiré au sort parmi les présents (deux par exercice, distincts),
  rend une note entière 0–20 avec commentaire, et peut corriger sa note tant que
  la session n'est pas clôturée.

Périmètre conforme au cahier des charges (`docs/CAHIER_DES_CHARGES.md`) : pas
d'authentification par mot de passe (l'identité est choisie dans une liste), les
promotions et étudiants sont préchargés.

## Démarrage en 3 commandes

Prérequis : Docker (avec Compose v2). Node/npm uniquement pour développer le frontend.

```bash
git clone https://github.com/237-rasac/kfokam48-epreuve-KF48-227.git
cd kfokam48-epreuve-KF48-227/backend
docker compose up -d --build
```

- Backend + API : <http://localhost:8080> (Swagger UI : <http://localhost:8080/swagger-ui.html>)
- PostgreSQL 16 : port hôte **5433** (pour éviter un conflit avec un PostgreSQL local sur 5432)
- Données de démonstration chargées au démarrage : 1 promotion, 5 étudiants, 2 sessions

### Frontend en développement (4ᵉ commande, facultative)

```bash
cd ../frontend && npm install && npm run dev   # http://localhost:5173 (proxy /api -> 8080)
```

Arrêt : `docker compose down` (ajouter `-v` pour effacer les données).

## Architecture

```
api/contrat.yaml          Contrat OpenAPI v2 (5 opérations imposées + extensions EF1-EF12)
backend/                  Spring Boot 4 (Java 25) + PostgreSQL 16 + Flyway
  src/main/.../domaine/     Entités JPA (validation du schéma, ddl-auto=validate)
  src/main/.../repository/  Spring Data JPA
  src/main/.../service/     Règles métier (RG1-RG21)
  src/main/.../api/         Contrôleurs REST + DTOs alignés sur le contrat
  src/main/.../erreur/      Format d'erreur unique { code, message } (ENF4)
  src/main/resources/db/migration/  V1 schéma, V2 données démo, V3-V6 évolutions
frontend/                 React 19 + TypeScript + Vite + Tailwind
  src/api/                  Couche API dédiée (fetch centralisé, erreurs { code, message })
  src/pages/                Écrans : sélection, formateur, étudiant, relecteur
docs/                     Cahier des charges, diagrammes D1-D4, journal
```

### Règles métier principales

| Règle | Où |
|---|---|
| RG1 : code expiré 15 min après l'ouverture | `SessionService.DUREE_CODE` |
| RG2 : plus de présence après clôture | `PresenceService` (410 `SESSION_CLOTUREE`) |
| RG3 : jamais relecteur de son propre exercice | assignation + rendu (403 `AUTO_RELECTURE`) |
| RG4 : deux relecteurs distincts par exercice | `uk_relecture_exercice_relecteur` (V6) + service (409) |
| RG5 : relecteurs au hasard parmi les présents | `RelectureService.assignerSiPossible` |
| RG6 : note entière 0–20 | `RelectureRendueService` (400 `NOTE_INVALIDE`) |
| RG7/EF9 : note modifiable avant clôture, historique | table `relecture_historique` (V4) |
| RG8 : pas de relecteur disponible → exercice EN_ATTENTE | flag `relecteurAssignee`, panel formateur |
| RG14 : 5 erreurs de code → blocage 2 minutes | `CompteurErreursCode` (429) |
| RG15 : une présence par session et étudiant | contrainte UNIQUE + 409 `DEJA_PRESENT` |
| RG21 : moyenne null si aucune note reçue | `TableauService` (front : « — ») |

## API

Contrat complet : `api/contrat.yaml`. Principales opérations :

| Opération | Description |
|---|---|
| `POST /api/sessions` | Ouvrir une session, obtenir le code (201) |
| `POST /api/sessions/{id}/cloture` | Clôturer la session (EF8) |
| `GET /api/sessions/{id}/presences` | Présences d'une session |
| `POST /api/presences` | Marquer sa présence (ou ajout formateur via `source`) |
| `POST /api/exercices` | Déposer son exercice + assignation de deux relecteurs |
| `PATCH /api/exercices/{id}` | Remplacer le lien (EF10) |
| `GET /api/exercices/{id}` | Statut, moyenne, provisoire, commentaires — sans relecteurs (EF11) |
| `POST /api/exercices/{id}/assigner` | Assignation manuelle du relecteur (formateur) |
| `POST /api/relectures/{id}` | Rendre (puis modifier) une relecture |
| `GET /api/etudiants/{id}/relectures` | Relectures en attente d'un étudiant |
| `GET /api/tableau?promotionId=` | Tableau de bord par étudiant |

Toute erreur renvoie `{ "code": "...", "message": "..." }` (jamais de stack trace).
Catalogue des codes : `x-codes-erreur` dans le contrat.

## Tests

```bash
cd backend && ./mvnw test      # 67 tests (H2 en mémoire, sans base locale — ENF6)
cd frontend && npm test        # 28 tests vitest (msw), build : npm run build
```

## Configuration

Variables facultatives (valeurs par défaut du `compose.yaml`) : `POSTGRES_PASSWORD`,
`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `SERVER_PORT`, `VITE_API_BASE_URL`.
`backend/.env.example` sert de modèle.

## Documentation

- `docs/CAHIER_DES_CHARGES.md` — exigences EF/ENF, règles RG, décisions
- `docs/diagrammes/` — D1 cas d'utilisation, D2 modèle de données, D3 séquence présence, D4 états exercice
- `docs/JOURNAL.md` — journal de bord
- `CHANGELOG.md` — historique des versions
