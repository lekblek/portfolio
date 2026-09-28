package com.scalke.portfolio.backend.shared.error;

/**
 * Contenu envoyé trop volumineux (413 Content Too Large).
 */
public class ContentTooLargeException extends ApplicationException {

    public ContentTooLargeException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
