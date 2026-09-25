package com.example.backend.service;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Générateur du code de présence : 6 caractères A-Z/0-9, unique parmi toutes
 * les sessions (RG16). La vérification d'unicité est faite par le service.
 */
@Component
public class GenerateurCode {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGUEUR = 6;

    private final SecureRandom aleatoire = new SecureRandom();

    public String generer() {
        StringBuilder sb = new StringBuilder(LONGUEUR);
        for (int i = 0; i < LONGUEUR; i++) {
            sb.append(ALPHABET.charAt(aleatoire.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** Génère un code distinct de ceux déjà pris (le service fournit l'ensemble des codes existants). */
    public String genererUnique(Set<String> codesExistants) {
        Set<String> pris = codesExistants == null ? Set.of() : codesExistants;
        String code = generer();
        int tentatives = 0;
        while (pris.contains(code)) {
            code = generer();
            if (++tentatives > 100) {
                // Probabilité quasi nulle (32^6 = ~1 milliard de combinaisons) : filet de sécurité
                throw new IllegalStateException("Impossible de générer un code de session unique.");
            }
        }
        return code;
    }

    /** Utilitaire de test : ensemble des codes générés pour vérifier l'unicité. */
    public static Set<String> echantillon(int n) {
        GenerateurCode g = new GenerateurCode();
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < n; i++) {
            codes.add(g.generer());
        }
        return codes;
    }
}
