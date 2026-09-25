
---

## Fichier 4 (bonus +3) : `docs/diagrammes/D4_etats_exercice.md`

```markdown
# D4 — États-transitions : cycle de vie d'un exercice

```mermaid
stateDiagram-v2
    [*] --> EN_ATTENTE : dépôt du lien (EF3)
    EN_ATTENTE --> EN_ATTENTE : remplacement du lien (RG10)
    EN_ATTENTE --> RELU : relecture rendue (EF5)
    RELU --> RELU : correction de la note (RG7)
    RELU --> [*] : session clôturée
    EN_ATTENTE --> [*] : session clôturée sans relecture (RG8)

    note right of EN_ATTENTE
        Relecteur assigné au hasard (RG5)
        Aucune note visible par l'auteur
    end note

    note right of RELU
        Note entière 0–20 (RG6)
        Note et commentaire visibles par l'auteur
        Nom du relecteur masqué (EF11)
    end note