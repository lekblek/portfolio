package com.scalke.portfolio.backend.shared.api;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ContentTooLargeException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.InvalidInputException;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.shared.error.TooManyRequestsException;
import com.scalke.portfolio.backend.shared.error.UnsupportedContentException;
import jakarta.validation.Valid;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * Routes qui lèvent chaque erreur rendue par {@link GlobalExceptionHandler}, pour {@code GlobalExceptionHandlerTest}
 * seulement. {@link TestComponent} l'exclut de l'analyse des composants de tout contexte de test (KI-37) : sans cela,
 * chaque {@code @SpringBootTest} l'enregistrait, et ses routes entraient dans le contrat OpenAPI versionné.
 */
@TestComponent
@RestController
@RequestMapping("/test-errors")
public class ErrorHandlingTestController {

    record SampleRequest(@NotBlank String title) {
    }

    @GetMapping("/not-found")
    void notFound() {
        throw new ResourceNotFoundException(
            ErrorCode.RESOURCE_NOT_FOUND, "Publication introuvable.");
    }

    @GetMapping("/conflict")
    void conflict() {
        throw new BusinessRuleViolationException(
            ErrorCode.SLUG_ALREADY_USED, "Ce slug est déjà utilisé.");
    }

    @GetMapping("/unsupported-content")
    void unsupportedContent() {
        throw new UnsupportedContentException(
            ErrorCode.UNSUPPORTED_MEDIA_FORMAT, "Format de fichier non accepté : PNG, JPEG, WebP ou PDF.");
    }

    @GetMapping("/too-large")
    void tooLarge() {
        throw new ContentTooLargeException(
            ErrorCode.MEDIA_TOO_LARGE, "Fichier trop volumineux : 5 Mo au plus pour ce format.");
    }

    @PostMapping("/validate")
    void validate(@Valid @RequestBody SampleRequest request) {
    }

    @GetMapping("/invalid-input")
    void invalidInput() {
        throw new InvalidInputException("categoryId", "Catégorie inconnue.");
    }

    @GetMapping("/required-param")
    void requiredParam(@RequestParam String value) {
    }

    @GetMapping("/validate-param")
    void validateParam(@RequestParam @Size(max = 3, message = "au plus {max} caractères") String value) {
    }

    @GetMapping("/unauthenticated")
    void unauthenticated() {
        throw new InsufficientAuthenticationException("aucune session");
    }

    @GetMapping("/access-denied")
    void accessDenied() {
        throw new AccessDeniedException("refusé");
    }

    @GetMapping("/too-many")
    void tooMany() {
        throw new TooManyRequestsException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, "Trop de tentatives.",
            Duration.ofMillis(90_500));
    }

    @GetMapping("/boom")
    void boom() {
        throw new IllegalStateException("fuite: mot de passe postgres = s3cr3t");
    }
}
