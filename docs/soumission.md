# Soumission — Épreuve finale fullstack KFOKAM48

> Remplis ce fichier, **vérifie tes deux liens depuis une fenêtre de navigation privée**,
> puis téléverse-le sur la plateforme **avant 18h00**.
> Sans ce dépôt sur la plateforme, tu n'as rien rendu.

---

## Candidat

| | |
|---|---|
| Nom et prénom(s) | NJANKOUO KAMDJOU Abdoul Rasac |
| Matricule | KF48-YA0-227 |
| Centre | Yaoundé |
| Compte GitHub | [237-rasac](https://github.com/237-rasac) |

## Projet

| | |
|---|---|
| Dépôt (public) | [`https://github.com/237-rasac/kfokam48-epreuve-KF48-227`](https://github.com/237-rasac/kfokam48-epreuve-KF48-227) |
| Commit final — hash complet, 40 caractères | `97a1b70d9862eecb19e1db75106ed0d01892f443` |
| Branche | `main` |

## Épreuve Git — étape 5

| | |
|---|---|
| Dépôt (public) | `https://github.com/237-rasac/kfokam48-gitlab-KF48-227` |
| Commit final — hash complet, 40 caractères | |

## Technique

| | |
|---|---|
| Frontend utilisé | React (Vite, TypeScript) |
| Base de données | PostgreSQL 16, schéma versionné par Flyway (H2 en mémoire pour les tests) |
| Commandes de démarrage | 1. `cd backend && docker compose up -d --build` (base et API sur http://localhost:8080)<br>2. `cd frontend && npm install`<br>3. `npm run dev` (application sur http://localhost:5173) |

## Ce que j'ai livré

Six modules sur dix sont fusionnés sur `main`, avec leurs tests backend et frontend :
- ouverture de session avec un code valable 15 minutes (EF1) ;
- présence par code, avec blocage de 2 minutes après 5 erreurs (EF2, EF12) ;
- ajout manuel d'une présence par le formateur (EF7) ;
- dépôt d'exercice avec assignation aléatoire d'un relecteur, remplacement du lien et consultation de la note (EF3, EF4, EF10, EF11) ;
- rendu d'une relecture notée de 0 à 20 (EF5).

Le tableau de bord (EF6) et la clôture de session (EF8) ne sont pas encore livrés. La correction de note avant clôture (EF9) non plus.

J'ai volontairement exclu l'authentification (cahier des charges §3). L'identité est choisie dans une liste.

---

## Avant de téléverser, vérifie

- [ ] Mes deux dépôts sont **publics** et s'ouvrent en navigation privée
- [ ] Les deux hash font bien **40 caractères** et existent sur GitHub
- [ ] Tout mon travail est **poussé** — `git status` est propre sur les deux dépôts
- [ ] Mon `README` a été testé depuis un clone vierge, dans un dossier vide
- [x] Mon `JOURNAL.md` et mon cahier des charges sont dans `docs/`
- [ ] Les trois commits `[JALON]` sont poussés et dans le bon ordre

---

**Déclaration.** J'ai réalisé ce travail seul. Les outils d'IA étaient autorisés sans restriction et je les ai utilisés ; mon journal indique où et comment j'ai vérifié leurs réponses. Mes dépôts resteront publics et inchangés jusqu'à la publication des résultats.

Signature : ______________________  Date : __________
