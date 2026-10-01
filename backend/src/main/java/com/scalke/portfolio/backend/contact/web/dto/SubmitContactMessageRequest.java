package com.scalke.portfolio.backend.contact.web.dto;

import com.scalke.portfolio.backend.contact.application.usecase.ContactMessageSubmission;
import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Message envoyé par le formulaire de contact public (D-EJ), validé avec les bornes du domaine (D-CG).
 * <p>
 * {@code website} est le piège à robots : un champ que le formulaire cache aux personnes. Rempli, le message est
 * écarté sans être enregistré, avec la même réponse qu'un envoi réussi.
 */
public record SubmitContactMessageRequest(
    @NotBlank @Size(max = ContactMessage.NAME_MAX_LENGTH) String name,
    @NotBlank @Size(max = ContactMessage.EMAIL_MAX_LENGTH)
    @Pattern(regexp = ContactMessage.EMAIL_FORMAT, message = EMAIL_MESSAGE)
    String email,
    @NotBlank @Size(max = ContactMessage.SUBJECT_MAX_LENGTH) String subject,
    @NotBlank @Size(max = ContactMessage.MESSAGE_MAX_LENGTH) String message,
    @Size(max = TRAP_MAX_LENGTH) String website
) {

    static final String EMAIL_MESSAGE = "adresse électronique attendue, de la forme nom@domaine.fr";
    static final int TRAP_MAX_LENGTH = 200;

    public boolean trapped() {
        return website != null && !website.isBlank();
    }

    public ContactMessageSubmission toSubmission() {
        return new ContactMessageSubmission(name, email, subject, message);
    }
}
