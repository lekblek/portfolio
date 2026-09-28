package com.scalke.portfolio.backend.shared.api;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ContentTooLargeException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.shared.error.UnsupportedContentException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/boom")
    void boom() {
        throw new IllegalStateException("fuite: mot de passe postgres = s3cr3t");
    }
}
