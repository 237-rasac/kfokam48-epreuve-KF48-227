package com.example.backend.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * EF12 / RG14 : après 5 erreurs de code, l'étudiant est bloqué 2 minutes.
 * Un code valide remet le compteur à zéro (issue #15).
 * Compteurs en mémoire (aucune table imposée par le contrat) ; suffisant car
 * l'application tourne sur un poste formateur unique.
 * Le temps vient de {@link HorlogeMetier} pour pouvoir tester la levée du blocage.
 */
@Component
public class CompteurErreursCode {

    /** Seuil d'erreurs avant blocage (EF12). */
    public static final int SEUIL_ERREURS = 5;

    /** Durée du blocage après le seuil (RG14). */
    public static final Duration DUREE_BLOCAGE = Duration.ofMinutes(2);

    /** Immuable : chaque mise à jour passe par compute(), atomique par étudiant. */
    private record Etat(int erreurs, LocalDateTime bloqueJusqua) {
    }

    private final Map<Long, Etat> compteurs = new ConcurrentHashMap<>();
    private final HorlogeMetier horloge;

    public CompteurErreursCode(HorlogeMetier horloge) {
        this.horloge = horloge;
    }

    /** L'étudiant est-il actuellement bloqué ? Un blocage expiré est nettoyé. */
    public boolean estBloque(Long etudiantId) {
        LocalDateTime maintenant = horloge.maintenant();
        Etat etat = compteurs.computeIfPresent(etudiantId, (id, courant) ->
                courant.bloqueJusqua() != null && !maintenant.isBefore(courant.bloqueJusqua()) ? null : courant);
        return etat != null && etat.bloqueJusqua() != null;
    }

    /** Enregistre une erreur de code et bloque l'étudiant dès que le seuil est atteint. */
    public void noterEchec(Long etudiantId) {
        LocalDateTime maintenant = horloge.maintenant();
        compteurs.compute(etudiantId, (id, courant) -> {
            int erreurs = (courant == null ? 0 : courant.erreurs()) + 1;
            return new Etat(erreurs, erreurs >= SEUIL_ERREURS ? maintenant.plus(DUREE_BLOCAGE) : null);
        });
    }

    /** Un code valide remet le compteur d'erreurs à zéro (issue #15). */
    public void noterSucces(Long etudiantId) {
        compteurs.remove(etudiantId);
    }
}
