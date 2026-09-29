package com.scalke.portfolio.backend.shared.api;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ContentTooLargeException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.shared.error.UnsupportedContentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
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

    @ExceptionHandler(UnsupportedContentException.class)
    ProblemDetail handleUnsupportedContent(UnsupportedContentException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), ex.errorCode());
    }

    @ExceptionHandler(ContentTooLargeException.class)
    ProblemDetail handleContentTooLarge(ContentTooLargeException ex) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, ex.getMessage(), ex.errorCode());
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

    /**
     * Contrainte Jakarta Validation posée sur un paramètre de requête (ex. longueur de {@code q}, D-CF) : même
     * forme que l'échec de validation d'un corps JSON ; {@code field} est le nom du paramètre.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
        HandlerMethodValidationException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

        ProblemDetail body = ex.getBody();
        body.setProperty(CODE_PROPERTY, ErrorCode.VALIDATION_FAILED.name());
        body.setProperty("errors", ex.getParameterValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream()
                .map(error -> Map.of(
                    "field", Objects.requireNonNullElse(
                        result.getMethodParameter().getParameterName(), "paramètre"),
                    "message", Objects.requireNonNullElse(
                        error.getDefaultMessage(), "valeur invalide"))))
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

        // Certaines exceptions de Spring (paramètre obligatoire absent, par exemple) arrivent sans corps : la
        // classe mère le construirait après ce contrôle, sans code. Il est donc construit ici, de la même façon.
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        }
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
