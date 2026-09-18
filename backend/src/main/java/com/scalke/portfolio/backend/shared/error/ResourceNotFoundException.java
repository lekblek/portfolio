package com.scalke.portfolio.backend.shared.error;

public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
