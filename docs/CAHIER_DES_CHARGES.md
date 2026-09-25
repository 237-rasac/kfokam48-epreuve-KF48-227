# Cahier des charges — Présence et relecture KFOKAM48

Auteur : KF48-YA0-227 · Version 2 · Frontend choisi : React, parce que je maîtrise mieux son écosystème et que le build est simple à documenter.

## 1. Contexte et objectif

La direction de la formation KFOKAM48 veut outiller le suivi des sessions de cours. Aujourd’hui, la présence et la relecture des exercices se font de manière informelle. L’objectif est de fournir une application web permettant :
- au formateur d’ouvrir une session, d’obtenir un code de présence et de suivre l’assiduité et les résultats ;
- à l’étudiant de marquer sa présence, de déposer son exercice et de relire l’exercice d’un pair ;
- au formateur de visualiser un tableau de bord par promotion.

## 2. Acteurs et rôles

| Acteur | Ce qu’il peut faire |
|---|---|
| Formateur | Ouvrir une session, voir le tableau de bord, ajouter une présence manuellement, clôturer une session |
| Étudiant | Marquer sa présence avec un code, déposer un exercice, remplacer le lien de son exercice, être assigné à une relecture, rendre une relecture, voir sa note et son commentaire |
| Système | Générer le code de présence, assigner aléatoirement deux relecteurs, calculer la moyenne des relectures rendues, bloquer un étudiant après 5 erreurs de code |

## 3. Périmètre

**Inclus :**
- Gestion des sessions de cours et des codes de présence.
- Marquage de présence par code.
- Dépôt d’exercice par lien.
- Assignation automatique de deux relecteurs par exercice.
- Relecture avec note entière de 0 à 20 et commentaire.
- Tableau de bord formateur par promotion.
- Ajout manuel de présence par le formateur.
- Clôture de session.
- Blocage temporaire après 5 erreurs de code.

**Exclu :**
- Authentification par mot de passe (Q1).
- Gestion des promotions et des étudiants (les données sont préchargées).
- Gestion des formateurs (pas de compte formateur).
- Envoi d’e-mails ou de notifications.
- Export des données.
- Paiement.
- Application mobile native.

## 4. Exigences fonctionnelles

| Réf | Exigence | Critère d’acceptation | Priorité |
|---|---|---|---|
| EF1 | Le formateur ouvre une session et obtient un code | Quand je crée une session avec un titre et une promotion, alors je reçois un code, une date d’ouverture et une date d’expiration | Must |
| EF2 | L’étudiant marque sa présence avec un code | Quand je saisis un code valide et non expiré, ma présence apparaît dans le tableau du formateur | Must |
| EF3 | L’étudiant dépose le lien de son exercice | Quand je dépose un lien valide pour une session, alors un exercice est créé avec le statut EN_ATTENTE | Must |
| EF4 | Deux étudiants sont assignés à la relecture d’un exercice | Quand un exercice est déposé, alors le système assigne deux relecteurs différents parmi les étudiants présents, chacun distinct de l’auteur et de l’autre (dans la limite du pool disponible) | Must |
| EF5 | Le relecteur rend une note et un commentaire | Quand je soumets une note entière entre 0 et 20 et un commentaire, alors la relecture est enregistrée ; quand les deux relectures attendues sont rendues, l’exercice passe au statut RELU | Must |
| EF6 | Le formateur consulte le tableau de bord | Quand j’ouvre le tableau pour une promotion, alors je vois par étudiant : présences, exercices déposés, moyenne, relectures en attente | Must |
| EF7 | Le formateur ajoute une présence manuellement | Quand j’ajoute une présence, alors elle est marquée source=FORMATEUR | Must |
| EF8 | Le formateur clôture une session | Quand je clôture une session, alors plus aucune présence ni dépôt n’est possible | Must |
| EF9 | Le relecteur peut corriger sa note tant que la session n’est pas clôturée | Quand je modifie ma note avant la clôture, alors la nouvelle note remplace l’ancienne | Should |
| EF10 | L’étudiant peut remplacer le lien de son exercice | Quand je remplace le lien avant qu’une relecture ait commencé, alors le lien est mis à jour | Should |
| EF11 | L’étudiant relu voit sa note et son commentaire, sans le nom du relecteur | Quand je consulte mon exercice relu, alors je vois la moyenne des relectures rendues et les commentaires, mais pas l’identité des relecteurs ; tant que les deux relectures ne sont pas rendues, la note est marquée provisoire | Should |
| EF12 | Le système bloque un étudiant après 5 erreurs de code | Quand je saisis 5 codes erronés, alors je suis bloqué 2 minutes | Could |

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| ENF1 | Temps de réponse inférieur à 2 secondes pour le tableau | Test manuel avec 100 étudiants |
| ENF2 | Usage mobile responsive | Test sur largeur 375 px |
| ENF3 | Volumétrie : 500 étudiants, 50 sessions, 5000 présences | Jeu de données de démonstration |
| ENF4 | Les erreurs renvoient un JSON avec code et message, jamais de stack trace | Test avec un code invalide |
| ENF5 | Le schéma est versionné par Flyway | Vérification des migrations |
| ENF6 | Les tests tournent sans base locale | `mvn test` sur un poste vierge |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| RG1 | Le code de présence expire 15 minutes après l’ouverture de la session | Q2 |
| RG2 | Un étudiant ne peut pas marquer sa présence après la fin de la session | Q3 |
| RG3 | Un étudiant ne peut pas relire son propre exercice | Q5 |
| RG4 | Chaque exercice est relu par deux relecteurs différents, distincts entre eux et de l’auteur (remplace « un seul relecteur par exercice », Q6, depuis l’enveloppe étape 3) | Q6 modifiée |
| RG5 | Le relecteur est choisi au hasard parmi les étudiants présents à la session | Q7 |
| RG6 | La note est un entier de 0 à 20 | Q9 |
| RG7 | Un relecteur peut corriger sa note tant que le formateur n’a pas clôturé la session | Q10 |
| RG8 | Si un relecteur ne rend jamais sa relecture, l’exercice reste EN_ATTENTE ; si l’autre a rendu, sa note est affichée mais marquée provisoire | Q11 modifiée |
| RG9 | Un étudiant peut déposer son exercice jusqu’à la clôture de la session | Q12 |
| RG10 | Un étudiant peut remplacer le lien de son exercice tant que personne n’a commencé à le relire | Q13 |
| RG11 | Le formateur peut ajouter une présence manuellement, marquée source=FORMATEUR | Q14 |
| RG12 | La note est définitive une fois envoyée | Q15 |
| RG13 | Le tableau affiche par étudiant : présences, exercices déposés, moyenne, relectures en attente | Q16 |
| RG14 | Après 5 erreurs de code, l’étudiant est bloqué 2 minutes | Q4 |
| RG15 | Une seule présence par session et par étudiant | Contrat |
| RG16 | Le code de présence est unique par session | Implicite |
| RG17 | La note retenue est la moyenne des relectures rendues ; elle est provisoire tant que les deux relectures attendues ne sont pas rendues | Changement client (enveloppe étape 3) |

