# Rapport — MODULE 4 : Ajouter une présence manuellement (formateur)

Issue : #16 · Réf. EF7, RG11 · Vérifié le 2026-09-25

## Verdict

**Validé.** Tous les points de l'issue sont couverts et vérifiés sur la stack Docker réelle. Un choix d'interprétation est documenté plus bas (expiration et blocage outrepassés en voie formateur).

## Ce qui a été implémenté

| Élément de l'issue #16 | État | Où |
|---|---|---|
| `POST /api/presences` accepte `source = FORMATEUR` | ✅ | `PresenceDtos.PresenceCreation` (champ optionnel), `PresenceController.convertirSource` |
| Service : voie formateur via champ explicite (pas d'auth, périmètre cahier §3) | ✅ | `PresenceService.marquer(code, etudiantId, source)` |
| Retour 201 avec `source = FORMATEUR` conservé | ✅ | vérifié E2E |
| Session clôturée → 410 `SESSION_CLOTUREE` | ✅ | RG2 appliqué aux deux voies |
| Doublon → 409 `DEJA_PRESENT` | ✅ | RG15 appliqué aux deux voies |
| Écran formateur : bouton « Ajouter une présence » sur session ouverte | ✅ | `pages/PanelPresencesSession.tsx` (liste + sélection) |
| Sélection de l'étudiant, soumission | ✅ | menu déroulant qui signale les déjà présents |
| La présence apparaît avec un badge « ajouté par le formateur » | ✅ | badge `FORMATEUR` vs `étudiant` dans la liste |

## Tests exécutés

| Suite | Résultat |
|---|---|
| Backend `Module4PresenceManuelleTest` : 201 FORMATEUR, voie étudiant par défaut, expiration outrepassée, doublon 409, clôture 410, étudiant inconnu 404, source invalide 400, présence visible dans `GET /api/sessions/{id}/presences` | 8/8 ✅ |
| Backend suite complète (modules 1 à 4) | **40/40 ✅** |
| Frontend `module4-presence-manuelle.test.tsx` : ajout + badge, `DEJA_PRESENT`, `SESSION_CLOTUREE` | 3/3 ✅ |
| Frontend suite complète | **21/21 ✅**, build OK |
| **Stack Docker réelle** : ajout manuel 201 `FORMATEUR`, doublon 409, session démo clôturée 410, badge visible dans `GET presences` | ✅ |

## Choix d'interprétation documenté

En voie formateur, le service **outrepasse l'expiration du code (RG1)** et **ne consulte pas le blocage EF12** : l'ajout manuel sert précisément à rattraper un étudiant arrivé après l'expiration ou bloqué par erreur. L'issue #16 n'exige que la clôture (410) et le doublon (409) comme blocages, et le cahier des charges présente l'ajout manuel comme correctif du formateur. L'étudiant ajouté manuellement repart avec un compteur d'erreurs remis à zéro.

## Remarque non bloquante

Le code de la session est utilisé comme identifiant par le panneau formateur (il l'affiche déjà) ; une session dédiée « ajout manuel sans code » exigerait une évolution du contrat (`POST /api/sessions/{id}/presences`), hors périmètre de l'issue #16.
