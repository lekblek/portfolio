package com.scalke.portfolio.backend.contact.infrastructure.seed;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import com.scalke.portfolio.backend.contact.domain.port.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Messages de démonstration du profil `dev`, créés uniquement si la base n'en contient aucun : un par statut du
 * cycle sauf l'archive, pour l'administration à venir. Adresses du domaine réservé {@code example.com}.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class ContactMessageSeeder implements ApplicationRunner {

    private final ContactMessageRepository contactMessageRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (contactMessageRepository.existsAny()) {
            return;
        }
        Instant now = clock.instant();
        List.of(
            demo("Camille Martin", "camille.martin@example.com", "Proposition de mission",
                "Bonjour, votre profil correspond à une mission Spring Boot de six mois.",
                now.minus(Duration.ofHours(2)), ContactStatus.NEW, now),
            demo("Alex Durand", "alex.durand@example.com", "Question sur un article",
                "L'exemple de recherche plein texte fonctionne-t-il avec PostgreSQL 17 ?",
                now.minus(Duration.ofDays(2)), ContactStatus.READ, now),
            demo("Sam Bernard", "sam.bernard@example.com", "Merci",
                "Merci pour la série sur l'architecture modulaire.",
                now.minus(Duration.ofDays(10)), ContactStatus.PROCESSED, now)
        ).forEach(contactMessageRepository::create);
        log.info("Messages de contact de démonstration créés (profil dev)");
    }

    private static ContactMessage demo(String name, String email, String subject, String message,
                                       Instant receivedAt, ContactStatus status, Instant now) {
        ContactMessage received = ContactMessage.submit(name, email, subject, message, receivedAt);
        return status == ContactStatus.NEW ? received : received.moveTo(status, now);
    }
}
