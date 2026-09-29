package com.scalke.portfolio.backend.contact.infrastructure.mail;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.port.ContactNotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Notification par courriel texte (D-CI) : à l'administrateur, avec l'adresse du visiteur en {@code Reply-To} pour
 * lui répondre directement. Une erreur SMTP remonte en {@code MailException} (non vérifiée).
 */
@Component
@EnableConfigurationProperties(ContactNotificationProperties.class)
@RequiredArgsConstructor
class EmailContactNotificationSender implements ContactNotificationSender {

    static final String SUBJECT_PREFIX = "[Portfolio] ";

    private final JavaMailSender mailSender;
    private final ContactNotificationProperties properties;

    @Override
    public void messageReceived(ContactMessage message) {
        mailSender.send(toMail(message));
    }

    /**
     * Le sujet du visiteur est ramené sur une ligne : un saut de ligne dans un en-tête pourrait y injecter d'autres
     * en-têtes. L'adresse du visiteur ne contient ni espace ni saut de ligne (invariant de {@code ContactMessage}).
     */
    SimpleMailMessage toMail(ContactMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(properties.from());
        mail.setTo(properties.to());
        mail.setReplyTo(message.email());
        mail.setSubject(SUBJECT_PREFIX + message.subject().replaceAll("\\s+", " "));
        mail.setText("""
            Nouveau message reçu par le formulaire de contact.

            De : %s <%s>
            Reçu le : %s
            Sujet : %s

            %s

            Répondre à ce courriel écrit directement à l'expéditeur.
            """.formatted(message.name(), message.email(), message.createdAt(), message.subject(), message.message()));
        return mail;
    }
}
