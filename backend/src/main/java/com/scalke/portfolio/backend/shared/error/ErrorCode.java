package com.scalke.portfolio.backend.shared.error;

public enum ErrorCode {

    // génériques
    RESOURCE_NOT_FOUND,
    VALIDATION_FAILED,
    MALFORMED_REQUEST,
    INTERNAL_ERROR,

    // publication
    SLUG_ALREADY_USED,
    INVALID_PUBLICATION_TRANSITION,

    // series
    SERIES_POSITION_ALREADY_USED,
    NEWS_CANNOT_JOIN_SERIES,

    // media
    MEDIA_STILL_REFERENCED,
    UNSUPPORTED_MEDIA_FORMAT
}
