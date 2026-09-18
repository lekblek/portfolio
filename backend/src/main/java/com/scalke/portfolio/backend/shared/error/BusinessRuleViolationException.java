package com.scalke.portfolio.backend.shared.error;

public class BusinessRuleViolationException extends ApplicationException {

    public BusinessRuleViolationException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
