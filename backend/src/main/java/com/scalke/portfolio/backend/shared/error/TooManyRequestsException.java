package com.scalke.portfolio.backend.shared.error;

import java.time.Duration;
import java.util.Objects;

/**
 * Trop de requêtes de ce client (429 Too Many Requests) : {@code retryAfter} devient l'en-tête {@code Retry-After}.
 */
public class TooManyRequestsException extends ApplicationException {

    private final Duration retryAfter;

    public TooManyRequestsException(ErrorCode errorCode, String detail, Duration retryAfter) {
        super(errorCode, detail);
        this.retryAfter = Objects.requireNonNull(retryAfter, "retryAfter");
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
