package com.scalke.portfolio.backend.contact.domain.model;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Message reçu par le formulaire de contact ({@code 02} §23). Aucune adresse IP n'est conservée.
 * <p>
 * Invariants (D-CG), doublés par PostgreSQL ({@code V020}) : nom, sujet et message non blancs et bornés,
 * adresse électronique de forme {@code local@domaine.tld}, {@code updatedAt} jamais avant {@code createdAt}.
 * Une valeur hors règle est une erreur de programmation ({@link IllegalArgumentException}, D-N) : la saisie
 * est validée avant, par le contrat HTTP ({@code SubmitContactMessageRequest}, D-EJ), avec les mêmes bornes.
 * Invariant 29 : le statut ne change que par {@link #moveTo}.
 */
public record ContactMessage(
    Long id,
    String name,
    String email,
    String subject,
    String message,
    ContactStatus status,
    Instant createdAt,
    Instant updatedAt
) {

    public static final int NAME_MAX_LENGTH = 100;
    public static final int EMAIL_MAX_LENGTH = 254;
    public static final int SUBJECT_MAX_LENGTH = 200;
    public static final int MESSAGE_MAX_LENGTH = 5000;

    /**
     * Forme minimale d'une adresse : pas d'espace, un seul {@code @}, un point dans le domaine. Même expression
     * que {@code contact_message_email_check} ; reprise par la validation du formulaire public (D-EJ).
     */
    public static final String EMAIL_FORMAT = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+";

    private static final Pattern EMAIL = Pattern.compile(EMAIL_FORMAT);

    public ContactMessage {
        requireText(name, "name", NAME_MAX_LENGTH);
        requireText(email, "email", EMAIL_MAX_LENGTH);
        requireText(subject, "subject", SUBJECT_MAX_LENGTH);
        requireText(message, "message", MESSAGE_MAX_LENGTH);
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("email is not an address: " + email);
        }
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    /**
     * Nouveau message, reçu à {@code now} : statut {@code NEW}, espaces de début et de fin retirés.
     */
    public static ContactMessage submit(String name, String email, String subject, String message, Instant now) {
        return new ContactMessage(null, strip(name), strip(email), strip(subject), strip(message),
            ContactStatus.NEW, now, now);
    }

    /**
     * Fait avancer le message dans son cycle (invariant 29) ; {@code updatedAt} devient {@code now}.
     *
     * @throws BusinessRuleViolationException {@code INVALID_CONTACT_MESSAGE_TRANSITION} si {@code target} n'est
     *                                        pas après le statut actuel
     */
    public ContactMessage moveTo(ContactStatus target, Instant now) {
        if (!status.canMoveTo(target)) {
            throw new BusinessRuleViolationException(ErrorCode.INVALID_CONTACT_MESSAGE_TRANSITION,
                "Un message " + status + " ne peut pas passer à " + target + ".");
        }
        return new ContactMessage(id, name, email, subject, message, target, createdAt, now);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    private static void requireText(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
        }
    }
}
