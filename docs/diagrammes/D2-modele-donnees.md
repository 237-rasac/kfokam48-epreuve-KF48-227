
---

## Fichier 2 : `docs/diagrammes/D2_classes.md`

```markdown
# D2 — Modèle de données

```mermaid
erDiagram
    PROMOTION ||--o{ ETUDIANT : contient
    PROMOTION ||--o{ SESSION : organise
    SESSION ||--o{ PRESENCE : enregistre
    SESSION ||--o{ EXERCICE : accueille
    ETUDIANT ||--o{ PRESENCE : marque
    ETUDIANT ||--o{ EXERCICE : depose
    %% enveloppe étape 3 : deux relectures par exercice (RG4), une seule par
    %% (exercice, relecteur) — contrainte uk_relecture_exercice_relecteur (V6)
    EXERCICE ||--o{ RELECTURE : genere
    ETUDIANT ||--o{ RELECTURE : effectue

    PROMOTION {
        long id PK
        string nom
    }

    ETUDIANT {
        long id PK
        string nom
        long promotion_id FK
    }

    SESSION {
        long id PK
        string titre
        string code
        timestamp ouverture_at
        timestamp expiration_at
        timestamp cloture_at
        long promotion_id FK
    }

    PRESENCE {
        long id PK
        long session_id FK
        long etudiant_id FK
        string source
        timestamp marquee_at
    }

    EXERCICE {
        long id PK
        long session_id FK
        long etudiant_id FK
        string lien
        string statut
        timestamp depose_at
    }

    RELECTURE {
        long id PK
        long exercice_id FK
        long relecteur_id FK
        int note
        string commentaire
        timestamp rendue_at
    }