package com.example.backend.erreur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Format d'erreur unique { code, message } pour toutes les erreurs, jamais de stack trace (ENF4).
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireErreurs.class);

    /** Erreurs métier : code contractuel + statut portés par l'exception. */
    @ExceptionHandler(ErreurMetierException.class)
    public ResponseEntity<ReponseErreur> traiterMetier(ErreurMetierException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ReponseErreur(exception.getCode(), exception.getMessage()));
    }

    /** Corps illisible / JSON invalide → 400 CHAMP_MANQUANT. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ReponseErreur> traiterCorpsIllisible(HttpMessageNotReadableException exception) {
        return repondre(HttpStatus.BAD_REQUEST, "CHAMP_MANQUANT",
                "Corps de requête invalide ou champ obligatoire manquant.");
    }

    /** Échec de validation bean validation → 400 CHAMP_MANQUANT. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ReponseErreur> traiterValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erreur -> erreur.getField() + " : " + erreur.getDefaultMessage())
                .orElse("Champ obligatoire manquant.");
        return repondre(HttpStatus.BAD_REQUEST, "CHAMP_MANQUANT", message);
    }

    /** Paramètre de requête ou de chemin invalide → 400 CHAMP_MANQUANT. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ReponseErreur> traiterParametreInvalide(MethodArgumentTypeMismatchException exception) {
        return repondre(HttpStatus.BAD_REQUEST, "CHAMP_MANQUANT",
                "Paramètre invalide : " + exception.getName() + ".");
    }

    /** Route inconnue → 404 sous le format du contrat (jamais de page d'erreur HTML). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ReponseErreur> traiterRouteInconnue(NoResourceFoundException exception) {
        return repondre(HttpStatus.NOT_FOUND, "RESSOURCE_INCONNUE", "Ressource inconnue.");
    }

    /** Filet de sécurité : toute erreur non prévue renvoie le format du contrat, sans stack trace. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ReponseErreur> traiterInattendu(Exception exception) {
        log.error("Erreur interne non prévue", exception);
        return repondre(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE", "Erreur interne.");
    }

    private ResponseEntity<ReponseErreur> repondre(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ReponseErreur(code, message));
    }
}
