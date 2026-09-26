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

### Organisation du développement (MODULES 1 et 2)

- Ouverture des issues MODULE 1 à 10 (#13 à #22), milestone Sprint 1, gabarit
  Backend / Frontend / Tests / Critères d'acceptation, références EF/RG.
- Workflow adopté pour chaque module : branche `feat/module-N-slug`, commit
  backend, commit frontend, PR « Closes #N », merge sur `main` (branches
  conservées après le MODULE 4), vérification E2E contre la stack Docker.
- **MODULE 1 (#13, PR #23)** : socle technique — entités JPA calées sur V1,
  repositories, gestion d'erreur contractuelle (`ErreurMetierException`,
  `ReponseErreur`, `GestionnaireErreurs`), référentiel promotions/étudiants.
  Côté front : écran de sélection d'identité (pas d'auth au périmètre), écrans
  formateur / étudiant / relecteur, hook `useReferentiel`.
- **MODULE 2 (#14, PR #24)** : `POST /api/sessions` (EF1) avec code unique
  (RG16) et expiration 15 min (RG1), `HorlogeMetier` et `GenerateurCode`
  injectables, écran formateur avec compte à rebours.
- **Bug corrigé (migration V3)** : les inserts à id explicite de V2 ne
  reculaient pas les séquences — le premier POST sur `/api/sessions` violait
  la clé primaire (constaté sur H2, probable aussi sur PostgreSQL). V3 recale
  les six séquences après les données de démo.
- **Bug corrigé (dates)** : les horodatages sérialisés en `LocalDateTime.toString()`
  perdaient secondes et fuseau — le compte à rebours affichait 00:00 dès que le
  conteneur (UTC) et le navigateur (UTC+1) étaient décalés. Passage au format
  date-time du contrat (RFC 3339) + test unitaire RG1 à horloge figée.

### MODULES 3 et 4 (#15, #16 — présences)

- **MODULE 3 (PR #25)** : `POST /api/presences` (EF2) avec tous les codes du
  contrat (`CODE_INCONNU`, `CODE_EXPIRE`, `DEJA_PRESENT`, `SESSION_CLOTUREE`)
  et blocage EF12/RG14 : `CompteurErreursCode` en mémoire (seuil 5, blocage
  2 min, remise à zéro au code valide), 429 `ETUDIANT_BLOQUE`. Écran étudiant
  avec les messages d'erreur distingués. Choix documenté : compteur en mémoire,
  aucune table imposée par le contrat, application mono-poste formateur.
- **MODULE 4 (PR #26)** : `source=FORMATEUR` sur `POST /api/presences`
  (EF7/RG11). Arbitrage : la voie formateur outrepasse l'expiration (RG1) et le
  blocage (EF12) — c'est le sens d'un rattrapage manuel ; clôture (RG2) et
  unicité (RG15) restent valables. Panneau formateur avec badge « ajouté par
  le formateur ».

### MODULES 5 et 6 (#17, #18 — exercices et relectures)

- **MODULE 5 (PR #27)** : dépôt d'exercice (EF3) avec assignation aléatoire du
  relecteur (EF4) — pool = présents hors auteur (RG3/RG5), hors relecteurs
  actifs de la session ; pool vide → exercice EN_ATTENTE sans relecture (RG8).
  Arbitrage : le statut `EN_ATTENTE_SANS_RELECTEUR` demandé par l'issue n'existe
  ni dans le contrat ni dans V1 ; le cahier §7 tranche (« reste EN_ATTENTE ») —
  la réponse porte un flag `relecteurAssignee`. Remplacement du lien (EF10/RG10)
  et consultation EF11 (note + commentaire, jamais le relecteur).
- **MODULE 6 (PR #28)** : `POST /api/relectures/{id}` (EF5) — note entière
  0–20 (RG6), 403 `AUTO_RELECTURE` (RG3, vérifiée aussi sur le `relecteurId` du
  corps puisque pas d'auth), 409 `RELECTURE_DEJA_RENDUE`, l'exercice passe à
  RELU. Écran relecteur : liste des assignations + formulaire note/commentaire.

### MODULES 7 à 10 (#19 à #22 — finalisation du Sprint 1)

- **MODULE 7 (PR #29)** : modification de relecture avant clôture (EF9/RG7).
  Migration V4 `relecture_historique` (anciennes et nouvelles valeurs).
  Arbitrage : après clôture le contrat impose 409 `RELECTURE_VERROUILLEE` (l'issue
  citait `SESSION_CLOTUREE`) — le contrat s'impose à la lettre. Écran relecteur :
  section « déjà rendues » avec formulaire pré-rempli.
- **MODULE 8 (PR #30)** : `GET /api/tableau?promotionId=` (EF6/RG13/RG21) —
  une ligne par étudiant, moyenne null si aucune note (front : « — »), champ
  `exercicesSansRelecteur` (extension du contrat, cohérente avec MODULE 5).
  Tableau formateur réel remplaçant la simple liste.
- **MODULE 9 (PR #31)** : `POST /api/sessions/{id}/cloture` (EF8) — chemin et
  codes du contrat (`SESSION_DEJA_CLOTUREE`). Verrouillage aval déjà en place
  depuis les modules 3/5/7 : présences et dépôts → 410, relectures → 409
  `RELECTURE_VERROUILLEE`. UI : bouton Clôturer avec confirmation, badge « gelée ».
- **MODULE 10 (PR #32)** : `POST /api/exercices/{id}/assigner` — assignation
  manuelle du relecteur avec `assignedBy=FORMATEUR` (migration V5), 400
  auto-assignation, 409 `RELECTURE_DEJA_ASSIGNEE`. `GET
  /api/sessions/{id}/exercices-sans-relecteur` + panel formateur « débloquer
  en un clic ». À partir du MODULE 4, la consigne est de ne plus écrire de
  nouveaux tests : seuls les builds et les suites existantes (55 backend,
  26 frontend) sont maintenus verts.

### Jalon v0.1 et passe E2E complète

- Rebuild complet de la stack Docker (`docker compose up -d --build`),
  migrations V1→V5 appliquées sur PostgreSQL 16 réel.
- Scénario complet joué par API (formateur / étudiant / relecteur) : référentiel,
  ouverture de session, présences manuelles, présence par code, doublon 409,
  dépôt avec assignation, EF11, remplacement du lien, rendu de relecture,
  modification EF9, EF11 relu, tableau (moyenne 17,0 pour l'étudiante test),
  clôture et verrouillage complet, blocage EF12 → 429.
- **Bug trouvé et corrigé** : une note décimale (`15.5`) était rejetée par la
  désérialisation Jackson avec le code `CHAMP_MANQUANT` au lieu de
  `NOTE_INVALIDE` (RG6). Correctif : champ désérialisé en `BigDecimal` et
  validation métier de l'intégralité (15.5 → 400 `NOTE_INVALIDE`, 15.0 accepté).
  E2E re-joué : tout est vert.
- Commit `verification` puis commit vide `[JALON] v0.1` posé sur `main`.
- Rédaction des livrables manquants : `README.md` (démarrage en 3 commandes,
  architecture, règles RG, API, tests) et `CHANGELOG.md` (Keep a Changelog,
  version 0.1.0) — PR #33.
- État du Sprint 1 : 10/10 issues MODULE fermées, EF1–EF12 couvertes, branches
  de modules conservées, PR #23 à #33 fusionnées.

## Changement de besoin : double relecture (enveloppe étape 3)

Reçu après le jalon `[JALON] v0.1` : chaque exercice est relu par **deux pairs
différents**, note retenue = moyenne des deux ; si un seul a rendu, note
affichée mais **provisoire**. C'est un **Must qui arrive tard** : quelque chose
doit sortir du périmètre. Ce qui est écrit ici est assumé comme tel.

### Découpage et re-priorisation (milestone Sprint 2)

- Issue **#39** (bug) : présences concurrentes perdues — `PresenceService`
  check-then-act sur RG15. **Priorité 1**, avant toute feature : l'ordre
  issue → test rouge → correctif est évalué, et un bug de données réelles
  passe avant une évolution.
- Issue **#40** (MODULE 11) : migration **V6** versionnée — drop de
  `uk_relecture_exercice`, unique `(exercice_id, relecteur_id)` ; V1–V5
  intacts, les données de démo doivent survivre.
- Issue **#41** (MODULE 12) : backend — deux relecteurs distincts (RG3/RG5),
  RELU quand tout est rendu (EF5), moyenne + flag `provisoire` (RG17) selon
  le contrat v2.0.0.
- Issue **#42** (MODULE 13) : frontend — badge « note provisoire »,
  commentaires multiples (EF11).

### Ce que je sacrifie, et pourquoi

- **ENF1 — « temps de réponse < 2 s pour le tableau avec 100 étudiants »** :
  sort du périmètre du Sprint 2. C'est la seule exigence vérifiable qui
  demande un outillage que le projet n'a pas (jeu de charge, mesure);
  la faire mal aurait coûté plus qu'elle ne rapporte, et aucune exigence
  Must du changement client ne la porte. La pagination éventuelle du tableau
  ne sera pas traitée non plus. ENF2 (responsive 375 px) est conservée :
  elle ne coûte presque rien. L'exigence reste écrite au cahier des charges,
  marquée « sacrifiée » — un périmètre réduit et assumé vaut mieux qu'un
  périmètre annoncé et non tenu.
- **EF12/RG14 (blocage après 5 erreurs)** : livré au Sprint 1 (Could),
  maintenance seule — aucune extension (pas de persistance du compteur,
  pas de paramétrage du seuil) tant que le Sprint 2 n'est pas livré.
- **EF9/RG7 (historique de relecture)** : livré au Sprint 1, non étendu au
  nouveau mode deux relecteurs (l'historique existant reste par relecture,
  pas de vue agrégée par exercice).

### Séparation des sujets

Correctif bug et changement de besoin sont deux sujets : deux branches,
deux PR. L'analyse (cahier v2, RG4/RG17, contrat v2.0.0, diagrammes D2/D4)
est déjà commiteé sur `docs/double-relecture` (**PR #38**) — le commit
documentaire précède tout code, conformément à l'enveloppe (point 2.1).

## 2026-09-26 — Réalisation du Sprint 2

Les quatre issues du milestone Sprint 2 sont fermées, chacune par sa propre PR
(#43 à #46), un sujet = une branche = une PR, comme annoncé.

### Bug #39 — présences concurrentes (PR #43)

- **Test d'abord** : `Bug39PresencesConcurrentesTest` commité avant tout
  correctif (commit `28683e1` sur `fix/presences-concurrentes`). Le test ne
  course pas au timing : une passerelle de test (proxy `@Primary` sur le
  repository) gèle le premier `save()` AVANT son INSERT ; la seconde requête
  traverse alors le contrôle RG15 pendant que la première transaction est en
  vol — entrelacement déterministe, reproductible à chaque exécution.
  Rouge constaté : le perdant sort en `[201, 500]` au lieu de `[201, 409]`.
- **Correctif** (commit `48e82af`) : interception ciblée de
  `DataIntegrityViolationException` autour du `save()` dans
  `PresenceService.marquer()` → 409 `DEJA_PRESENT`. La contrainte
  `uk_presence_session_etudiant` (V1) sert d'arbitre atomique de la course.
- **Arbitrage** : le verrou pessimiste sur la session (l'issue proposait
  « et/ou ») a été écarté — avec une transaction lente ou gelée, il sérialise
  les requêtes et peut mener à un deadlock ; l'interception seule suffit et
  le contrôle applicatif RG15 reste en place pour le cas nominal.
- Fusion PR #43 (`1dfaf78`), issue clôturée automatiquement (`Closes #39`).

### MODULE 11 — migration V6 (issue #40, PR #44)

- `V6__deux_relectures_par_exercice.sql` : drop de `uk_relecture_exercice`,
  unique `(exercice_id, relecteur_id)`. V1–V5 intacts (prouvable au git log
  de la PR : la branche n'ajoute que deux fichiers).
- Données de démo V2 testées à la migration sur H2 (tests dédiés : survie de
  la relecture existante, second relecteur distinct accepté, même relecteur
  rejeté, table `relecture_historique` de V4 fonctionnelle) ET sur
  PostgreSQL 16 réel via `docker compose up` : V1→V6 appliquées, les trois
  relectures existantes (dont une non rendue) préservées, contrainte
  vérifiée active sur la base réelle. Ligne de test insérée lors de la
  vérification nettoyée ensuite.
- Fusion PR #44 (`ace4b4c`), issue clôturée.

### MODULE 12 — backend contrat v2 (issue #41, PR #45)

- Deux relecteurs distincts à l'inscription (RG4 modifiée) ; pool
  insuffisant → une seule relecture, voire aucune (RG8).
- **Bug d'assignation trouvé et corrigé** : la répartition issue #17 (ne pas
  réassigner un relecteur actif) excluait aussi les relectures de l'exercice
  en cours — le second relecteur était donc systématiquement écarté du pool
  (la première relecture venait d'être créée quelques lignes plus tôt).
  Le filtre ne considère plus que les relectures des AUTRES exercices de la
  session.
- Statut RELU seulement quand toutes les relectures attendues sont rendues
  (EF5) ; `ExerciceDetail` v2 (moyenne double, `provisoire`, compteurs,
  `commentaires[]`, jamais l'identité des relecteurs) ; `TableauService`
  recalculé sur les moyennes d'exercices avec `moyenneProvisoire` (RG13) ;
  `assigner` complété vers le second relecteur, 409 au-delà de deux ou en
  doublon.
- Convention §7 appliquée : un exercice à relecteur unique dont la note est
  rendue est définitif — couvert par un test dédié.
- 67 tests backend verts (5 nouveaux cas d'acceptation dans
  `Module12DeuxRelecteursTest`, ajustements des tests MODULE 5 et du pool
  pour les deux relecteurs). Scénario E2E aligné sur les formes v2
  (vérifications 16 et 21). Fusion PR #45 (`5700212`), issue clôturée.

### MODULE 13 — frontend contrat v2 (issue #42, PR #46)

- `types.ts` : `ExerciceDetail` v2 et `LigneTableau.moyenneProvisoire` ;
  endpoints inchangés (mêmes routes, seules les formes de réponse évoluent).
- Panel étudiant : badge « Note provisoire (n/m relectures rendues) » tant
  qu'un seul relecteur a rendu, moyenne affichée, les commentaires multiples
  listés — jamais l'identité des relecteurs (assertion anti-fuite sur tout
  le bloc exercice). Tableau formateur : astérisque + tooltip sur la moyenne
  provisoire (le calcul reste côté API, RG21 conservé — périmètre assumé,
  le tableau n'expose pas le détail des relectures). Écran relecteur
  inchangé fonctionnellement.
- Handlers MSW complétés pour les nouvelles formes (fixtures `LigneTableau`
  à jour dans ecrans/module2/module4). `npm run build` et `tsc -b` verts,
  28 tests frontend (2 nouveaux). Fusion PR #46 (`a454c0d`), issue clôturée.

### État du Sprint 2

- 4/4 issues fermées (#39 à #42), PR #43 à #46 fusionnées sur `main`,
  branches supprimées après fusion.
- Tests : 67 backend / 28 frontend verts ; migrations V1→V6 validées sur H2
  et PostgreSQL 16 réel.
- Périmètre sacrifié (ENF1) et extensions non traitées (EF12, EF9) : voir
  « Ce que je sacrifie, et pourquoi » ci-dessus — inchangé.
- Le contrat v2.0.0 est implémenté de bout en bout : dépôt avec deux
  relecteurs, rendus, moyenne provisoire puis définitive, tableau et panel
  étudiant alignés.
