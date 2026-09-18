package com.scalke.portfolio.backend.shared.api;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
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

    @PostMapping("/validate")
    void validate(@Valid @RequestBody SampleRequest request) {
    }

    @GetMapping("/boom")
    void boom() {
        throw new IllegalStateException("fuite: mot de passe postgres = s3cr3t");
    }
}
