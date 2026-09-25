# Soumission — Épreuve finale fullstack KFOKAM48

> Remplis ce fichier, **vérifie tes deux liens depuis une fenêtre de navigation privée**,
> puis téléverse-le sur la plateforme **avant 18h00**.
> Sans ce dépôt sur la plateforme, tu n'as rien rendu.

---

## Candidat

| | |
|---|---|
| Nom et prénom(s) | NJANKOUO KAMDJOU Abdoul Rasac |
| Matricule | KF48-YA0-227 |
| Centre | Yaoundé |
| Compte GitHub | [237-rasac](https://github.com/237-rasac) |

## Projet

| | |
|---|---|
| Dépôt (public) | [`https://github.com/237-rasac/kfokam48-epreuve-KF48-227`](https://github.com/237-rasac/kfokam48-epreuve-KF48-227) |
| Commit final — hash complet, 40 caractères | À compléter après `[JALON] v1.0` (dernier commit poussé sur `main`) |
| Branche | `main` |

## Épreuve Git — étape 5

| | |
|---|---|
| Dépôt (public) | `https://github.com/237-rasac/kfokam48-gitlab-KF48-227` |
| Commit final — hash complet, 40 caractères | À compléter après la synchronisation de l'étape 5 |

## Technique

| | |
|---|---|
| Frontend utilisé | React 19 + TypeScript (Vite), choix justifié au cahier des charges §0 |
| Base de données | PostgreSQL 16, schéma versionné par Flyway V1–V5 (H2 en mémoire pour les tests) |
| Commandes de démarrage | 1. `cd backend && docker compose up -d --build` (base et API sur http://localhost:8080, Swagger : /swagger-ui.html)<br>2. `cd frontend && npm install`<br>3. `npm run dev` (application sur http://localhost:5173) |

## Ce que j'ai livré

Les dix modules du Sprint 1 sont fusionnés sur `main` (issues #13 à #22, PR #23 à #35),
couvrant toutes les exigences EF1 à EF12 :

- ouverture de session avec code unique valable 15 minutes (EF1, RG1, RG16) ;
- présence par code avec tous les cas d'erreur contractuels et blocage de 2 minutes
  après 5 erreurs (EF2, EF12, RG2, RG14, RG15) ;
- ajout manuel d'une présence par le formateur, badge « ajouté par le formateur » (EF7, RG11) ;
- dépôt d'exercice avec assignation aléatoire du relecteur parmi les présents, remplacement
  du lien tant que personne n'a relu, consultation de la note sans identité du relecteur
  (EF3, EF4, EF10, EF11, RG3 à RG5, RG8 à RG10) ;
- rendu d'une relecture notée de 0 à 20 avec commentaire (EF5, RG6) ;
- modification de sa relecture avant la clôture, avec historique (EF9, RG7) ;
- tableau de bord par promotion : présences, exercices, moyenne (« — » si aucune note),
  relectures en attente, exercices sans relecteur (EF6, RG13, RG21) ;
- clôture de session avec verrouillage complet : plus aucune présence, dépôt ni relecture
  acceptés (EF8, RG2, RG9) ;
- assignation manuelle d'un relecteur par le formateur sur un exercice resté sans relecteur
  (issue #22, `assignedBy = FORMATEUR`).

J'ai volontairement exclu l'authentification (cahier des charges §3). L'identité est
choisie dans une liste et mémorisée localement.

## Qualité

| | |
|---|---|
| Tests backend | 55 tests (H2 en mémoire, `mvnw test` sans base locale — ENF6) |
| Tests frontend | 26 tests vitest + msw, build de production OK |
| E2E | Parcours complet formateur / étudiant / relecteur vérifié sur la stack Docker (jalon v0.1) |
| Revue contrat | Contrat OpenAPI ↔ implémentation vérifié opération par opération (`docs/revue-contrat.md`) |

---

## Reste à faire avant de téléverser (étapes 5 à 9 du cahier §10)

1. **Ouvrir l'enveloppe** (correctif de bug + changement à intégrer) — étape 5.
2. Corriger le bug, intégrer le changement, mettre à jour l'analyse et le contrat — étapes 5-6.
3. Jouer la passe E2E complète après le correctif.
4. Commit final, **`[JALON] v1.0`** — étape 8.
5. Compléter les deux **hash de commit** (40 caractères) ci-dessus.
6. Synchroniser l'**épreuve Git** (étape 5) et compléter son hash.
7. Cocher toutes les cases de la liste ci-dessous, puis téléverser.

---

## Avant de téléverser, vérifie

- [ ] Mes deux dépôts sont **publics** et s'ouvrent en navigation privée
- [ ] Les deux hash font bien **40 caractères** et existent sur GitHub
- [ ] Tout mon travail est **poussé** — `git status` est propre sur les deux dépôts
- [ ] Mon `README` a été testé depuis un clone vierge, dans un dossier vide
- [x] Mon `JOURNAL.md` et mon cahier des charges sont dans `docs/`
- [ ] Les trois commits `[JALON]` sont poussés et dans le bon ordre
      (`[JALON] analyse` ✅ · `[JALON] v0.1` ✅ · `[JALON] v1.0` ⬜ à poser après le correctif)

---

**Déclaration.** J'ai réalisé ce travail seul. Les outils d'IA étaient autorisés sans restriction et je les ai utilisés ; mon journal indique où et comment j'ai vérifié leurs réponses. Mes dépôts resteront publics et inchangés jusqu'à la publication des résultats.

Signature : ______________________  Date : __________
