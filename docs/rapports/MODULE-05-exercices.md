# Rapport — MODULE 5 : Déposer un exercice, assigner le relecteur, remplacer le lien et voir sa note

Issue : #17 · Réf. EF3, EF4, EF10, EF11, RG3, RG4, RG5, RG9, RG10 · Vérifié le 2026-09-25

## Verdict

**Validé.** Tous les points backend de l'issue sont couverts et vérifiés sur la stack Docker réelle. Côté frontend, l'écran étudiant couvre le dépôt, le remplacement du lien et la consultation ; les relectures assignées (écran relecteur) sont livrées avec le MODULE 6.

## Choix d'interprétation (contrat vs issue)

L'issue mentionne un statut `EN_ATTENTE_SANS_RELECTEUR`, absent du contrat (imposé à la lettre : `EN_ATTENTE` / `RELU`) et du schéma V1 (`CHECK (statut IN ('EN_ATTENTE','RELU'))`). Le cahier des charges §7 tranche : « L'exercice reste EN_ATTENTE et le formateur le voit dans son tableau ». Décision retenue :

- « sans relecteur » = **absence de ligne relecture** (RG8) ;
- la réponse de dépôt porte un flag **`relecteurAssignee`** (extension du DTO) pour que le front affiche « En attente — aucun relecteur disponible » sans falsifier le statut contractuel ;
- le MODULE 8 ajoutera `exercicesSansRelecteur` au tableau (évolution du contrat documentée à ce moment).

## Ce qui a été implémenté

| Élément de l'issue #17 | État | Où |
|---|---|---|
| `POST /api/exercices` → 201 `{ id, statut }` + sessionId, etudiantId, lien, deposeAt | ✅ | `ExerciceController`, `ExerciceService.deposer` |
| Lien invalide → 400 `LIEN_INVALIDE` (http/https exigé) | ✅ | |
| Déjà déposé → 409 `EXERCICE_DEJA_DEPOSE` | ✅ | + contrainte UNIQUE (session, étudiant) |
| Session clôturée → 410 `SESSION_CLOTUREE` (RG9) | ✅ | |
| Assignation aléatoire : pool = présents hors auteur (RG3/RG5) | ✅ | `RelectureService.assignerSiPossible` |
| Non-réassignation d'un relecteur actif de la session | ✅ | filtrage des relectures non rendues |
| Pool vide → exercice EN_ATTENTE sans relecteur (RG8) | ✅ | aucune ligne relecture créée |
| `PATCH /api/exercices/{id}` → 200 (EF10/RG10) | ✅ | 409 `RELECTURE_DEJA_COMMENCEE` si relecture rendue, 410 clôturée, 400 lien |
| `GET /api/exercices/{id}` → 200 sans relecteurId (EF11) | ✅ | note/commentaire null tant que non rendue |
| Écran étudiant : formulaire de dépôt | ✅ | `pages/PanelExerciceEtudiant.tsx` |
| Écran étudiant : bouton « Remplacer le lien » (EF10) | ✅ | visible uniquement en EN_ATTENTE |
| Écran étudiant : statut, puis note et commentaire sans nom de relecteur (EF11) | ✅ | badge statut + bloc note |
| Affichage « aucun relecteur disponible » | ✅ | via `relecteurAssignee=false` (message affiché au MODULE 6 avec l'écran relecteur) |
| Appel API via couche dédiée | ✅ | `endpoints.ts` — `deposerExercice`, `getExercice`, `remplacerLienExercice` |

## Tests exécutés

| Suite | Résultat |
|---|---|
| Backend `RelectureServicePoolTest` (unitaires) : auteur exclu (RG3), relecteur parmi les présents (RG5), non-réassignation d'un relecteur actif, pool vide sans exception (RG8) | 4/4 ✅ |
| Backend `Module5ExercicesTest` (intégration) : 201 avec assignation, pool vide RG8, 409 déjà déposé, 410 clôture, 400 lien, PATCH avant clôture 200, GET sans relecteur ni note, 404, PATCH après clôture 410 | 9/9 ✅ |
| Backend suite complète (modules 1 à 5) | **55/55 ✅** |
| Frontend `module5-exercices.test.tsx` : dépôt + statut, `LIEN_INVALIDE`, remplacement EF10, `RELECTURE_DEJA_COMMENCEE` RG10, note EF11 sans relecteur | 5/5 ✅ |
| Frontend suite complète | **26/26 ✅**, build OK |
| **Stack Docker réelle** : dépôt 201 avec `relecteurAssignee=true`, GET sans relecteur exposé, PATCH 200, 409 doublon, 400 lien invalide | ✅ |

## Détails d'implémentation

- Le filtrage du pool compare les étudiants **par id, sinon par référence** : les tests unitaires utilisent des entités non persistées (id null), et le comparateur reste correct dans les deux mondes.
- Le lien exige `http://` ou `https://` : `URI.create` seul accepte des chaînes trop laxistes.
- Le remplacement du lien est bloqué dès qu'une relecture est **rendue** (`RELECTURE_DEJA_COMMENCEE`) ; tant que le relecteur n'a pas rendu, l'auteur peut remplacer (RG10, conforme à l'issue).
- Le test MODULE 3 « DEJA_PRESENT » a été mis à jour : ce code est désormais traité comme une réussite côté étudiant (il peut poursuivre vers le dépôt), ce qui correspond au flux réel.
