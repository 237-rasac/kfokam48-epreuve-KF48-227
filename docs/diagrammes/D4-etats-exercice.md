
---

## Fichier 4 (bonus +3) : `docs/diagrammes/D4_etats_exercice.md`

```markdown
# D4 — États-transitions : cycle de vie d'un exercice

```mermaid
stateDiagram-v2
    [*] --> EN_ATTENTE : dépôt du lien (EF3)
    EN_ATTENTE --> EN_ATTENTE : remplacement du lien (RG10)
    EN_ATTENTE --> RELU : les deux relectures rendues (EF5/RG17)
    RELU --> RELU : correction de la note (RG7)
    RELU --> [*] : session clôturée
    EN_ATTENTE --> [*] : session clôturée sans relecture complète (RG8)

    note right of EN_ATTENTE
        Deux relecteurs assignés au hasard (RG4/RG5)
        Une seule relecture rendue → sa note est affichée
        mais marquée provisoire (RG8/RG17)
    end note

    note right of RELU
        Notes entières 0–20 (RG6)
        Note retenue = moyenne des relectures rendues (RG17)
        Note et commentaires visibles par l'auteur
        Nom des relecteurs masqué (EF11)
    end note