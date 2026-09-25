-- MODULE 11 (issue #40) : deux relectures par exercice (RG4 modifiée).
-- Changement de besoin [JALON] v0.1 (enveloppe étape 3, point 2) : chaque
-- exercice est relu par deux pairs différents ; note retenue = moyenne des
-- deux ; si un seul a rendu, note affichée mais provisoire (RG17).
--
-- V1–V5 restent intacts (prouvable au git log) : une migration versionnée
-- n'est jamais retouchée, le changement passe par un nouveau fichier.
--
-- La base déjà remplie (données de démo V2) survit à la migration : la
-- relecture existante (exercice 2, relecteur 1, note 15) ne viole pas la
-- nouvelle contrainte — sa note devient simplement provisoire tant que le
-- second relecteur attendu n'a pas rendu (traité côté backend, MODULE 12).

-- RG4 (modifiée) : un exercice n'a plus un relecteur unique...
ALTER TABLE relecture DROP CONSTRAINT uk_relecture_exercice;

-- ... mais au plus une relecture par paire (exercice, relecteur) : les deux
-- relecteurs d'un exercice sont distincts, et un même relecteur ne peut pas
-- être assigné deux fois sur le même exercice (409 RELECTURE_DEJA_ASSIGNEE).
ALTER TABLE relecture ADD CONSTRAINT uk_relecture_exercice_relecteur UNIQUE (exercice_id, relecteur_id);
