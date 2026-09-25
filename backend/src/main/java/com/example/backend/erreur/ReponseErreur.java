package com.example.backend.erreur;

/**
 * Réponse d'erreur imposée par le contrat pour toute erreur, sans exception (ENF4) :
 * {@code { "code": "CODE_EXPIRE", "message": "Le code de présence a expiré." }}.
 */
public record ReponseErreur(String code, String message) {
}
