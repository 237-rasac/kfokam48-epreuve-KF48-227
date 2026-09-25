package com.example.backend.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.domaine.Etudiant;
import com.example.backend.domaine.Exercice;
import com.example.backend.domaine.Relecture;
import com.example.backend.domaine.SessionCours;
import com.example.backend.erreur.ErreurMetierException;
import com.example.backend.repository.EtudiantRepository;
import com.example.backend.repository.ExerciceRepository;
import com.example.backend.repository.RelectureRepository;
import com.example.backend.repository.SessionCoursRepository;

/**
 * Règles de dépôt et de modification d'exercice (EF3, EF10, EF11, RG9, RG10) :
 * - lien invalide → 400 LIEN_INVALIDE
 * - déjà déposé → 409 EXERCICE_DEJA_DEPOSE
 * - session clôturée → 410 SESSION_CLOTUREE (RG9 : dépôt jusqu'à la clôture)
 * - remplacement du lien interdit dès qu'une relecture est rendue (RG10 → 409
 *   RELECTURE_DEJA_COMMENCEE) ou session clôturée (410)
 */
@Service
public class ExerciceService {

    private final SessionCoursRepository sessions;
    private final EtudiantRepository etudiants;
    private final ExerciceRepository exercices;
    private final RelectureRepository relectures;
    private final HorlogeMetier horloge;
    private final RelectureService relectureService;

    public ExerciceService(SessionCoursRepository sessions, EtudiantRepository etudiants,
            ExerciceRepository exercices, RelectureRepository relectures,
            HorlogeMetier horloge, RelectureService relectureService) {
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.exercices = exercices;
        this.relectures = relectures;
        this.horloge = horloge;
        this.relectureService = relectureService;
    }

    @Transactional
    public Exercice deposer(Long sessionId, Long etudiantId, String lien) {
        if (sessionId == null || etudiantId == null || lien == null || lien.isBlank()) {
            throw new ErreurMetierException("CHAMP_MANQUANT", 400, "Champ obligatoire manquant.");
        }
        SessionCours session = sessions.findById(sessionId)
                .orElseThrow(() -> new ErreurMetierException("SESSION_INCONNUE", 404, "Session inconnue."));
        Etudiant etudiant = etudiants.findById(etudiantId)
                .orElseThrow(() -> new ErreurMetierException("ETUDIANT_INCONNU", 404, "Étudiant inconnu."));

        // RG9 : dépôt possible jusqu'à la clôture
        if (session.estCloturee()) {
            throw new ErreurMetierException("SESSION_CLOTUREE", 410, "La session est clôturée.");
        }
        validerLien(lien);

        if (exercices.existsBySessionIdAndEtudiantId(sessionId, etudiantId)) {
            throw new ErreurMetierException("EXERCICE_DEJA_DEPOSE", 409,
                    "Un exercice a déjà été déposé pour cette session.");
        }

        Exercice exercice = exercices.save(new Exercice(session, etudiant, lien.trim(), horloge.maintenant()));

        // EF4 : assignation immédiate d'un relecteur parmi les présents (RG5)
        relectureService.assignerSiPossible(exercice);
        // On attache la relecture éventuelle au champ transitoire pour la réponse (relecteurAssignee)
        exercice.setRelecture(relectures.findByExerciceId(exercice.getId()).orElse(null));
        return exercice;
    }

    /** EF10/RG10 : remplacer le lien tant qu'aucune relecture n'a commencé. */
    @Transactional
    public Exercice remplacerLien(Long exerciceId, String lien) {
        Exercice exercice = exercices.findById(exerciceId)
                .orElseThrow(() -> new ErreurMetierException("EXERCICE_INCONNU", 404, "Exercice inconnu."));
        if (exercice.getSession().estCloturee()) {
            throw new ErreurMetierException("SESSION_CLOTUREE", 410, "La session est clôturée.");
        }
        validerLien(lien);
        boolean relectureRendue = relectures.existsByExerciceIdAndRendueAtIsNotNull(exerciceId);
        if (relectureRendue) {
            throw new ErreurMetierException("RELECTURE_DEJA_COMMENCEE", 409,
                    "Une relecture a déjà commencé, le lien ne peut plus être remplacé.");
        }
        exercice.setLien(lien.trim());
        return exercices.save(exercice);
    }

    /**
     * EF11 : le relecté voit son statut, sa note et son commentaire, sans l'identité
     * du relecteur. Note/commentaire null tant que la relecture n'est pas rendue.
     */
    @Transactional(readOnly = true)
    public Exercice consulterAvecNote(Long exerciceId) {
        Exercice exercice = exercices.findById(exerciceId)
                .orElseThrow(() -> new ErreurMetierException("EXERCICE_INCONNU", 404, "Exercice inconnu."));
        Relecture relecture = relectures.findByExerciceId(exerciceId).orElse(null);
        exercice.setRelecture(relecture);
        return exercice;
    }

    private void validerLien(String lien) {
        try {
            new URI(lien.trim());
        } catch (URISyntaxException exception) {
            throw new ErreurMetierException("LIEN_INVALIDE", 400, "Le lien fourni est invalide.");
        }
        String propre = lien.trim();
        if (!(propre.startsWith("http://") || propre.startsWith("https://"))) {
            throw new ErreurMetierException("LIEN_INVALIDE", 400, "Le lien fourni est invalide.");
        }
    }
}
