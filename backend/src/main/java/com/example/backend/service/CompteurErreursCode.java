package com.example.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * EF12 / RG14 : après 5 erreurs de code, l'étudiant est bloqué 2 minutes.
 * Un code valide remet le compteur à zéro (issue #15).
 * Compteurs en mémoire (aucune table imposée par le contrat) ; suffisant car
 * l'application tourne sur un poste formateur unique.
 */
@Component
public class CompteurErreursCode {

    /** Seuil d'erreurs avant blocage (EF12). */
    public static final int SEUIL_ERREURS = 5;

    /** Durée du blocage après le seuil (RG14). */
    public static final Duration DUREE_BLOCAGE = Duration.ofMinutes(2);

    private static final class Etat {
        private int erreurs;
        private Instant bloqueJusqua;
    }

    private final Map<Long, Etat> compteurs = new ConcurrentHashMap<>();

    /** L'étudiant est-il actuellement bloqué ? Un blocage expiré est nettoyé. */
    public boolean estBloque(Long etudiantId) {
        Etat etat = compteurs.get(etudiantId);
        if (etat == null) {
            return false;
        }
        if (etat.bloqueJusqua != null) {
            if (Instant.now().isBefore(etat.bloqueJusqua)) {
                return true;
            }
            compteurs.remove(etudiantId);
        }
        return false;
    }

    /** Enregistre une erreur de code et bloque l'étudiant dès que le seuil est atteint. */
    public void noterEchec(Long etudiantId) {
        Etat etat = compteurs.computeIfAbsent(etudiantId, id -> new Etat());
        etat.erreurs++;
        if (etat.erreurs >= SEUIL_ERREURS) {
            etat.bloqueJusqua = Instant.now().plus(DUREE_BLOCAGE);
        }
    }

    /** Un code valide remet le compteur d'erreurs à zéro (issue #15). */
    public void noterSucces(Long etudiantId) {
        compteurs.remove(etudiantId);
    }
}
