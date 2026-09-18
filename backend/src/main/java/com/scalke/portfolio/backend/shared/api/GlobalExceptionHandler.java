package com.scalke.portfolio.backend.shared.api;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


    private static final String CODE_PROPERTY = "code";

    private static ProblemDetail problem(
        HttpStatus status, String detail, ErrorCode code) {
        ProblemDetail problemDetail =
            ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty(CODE_PROPERTY, code.name());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
            "Une erreur inattendue est survenue.",
            ErrorCode.INTERNAL_ERROR);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), ex.errorCode());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), ex.errorCode());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

        ProblemDetail body = ex.getBody();
        body.setProperty(CODE_PROPERTY, ErrorCode.VALIDATION_FAILED.name());
        body.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .map(error -> Map.of(
                "field", error.getField(),
                "message", Objects.requireNonNullElse(
                    error.getDefaultMessage(), "valeur invalide")))
            .toList());

        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex,
        Object body,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest request) {

        if (body instanceof ProblemDetail problemDetail) {
            Map<String, Object> properties = problemDetail.getProperties();
            if (properties == null || !properties.containsKey(CODE_PROPERTY)) {
                problemDetail.setProperty(
                    CODE_PROPERTY, defaultCodeFor(statusCode).name());
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static ErrorCode defaultCodeFor(HttpStatusCode status) {
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return ErrorCode.RESOURCE_NOT_FOUND;
        }
        if (status.is4xxClientError()) {
            return ErrorCode.MALFORMED_REQUEST;
        }
        return ErrorCode.INTERNAL_ERROR;
    }
}
