package com.example.backend.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
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
 * Règles de marquage de présence (EF2, EF7, EF12, RG1, RG2, RG11, RG14, RG15).
 *
 * Voie étudiant (source=ETUDIANT, défaut) :
 * - code inconnu → 400 CODE_INCONNU (compte pour le blocage EF12)
 * - code expiré → 410 CODE_EXPIRE (RG1)
 * - déjà présent → 409 DEJA_PRESENT (RG15)
 * - session clôturée → 410 SESSION_CLOTUREE (RG2)
 * - 5 erreurs → 429 ETUDIANT_BLOQUE pendant 2 minutes (RG14)
 *
 * Voie formateur (source=FORMATEUR, EF7/RG11 — champ explicite dans la requête,
 * pas d'authentification au périmètre) :
 * - outrepasse l'expiration du code (RG1) et le blocage (EF12) : c'est le sens
 *   d'une ajout manuel ; l'issue #16 n'exige que la clôture comme blocage
 * - session clôturée → 410 SESSION_CLOTUREE (RG2, exigé par l'issue #16)
 * - déjà présent → 409 DEJA_PRESENT (RG15)
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
    public Presence marquer(String code, Long etudiantId, SourcePresence source) {
        if (code == null || code.isBlank()) {
            throw new ErreurMetierException("CHAMP_MANQUANT", 400, "Le code est obligatoire.");
        }
        if (etudiantId == null) {
            throw new ErreurMetierException("CHAMP_MANQUANT", 400, "L'étudiant est obligatoire.");
        }
        SourcePresence sourceEffective = source == null ? SourcePresence.ETUDIANT : source;

        if (sourceEffective == SourcePresence.ETUDIANT) {
            // EF12 : on vérifie le blocage avant tout le reste (voie étudiant seulement)
            if (compteurErreurs.estBloque(etudiantId)) {
                throw new ErreurMetierException("ETUDIANT_BLOQUE", 429,
                        "Trop d'erreurs, réessayez dans 2 minutes.");
            }
        }

        // Les codes sont générés en majuscules : « a7k3p9 » désigne la même session que « A7K3P9 »
        SessionCours session = sessions.findByCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseGet(() -> {
                    if (sourceEffective == SourcePresence.ETUDIANT) {
                        compteurErreurs.noterEchec(etudiantId);
                    }
                    throw new ErreurMetierException("CODE_INCONNU", 400, "Code inconnu.");
                });

        Etudiant etudiant = etudiants.findById(etudiantId)
                .orElseThrow(() -> new ErreurMetierException("ETUDIANT_INCONNU", 404, "Étudiant inconnu."));

        // RG2 : plus de présence après la clôture (étudiant comme formateur, issue #16)
        if (session.estCloturee()) {
            throw new ErreurMetierException("SESSION_CLOTUREE", 410, "La session est clôturée.");
        }

        LocalDateTime maintenant = horloge.maintenant();

        // RG1 : le code expire 15 minutes après l'ouverture — voie étudiant seulement ;
        // l'ajout manuel du formateur (EF7/RG11) sert précisément à rattraper ces cas
        if (sourceEffective == SourcePresence.ETUDIANT && maintenant.isAfter(session.getExpirationAt())) {
            throw new ErreurMetierException("CODE_EXPIRE", 410, "Le code de présence a expiré.");
        }

        // RG15 : une seule présence par session et par étudiant
        if (presences.existsBySessionIdAndEtudiantId(session.getId(), etudiantId)) {
            throw new ErreurMetierException("DEJA_PRESENT", 409, "Présence déjà enregistrée.");
        }

        Presence presence;
        try {
            presence = presences.save(new Presence(session, etudiant, sourceEffective, maintenant));
        } catch (DataIntegrityViolationException e) {
            // Issue #39 : le contrôle RG15 ci-dessus est un check-then-act ; sous
            // concurrence, deux requêtes peuvent le franchir avant le commit de
            // l'autre. La contrainte uk_presence_session_etudiant (V1) arbitre alors
            // la course : traduite en 409 contractuel DEJA_PRESENT au lieu d'un 500
            // ERREUR_INTERNE (le générique ExceptionHandler la laissait filer).
            throw new ErreurMetierException("DEJA_PRESENT", 409, "Présence déjà enregistrée.");
        }
        // Un étudiant présent (par lui-même ou ajouté par le formateur) repart de zéro
        compteurErreurs.noterSucces(etudiantId);
        return presence;
    }
}
