package com.scalke.portfolio.backend.shared.error;

/**
 * Contenu envoyé dans un format refusé (415 Unsupported Media Type).
 */
public class UnsupportedContentException extends ApplicationException {

    public UnsupportedContentException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
