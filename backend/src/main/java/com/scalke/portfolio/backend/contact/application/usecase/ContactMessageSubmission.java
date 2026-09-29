package com.scalke.portfolio.backend.contact.application.usecase;

/**
 * Saisie d'un visiteur, telle que la transmettra le formulaire de contact (route à l'étape 48, D-CG).
 */
public record ContactMessageSubmission(String name, String email, String subject, String message) {
}
