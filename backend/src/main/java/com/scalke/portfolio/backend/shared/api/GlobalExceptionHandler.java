package com.scalke.portfolio.backend.shared.api;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ContentTooLargeException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.InvalidInputException;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.shared.error.TooManyRequestsException;
import com.scalke.portfolio.backend.shared.error.UnsupportedContentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
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

    /**
     * 401 : connexion refusée (D-CO). Même réponse pour un identifiant inconnu, un mauvais mot de passe ou un compte
     * désactivé : rien n'est révélé sur le compte.
     */
    @ExceptionHandler({BadCredentialsException.class, AccountStatusException.class})
    ProblemDetail handleInvalidCredentials(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Identifiant ou mot de passe incorrect.", ErrorCode.INVALID_CREDENTIALS);
    }

    /**
     * 401 : route d'administration sans authentification (D-CL). Levée par les filtres de sécurité, transmise ici
     * par {@code SecurityConfiguration} pour une réponse de même forme que les autres erreurs.
     */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentification requise.", ErrorCode.AUTHENTICATION_REQUIRED);
    }

    /**
     * 403 : accès refusé à une identité connue, route non ouverte, ou jeton CSRF absent ou invalide (D-CL). Déclaré
     * explicitement : sans lui, le gestionnaire de {@code Exception} en ferait une 500 (KI-20).
     */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Accès refusé.", ErrorCode.ACCESS_DENIED);
    }

    /**
     * 400 : requête rejetée par le pare-feu HTTP avant tout traitement (D-CL), par exemple un chemin contenant
     * {@code %2F} ou {@code ..}.
     */
    @ExceptionHandler(RequestRejectedException.class)
    ProblemDetail handleRequestRejected(RequestRejectedException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Requête refusée.", ErrorCode.MALFORMED_REQUEST);
    }

    /**
     * 429 : trop de tentatives (D-CQ) ; {@code Retry-After} en secondes, arrondi au-dessus.
     */
    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<ProblemDetail> handleTooManyRequests(TooManyRequestsException ex) {
        long seconds = Math.max(1, (ex.retryAfter().toMillis() + 999) / 1000);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, Long.toString(seconds))
            .body(problem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), ex.errorCode()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), ex.errorCode());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), ex.errorCode());
    }

    /**
     * Valeur refusée par un cas d'usage (référence inconnue, D-CU) : même forme qu'un échec de validation du corps.
     */
    @ExceptionHandler(InvalidInputException.class)
    ProblemDetail handleInvalidInput(InvalidInputException ex) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.errorCode());
        body.setProperty("errors", List.of(Map.of("field", ex.field(), "message", ex.getMessage())));
        return body;
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
     * Requête multipart plus grande que la limite ({@code spring.servlet.multipart}, D-CT) : même code qu'un fichier
     * trop volumineux pour son format (D-BS).
     */
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
        MaxUploadSizeExceededException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

        ProblemDetail body = problem(HttpStatus.CONTENT_TOO_LARGE, "Fichier trop volumineux : 10 Mo au plus.",
            ErrorCode.MEDIA_TOO_LARGE);
        return handleExceptionInternal(ex, body, headers, HttpStatus.CONTENT_TOO_LARGE, request);
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
