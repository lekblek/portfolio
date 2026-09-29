package com.scalke.portfolio.backend.shared.error;

/**
 * Valeur saisie refusée par un cas d'usage, et non par les contraintes du DTO : typiquement une référence vers une
 * ressource inconnue (D-CU). Rendue comme un échec de validation : 400 {@code VALIDATION_FAILED}, avec le champ.
 */
public class InvalidInputException extends ApplicationException {

    private final String field;

    public InvalidInputException(String field, String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
