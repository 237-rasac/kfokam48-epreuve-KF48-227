-- KFOKAM48 — données de démonstration (cahier des charges §8 : « Données de démonstration au démarrage »)
-- Volumétrie visée par ENF3 : promotions, étudiants, sessions, présences.

INSERT INTO promotion (id, nom) VALUES
    (1, 'KFOKAM48');

INSERT INTO etudiant (id, nom, promotion_id) VALUES
    (1, 'Amina Bello', 1),
    (2, 'Boris Kamdem', 1),
    (3, 'Clarisse Ngo', 1),
    (4, 'David Etoundi', 1),
    (5, 'Emma Fouda', 1);

INSERT INTO session (id, titre, code, ouverture_at, expiration_at, cloture_at, promotion_id) VALUES
    (1, 'Cours Java — 12 mars', 'A7K3P9', TIMESTAMP '2026-03-12 08:00:00', TIMESTAMP '2026-03-12 08:15:00', NULL, 1),
    (2, 'Cours Spring — 19 mars', 'M4X8Q2', TIMESTAMP '2026-03-19 08:00:00', TIMESTAMP '2026-03-19 08:15:00', TIMESTAMP '2026-03-19 10:00:00', 1);

INSERT INTO presence (id, session_id, etudiant_id, source, marquee_at) VALUES
    (1, 1, 1, 'ETUDIANT',  TIMESTAMP '2026-03-12 08:02:00'),
    (2, 1, 2, 'ETUDIANT',  TIMESTAMP '2026-03-12 08:05:00'),
    (3, 1, 3, 'FORMATEUR', TIMESTAMP '2026-03-12 08:10:00'),
    (4, 2, 1, 'ETUDIANT',  TIMESTAMP '2026-03-19 08:01:00');

INSERT INTO exercice (id, session_id, etudiant_id, lien, statut, depose_at) VALUES
    (1, 1, 1, 'https://github.com/amina/exercice-java', 'EN_ATTENTE', TIMESTAMP '2026-03-12 09:00:00'),
    (2, 1, 2, 'https://github.com/boris/exercice-java', 'RELU',       TIMESTAMP '2026-03-12 09:05:00');

INSERT INTO relecture (id, exercice_id, relecteur_id, note, commentaire, rendue_at) VALUES
    (1, 2, 1, 15, 'Bon travail, attention aux cas limites.', TIMESTAMP '2026-03-12 09:30:00');
