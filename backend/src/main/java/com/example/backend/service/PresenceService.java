package com.example.backend.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.domaine.Etudiant;
import com.example.backend.domaine.Presence;
import com.example.backend.domaine.SessionCours;
import com.example.backend.domaine.SourcePresence;
import com.example.backend.erreur.ErreurMetierException;
import com.example.backend.repository.EtudiantRepository;
import com.example.backend.repository.PresenceRepository;
import com.example.backend.repository.SessionCoursRepository;

/**
 * Règles de marquage de présence (EF2, EF12, RG1, RG2, RG14, RG15) :
 * - code inconnu → 400 CODE_INCONNU (et compte pour le blocage EF12)
 * - code expiré → 410 CODE_EXPIRE (RG1)
 * - déjà présent → 409 DEJA_PRESENT (RG15)
 * - session clôturée → 410 SESSION_CLOTUREE (RG2)
 * - 5 erreurs → 429 ETUDIANT_BLOQUE pendant 2 minutes (RG14)
 */
@Service
public class PresenceService {

    private final SessionCoursRepository sessions;
    private final EtudiantRepository etudiants;
    private final PresenceRepository presences;
    private final HorlogeMetier horloge;
    private final CompteurErreursCode compteurErreurs;

    public PresenceService(SessionCoursRepository sessions, EtudiantRepository etudiants,
            PresenceRepository presences, HorlogeMetier horloge, CompteurErreursCode compteurErreurs) {
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.presences = presences;
        this.horloge = horloge;
        this.compteurErreurs = compteurErreurs;
    }

    @Transactional
    public Presence marquer(String code, Long etudiantId) {
        if (code == null || code.isBlank()) {
            throw new ErreurMetierException("CHAMP_MANQUANT", 400, "Le code est obligatoire.");
        }
        if (etudiantId == null) {
            throw new ErreurMetierException("CHAMP_MANQUANT", 400, "L'étudiant est obligatoire.");
        }

        // EF12 : on vérifie le blocage avant tout le reste
        if (compteurErreurs.estBloque(etudiantId)) {
            throw new ErreurMetierException("ETUDIANT_BLOQUE", 429,
                    "Trop d'erreurs, réessayez dans 2 minutes.");
        }

        SessionCours session = sessions.findByCode(code.trim())
                .orElseGet(() -> {
                    compteurErreurs.noterEchec(etudiantId);
                    throw new ErreurMetierException("CODE_INCONNU", 400, "Code inconnu.");
                });

        Etudiant etudiant = etudiants.findById(etudiantId)
                .orElseThrow(() -> new ErreurMetierException("ETUDIANT_INCONNU", 404, "Étudiant inconnu."));

        // RG2 : plus de présence après la clôture
        if (session.estCloturee()) {
            throw new ErreurMetierException("SESSION_CLOTUREE", 410, "La session est clôturée.");
        }

        LocalDateTime maintenant = horloge.maintenant();

        // RG1 : le code expire 15 minutes après l'ouverture
        if (maintenant.isAfter(session.getExpirationAt())) {
            throw new ErreurMetierException("CODE_EXPIRE", 410, "Le code de présence a expiré.");
        }

        // RG15 : une seule présence par session et par étudiant
        if (presences.existsBySessionIdAndEtudiantId(session.getId(), etudiantId)) {
            throw new ErreurMetierException("DEJA_PRESENT", 409, "Présence déjà enregistrée.");
        }

        Presence presence = presences.save(
                new Presence(session, etudiant, SourcePresence.ETUDIANT, maintenant));
        compteurErreurs.noterSucces(etudiantId);
        return presence;
    }
}
