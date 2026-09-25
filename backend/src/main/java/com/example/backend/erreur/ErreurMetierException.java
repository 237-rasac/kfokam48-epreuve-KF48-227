package com.example.backend.erreur;

/**
 * Exception métier portant un code d'erreur du catalogue du contrat
 * ({@code x-codes-erreur}) et le statut HTTP associé (ENF4).
 */
public class ErreurMetierException extends RuntimeException {

    private final transient String code;
    private final transient int status;

    public ErreurMetierException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
