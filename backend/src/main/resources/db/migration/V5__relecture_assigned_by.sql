-- MODULE 10 (issue #22) : traçabilité de l'assignation du relecteur.
-- SYSTEME = tirage au sort à l'inscription (MODULE 5), FORMATEUR = assignation
-- manuelle (MODULE 10). Valeurs du passe : SYSTEME.

ALTER TABLE relecture ADD COLUMN assigned_by VARCHAR(12) NOT NULL DEFAULT 'SYSTEME';
ALTER TABLE relecture ALTER COLUMN assigned_by DROP DEFAULT;
