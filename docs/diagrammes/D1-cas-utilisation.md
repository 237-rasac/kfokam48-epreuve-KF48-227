# D1 — Cas d'utilisation

```mermaid
flowchart LR
    Formateur((Formateur))
    Etudiant((Étudiant))

    subgraph Système["Application KFOKAM48"]
        UC1[Ouvrir une session]
        UC2[Obtenir un code de présence]
        UC3[Voir le tableau de bord]
        UC4[Ajouter une présence manuellement]
        UC5[Clôturer une session]

        UC6[Marquer sa présence avec un code]
        UC7[Déposer un exercice]
        UC8[Remplacer le lien de son exercice]
        UC9[Voir sa note et son commentaire]
        UC10[Relire l'exercice d'un pair]
        UC11[Rendre une note et un commentaire]
        UC12[Corriger sa relecture]

        UC13[Assigner un relecteur au hasard]
        UC14[Bloquer après 5 erreurs de code]
        UC15[Calculer la moyenne]
    end

    Formateur --> UC1
    Formateur --> UC2
    Formateur --> UC3
    Formateur --> UC4
    Formateur --> UC5

    Etudiant --> UC6
    Etudiant --> UC7
    Etudiant --> UC8
    Etudiant --> UC9
    Etudiant --> UC10
    Etudiant --> UC11
    Etudiant --> UC12

    UC1 -.->|include| UC2
    UC6 -.->|include| UC13
    UC6 -.->|include| UC14
    UC3 -.->|include| UC15
    UC11 -.->|extend| UC12