## 7. Zones d’ombre, hypothèses et contradictions

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Pourquoi |
|---|---|---|---|
| Contradiction Q10 / Q15 | Q10 : correction possible jusqu’à clôture. Q15 : note définitive une fois envoyée | Q10 l’emporte | Q10 décrit un usage réel et précis. Q15 est une intention trop rigide. La clôture de session est un point de bascule clair. |
| Aucun relecteur disponible | Q7 dit « au hasard parmi les présents », mais ne dit pas quoi faire s’il n’y a personne | L’exercice reste EN_ATTENTE et le formateur le voit dans son tableau | On ne peut pas assigner un relecteur qui n’existe pas. Le formateur peut clôturer ou ajouter un relecteur manuellement plus tard. |
| Moyenne sans note | Q16 dit « moyenne des notes reçues » | Moyenne des relectures rendues ; null si aucune note reçue | Une moyenne de 0 serait fausse. Le front affiche « — ». |
| Identification du relecteur | Le contrat ne prévoit pas de relecteurId dans POST /api/relectures/{id} | L’{id} est l’identifiant de la relecture. Le relecteur est celui qui a été assigné. | On respecte le contrat à la lettre. Le frontend passe l’id de la relecture. |
| Promotion inconnue | Le contrat impose 404 pour GET /api/tableau | On renvoie 404 PROMOTION_INCONNUE | Cohérence avec le contrat. |
| Double relecture (enveloppe étape 3) | « Chaque exercice est relu par deux pairs différents, la note retenue est la moyenne des deux ; si un seul a rendu, note affichée mais provisoire » | Deux relectures par exercice, moyenne des rendues, flag `provisoire` dans l’API. Si le pool ne permet pas deux relecteurs, l’exercice n’a qu’une relecture et sa note rendue est définitive | Le flag provisoire n’a de sens que si une deuxième relecture est attendue. Version 2 du contrat (changement non rétrocompatible de sémantique). |

## 8. Contraintes techniques

- Backend : Java 17+, Spring Boot, Maven, wrapper `mvnw` commité.
- Contrat `api/contrat.yaml` respecté à la lettre : chemins, verbes, codes, format d’erreur.
- Séparation contrôleur / service / repository. DTO obligatoires.
- Validation des entrées et `@RestControllerAdvice`.
- Schéma versionné par Flyway. `ddl-auto=update` interdit hors tests.
- Deux tests : un unitaire sur RG1, un intégration sur POST /api/presences.
- Frontend : React, build qui passe.
- Trois écrans : formateur, étudiant, relecteur.
- Appels API dans une couche dédiée.
- Données de démonstration au démarrage.
- Démarrage : `docker compose up` ou 3 commandes maximum dans le README.

## 9. Livrables

- `docs/CAHIER_DES_CHARGES.md`
- `docs/diagrammes/D1_cas_utilisation.md`
- `docs/diagrammes/D2_classes.md`
- `docs/diagrammes/D3_sequence_presence.md`
- `docs/diagrammes/D4_etats_exercice.md` (bonus)
- `docs/JOURNAL.md`
- `api/contrat.yaml` complété
- Issues GitHub
- Backend Spring Boot
- Frontend React
- `README.md`
- `CHANGELOG.md`
- `SOUMISSION.md`

## 10. Démarche prévue

1. Analyser et spécifier : ce cahier, les diagrammes, les issues, le contrat.
2. Poser `[JALON] analyse`.
3. Construire les Must : sessions, présences, exercices, relectures, tableau.
4. Poser `[JALON] v0.1`.
5. Ouvrir l’enveloppe, corriger le bug, intégrer le changement.
6. Mettre à jour l’analyse.
7. Livrer la version finale.
8. Poser `[JALON] v1.0`.
9. Soumettre.

**Definition of Done :** une issue est terminée quand le code est sur `main`, les tests passent, la PR est fusionnée, l’issue est fermée par le commit, et le README est à jour si nécessaire.