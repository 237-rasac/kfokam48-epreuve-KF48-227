# Changelog

Tous les changements notables de ce projet sont documentés dans ce fichier.

Le format s'inspire de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/)
et le versionnage suit [SemVer](https://semver.org/lang/fr/).

## [Unreleased]

## [0.1.0] — 2026-09-25

Jalon `v0.1` : les dix modules du Sprint 1 sont livrés (issues #13 à #22),
couvrant EF1 à EF12, avec une passe de vérification E2E complète sur la stack
Docker réelle (parcours formateur / étudiant / relecteur, clôture et blocage).

### Ajouté

#### Backend

- **MODULE 1 (#13)** — Socle : entités JPA conformes au schéma Flyway V1
  (`ddl-auto=validate`), repositories Spring Data, format d'erreur contractuel
  `{ code, message }` via `@RestControllerAdvice` (ENF4, jamais de stack trace),
  référentiel `GET /api/promotions` et `GET /api/etudiants`, données de
  démonstration (1 promotion, 5 étudiants).
- **MODULE 2 (#14)** — `POST /api/sessions` : ouverture avec code de présence
  unique 6 caractères (RG16) et expiration à 15 minutes (RG1),
  `GET /api/sessions/{id}`, horloge et générateur de code injectables.
- **MODULE 3 (#15)** — `POST /api/presences` : présence par code (EF2),
  400 `CODE_INCONNU`, 410 `CODE_EXPIRE` (RG1), 409 `DEJA_PRESENT` (RG15),
  410 `SESSION_CLOTUREE` (RG2), blocage 429 `ETUDIANT_BLOQUE` après 5 erreurs
  pendant 2 minutes (EF12/RG14, remise à zéro au code valide),
  `GET /api/sessions/{id}/presences`.
- **MODULE 4 (#16)** — Ajout manuel d'une présence par le formateur
  (`source=FORMATEUR`, EF7/RG11) : outrepasse l'expiration et le blocage,
  conserve clôture et unicité.
- **MODULE 5 (#17)** — `POST /api/exercices` : dépôt du lien (EF3) avec
  assignation aléatoire du relecteur parmi les présents, hors auteur (RG3/RG5)
  et hors relecteurs actifs ; pool vide → exercice EN_ATTENTE sans relecteur
  (RG8, flag `relecteurAssignee`) ; `PATCH /api/exercices/{id}` pour remplacer
  le lien (EF10/RG10) ; `GET /api/exercices/{id}` sans identité du relecteur
  (EF11) ; 409 `EXERCICE_DEJA_DEPOSE`, 410 `SESSION_CLOTUREE`, 400 `LIEN_INVALIDE`.
- **MODULE 6 (#18)** — `POST /api/relectures/{id}` : rendre une note entière
  0–20 (RG6, 400 `NOTE_INVALIDE`) et un commentaire ; 403 `AUTO_RELECTURE`
  (RG3), 403 `RELECTURE_NON_ASSIGNEE`, 409 `RELECTURE_DEJA_RENDUE`, 410
  `SESSION_CLOTUREE` ; l'exercice passe à RELU ;
  `GET /api/etudiants/{id}/relectures`.
- **MODULE 7 (#19)** — Modification de la relecture avant clôture (EF9/RG7) :
  second POST accepté, anciennes valeurs conservées dans
  `relecture_historique` (migration V4), après clôture → 409
  `RELECTURE_VERROUILLEE` (catalogue du contrat) ;
  `GET /api/etudiants/{id}/relectures/rendues`.
- **MODULE 8 (#20)** — `GET /api/tableau?promotionId=` : une ligne par étudiant
  (présences, exercices déposés, moyenne null si aucune note reçue — RG21,
  relectures en attente, exercices sans relecteur), 404 `PROMOTION_INCONNUE`.
- **MODULE 9 (#21)** — `POST /api/sessions/{id}/cloture` (EF8) : clôture avec
  `clotureAt`, 409 `SESSION_DEJA_CLOTUREE`, verrouillage complet aval.
- **MODULE 10 (#22)** — `POST /api/exercices/{id}/assigner` : assignation
  manuelle du relecteur par le formateur avec `assignedBy=FORMATEUR`
  (migration V5), 400 auto-assignation, 409 `RELECTURE_DEJA_ASSIGNEE`,
  `GET /api/sessions/{id}/exercices-sans-relecteur`.

#### Frontend

- Couche API dédiée (`src/api/`) : fetch centralisé avec timeout, classe
  `ApiError` alignée sur le contrat d'erreur.
- Écran de sélection d'identité (pas d'authentification au périmètre),
  mémorisée en localStorage.
- Écran formateur : ouverture de session avec code et compte à rebours (RG1),
  panneau des présences avec badge « ajouté par le formateur », ajout manuel,
  tableau de bord par étudiant (moyenne « — » si null, badge rouge exercices
  sans relecteur), clôture avec confirmation et badge « session gelée »,
  panel d'assignation manuelle des relecteurs.
- Écran étudiant : saisie du code de présence avec tous les messages d'erreur,
  dépôt du lien, remplacement du lien, consultation de la note et du commentaire
  sans nom de relecteur.
- Écran relecteur : relectures en attente, formulaire note 0–20 + commentaire,
  section « déjà rendues » avec modification pré-remplie (EF9).

#### Infrastructure

- `backend/compose.yaml` : PostgreSQL 16 + backend Spring Boot, healthcheck,
  port hôte 5433 ; migrations Flyway V1 (schéma) à V5 (`assigned_by`,
  `relecture_historique`).
- Tests : 55 tests backend (H2 en mémoire, ENF6 — sans base locale) et 26 tests
  frontend (vitest + msw), tous verts au jalon.

### Corrigé

- **Séquences d'identité** (V3) : les inserts à id explicite des données de
  démonstration ne reculaient pas les séquences — le premier POST créait une
  collision de clé primaire.
- **Format des dates** : horodatages sérialisés en RFC 3339 avec décalage
  horaire explicite — le compte à rebours du code de présence était faux dès
  que le navigateur et le conteneur n'étaient pas dans le même fuseau.
- **Note décimale** (passe E2E v0.1) : `15.5` était rejeté par la
  désérialisation avec le code `CHAMP_MANQUANT` au lieu de `NOTE_INVALIDE`
  (RG6) ; le champ est désérialisé en `BigDecimal` et la validation métier
  vérifie l'intégralité.

[Unreleased]: https://github.com/237-rasac/kfokam48-epreuve-KF48-227/compare/v0.1...HEAD
[0.1.0]: https://github.com/237-rasac/kfokam48-epreuve-KF48-227/releases/tag/v0.1
