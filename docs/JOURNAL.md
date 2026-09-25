# Journal de projet

## 2026-09-25

### Initialisation de la documentation

- Création du cahier des charges dans `docs/CAHIER_DES_CHARGES.md`.
- Création des diagrammes d'architecture et de séquence dans `docs/DIAGRAMMES.md`.
- Formalisation du besoin de préciser le domaine métier, les rôles et les critères de recette.

### Configuration du backend

- Vérification de la configuration initiale du backend Spring Boot.
- Mise à jour de `backend/.gitignore` pour exclure `.env` et les variantes `.env.*`.
- Création de `backend/.env` pour la configuration locale.
- Création de `backend/.env.example` comme modèle de configuration versionnable.

### Vérifications

- Les fichiers d'environnement créés sont présents dans `backend`.
- Le test Maven `backend\mvnw.cmd test` a été exécuté.
- Le test échoue actuellement au chargement du contexte Spring : aucune source de données ni driver JDBC n'est configuré.
- Le fichier `backend/compose.yaml` contient encore `services: {}` et ne fournit donc pas de base de données.

### Issues GitHub

- Création des issues 2 à 12 sur le dépôt `237-rasac/kfokam48-epreuve-KF48-227`, à partir du modèle de l'issue #1.
- 12 issues au total, couvrant EF1 à EF12, chaque issue liée à ses règles RG et à son contrat API.
- Labels MoSCoW appliqués : Must (issues 1-8), should (issues 9-11), could (issue 12).
- Toutes les issues assignées à `237-rasac`.

### Base de données PostgreSQL et migrations Flyway

- Ajout du driver PostgreSQL, du plugin Flyway (avec `flyway-database-postgresql`) et de H2 (scope test) dans `backend/pom.xml`.
- Service PostgreSQL 16 dans `backend/compose.yaml`, port hôte 5433 pour éviter le conflit avec un PostgreSQL local sur 5432.
- `application.properties` : datasource PostgreSQL, `ddl-auto=validate` (le schéma appartient à Flyway), Flyway activé.
- Migration `V1__schema_initial.sql` : 6 tables conformes au diagramme D2 (promotion, etudiant, session, presence, exercice, relecture), contraintes RG3/RG4/RG15, unicité code de session (RG16).
- Migration `V2__donnees_demo.sql` : données de démonstration (1 promotion, 5 étudiants, 2 sessions, présences, exercices, relecture).
- Tests sur H2 en mémoire (`src/test/resources/application.properties`) : `mvnw test` passe sans base locale (ENF6).
- Migrations validées sur PostgreSQL réel : `mvnw flyway:migrate` applique V1 et V2, vérification via `psql` (7 relations, historique Flyway OK, 5 étudiants).
- `BackendApplicationTests.contextLoads` : BUILD SUCCESS.

### Lancement fullstack en une commande (docker compose up)

- Création de `backend/Dockerfile` multi-stage : build Maven (cache BuildKit sur `/root/.m2`) puis runtime JRE 25 alpine avec utilisateur non-root.
- Service `backend` ajouté à `backend/compose.yaml` : `depends_on: db: service_healthy`, URL datasource `jdbc:postgresql://db:5432/kfokam48` (nom du service dans le réseau Docker, port interne 5432).
- `.dockerignore` : exclusion de `target/`, `.idea/`, `.env`.
- Correction importante : `flyway-database-postgresql` était présent uniquement pour le plugin Maven, pas pour l'application → « Unsupported Database: PostgreSQL 16.15 » au démarrage du conteneur. La dépendance a été ajoutée au classpath principal du pom.
- Validation : `docker compose up -d --wait` → db et backend healthy, Flyway applique V1-V2, Tomcat démarre, `GET /v3/api-docs` renvoie 200, données démo présentes (5 étudiants, 2 sessions).
- `mvnw test` reste vert après l'ajout de la dépendance Flyway.
- Le périmètre choisi est backend + DB ; le frontend continuera de tourner en local (`npm run dev`).

### Frontend : couche API dédiée et coquille de l'application

- Couche API dédiée dans `frontend/src/api/` : `client.ts` (fetch centralisé, timeout 10 s, classe `ApiError` conforme au contrat d'erreur { code, message }, ENF4), `types.ts` (types du domaine alignés sur `api/contrat.yaml`), `endpoints.ts` (toutes les opérations du contrat).
- Coquille `App.tsx` : aucun écran métier, affiche l'état de connexion à l'API (chargement, promotions reçues, erreur normalisée avec bouton réessayer).
- Proxy de dev Vite : `/api` → `http://localhost:8080` (pas de CORS en dev). `.env.example` mis à jour (`VITE_API_BASE_URL` optionnel).
- `index.html` : titre et langue fr.
- Tests : `client.test.ts` (succès, erreur 410 contractuelle, réseau injoignable, POST JSON via msw) et `app.test.tsx` (promotion affichée, erreur normalisée). 9 tests passent.
- `npm run build` : OK (99 modules, ~314 Ko JS).
- Validation E2E : Vite sur 5173 répond 200, le proxy transmet `/api/promotions` au backend qui répond 404 (normal : aucun contrôleur implémenté).

### Vérifications backend

- `mvnw test` : BUILD SUCCESS.
- Stack Docker : `kfokam48-backend` et `kfokam48-db` up, base peuplée (1 promotion, 5 étudiants, 2 sessions, 4 présences, 2 exercices, 1 relecture).

### Décisions et prochaines actions

- Définir le domaine métier et les user stories.
- Confirmer le SGBD et ajouter le driver JDBC correspondant.
- Documenter les migrations Flyway.
- Définir les contrats API avant l'implémentation des ressources métier.
- Compléter les scénarios de recette et les tests associés.
