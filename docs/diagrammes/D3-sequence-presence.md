
---

## Fichier 3 : `docs/diagrammes/D3_sequence_presence.md`

```markdown
# D3 — Séquence : marquer sa présence

```mermaid
sequenceDiagram
    autonumber
    participant E as Étudiant
    participant F as Frontend React
    participant API as PresenceController
    participant S as PresenceService
    participant R as PresenceRepository
    participant DB as Base de données

    E->>F: saisit le code
    F->>API: POST /api/presences { code, etudiantId }

    API->>S: enregistrer(code, etudiantId)

    S->>R: findByCode(code)
    R->>DB: SELECT session WHERE code = ?
    DB-->>R: aucune session
    R-->>S: Optional.empty()
    S-->>API: CodeInconnuException
    API-->>F: 400 { code: "CODE_INCONNU", message: "Code inconnu." }
    F-->>E: affiche l'erreur

    alt code expiré (RG1)
        S->>S: expirationAt < now
        S-->>API: CodeExpireException
        API-->>F: 410 { code: "CODE_EXPIRE", message: "Le code de présence a expiré." }
        F-->>E: affiche l'erreur
    else étudiant déjà présent (RG15)
        S->>R: existsBySessionAndEtudiant(session, etudiantId)
        R->>DB: SELECT 1 FROM presence WHERE ...
        DB-->>R: true
        R-->>S: true
        S-->>API: DejaPresentException
        API-->>F: 409 { code: "DEJA_PRESENT", message: "Présence déjà enregistrée." }
        F-->>E: affiche l'erreur
    else cas nominal
        S->>R: save(new Presence(session, etudiant, ETUDIANT))
        R->>DB: INSERT INTO presence ...
        DB-->>R: id
        R-->>S: Presence
        S-->>API: Presence
        API-->>F: 201 { id, sessionId, etudiantId, source: "ETUDIANT" }
        F-->>E: affiche la confirmation
    end