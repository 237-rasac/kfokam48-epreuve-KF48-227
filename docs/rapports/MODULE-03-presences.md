# Rapport — MODULE 3 : Marquer sa présence avec un code (étudiant)

Issue : #15 · Réf. EF2, EF12, RG1, RG2, RG14, RG15 · PR : #25 (fusionnée sur `main`) · Vérifié le 2026-09-25

## Verdict

**Validé.** Tous les points de l'issue sont couverts et vérifiés sur la stack Docker réelle, y compris le blocage EF12 et sa remise à zéro.

## Ce qui a été implémenté

| Élément de l'issue #15 | État | Où |
|---|---|---|
| `POST /api/presences { code, etudiantId }` → 201 `{ id, sessionId, etudiantId, source: ETUDIANT, marqueeAt }` | ✅ | `PresenceController`, `PresenceService` |
| Code inconnu → 400 `CODE_INCONNU` | ✅ | |
| Code expiré (> 15 min) → 410 `CODE_EXPIRE` (RG1) | ✅ | comparaison avec `expirationAt` de la session |
| Déjà présent → 409 `DEJA_PRESENT` (RG15) | ✅ | `existsBySessionIdAndEtudiantId` + contrainte UNIQUE en base |
| Session clôturée → 410 `SESSION_CLOTUREE` (RG2) | ✅ | vérifié avant l'expiration |
| 5 codes erronés → 429 `ETUDIANT_BLOQUE`, blocage 2 minutes (RG14) | ✅ | `CompteurErreursCode` (en mémoire) |
| Un code valide remet le compteur à zéro | ✅ | test dédié « remise à zéro » |
| Écran étudiant : saisie du code | ✅ | `pages/EcranEtudiant.tsx` (majuscules automatiques) |
| Appel API via la couche dédiée | ✅ | `endpoints.ts` → `marquerPresence` |
| Affichage des erreurs : code inconnu, expiré, déjà présent, bloqué | ✅ | chaque code contractuel affiché, aide « Patientez 2 minutes » sur le blocage |

## Tests exécutés

| Suite | Résultat |
|---|---|
| Backend `CompteurErreursCodeTest` (unitaire RG14) : seuil 5, 4 erreurs → pas de blocage, durée 2 min, compteurs indépendants, reset au succès | ✅ |
| Backend `Module3PresencesTest` (intégration) : 201, 400 `CODE_INCONNU`, 410 `CODE_EXPIRE`, 409, 410 `SESSION_CLOTUREE`, 429 après 5 échecs même avec code valide, 400 `CHAMP_MANQUANT`, remise à zéro | 8/8 ✅ |
| Frontend `module3-presences.test.tsx` : confirmation, `CODE_INCONNU`, `CODE_EXPIRE`, `DEJA_PRESENT`, `ETUDIANT_BLOQUE` | 5/5 ✅ |
| **Stack Docker réelle** : 201, 409 doublon, 429 après 5 erreurs (code valide ensuite refusé), 410 sur le code de session démo expirée | ✅ |

## Détail d'implémentation

- Le compteur EF12 est **en mémoire** (`ConcurrentHashMap`) : aucune table n'est imposée par le contrat ni par le schéma V1, et l'application cible un poste formateur unique. Un redémarrage du backend remet les blocages à zéro — acceptable pour une sanction de 2 minutes.
- L'ordre de vérification est : blocage EF12 → code trouvé → étudiant trouvé → clôture (RG2) → expiration (RG1) → doublon (RG15). Le blocage prime sur tout : un étudiant bloqué ne peut pas sonder les codes.
- Un échec de code n'incrémente le compteur que sur la voie étudiant (utile dès le MODULE 4).
