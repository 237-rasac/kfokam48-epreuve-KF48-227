# Journal de projet

## 2026-09-25

### Initialisation de la documentation

- Création du cahier des charges dans `docs/CAHIER_DES_CHARGES.md`.
- Création des diagrammes d'architecture et de séquence dans `docs/DIAGRAMMES.md`.
- Formalisation du besoin de préciser le domaine métier, les rôles et les critères de recette.

### Configuration du backend

- Vérification de la configuration initiale du backend Spring Boot.
- Mise à jour de `backend/.gitignore` pour exclure `.env` et les variantes `.env.*`.
- Création de `backend/.env` pour la configuration locale.
- Création de `backend/.env.example` comme modèle de configuration versionnable.

### Vérifications

- Les fichiers d'environnement créés sont présents dans `backend`.
- Le test Maven `backend\mvnw.cmd test` a été exécuté.
- Le test échoue actuellement au chargement du contexte Spring : aucune source de données ni driver JDBC n'est configuré.
- Le fichier `backend/compose.yaml` contient encore `services: {}` et ne fournit donc pas de base de données.

### Décisions et prochaines actions

- Définir le domaine métier et les user stories.
- Confirmer le SGBD et ajouter le driver JDBC correspondant.
- Documenter les migrations Flyway.
- Définir les contrats API avant l'implémentation des ressources métier.
- Compléter les scénarios de recette et les tests associés.
