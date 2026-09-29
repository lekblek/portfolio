package com.scalke.portfolio.backend.contact.infrastructure.persistence.jpa.entity;

import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Structure de persistance d'un message de contact ({@code V020}). Les invariants vivent dans
 * {@code domain.model.ContactMessage} et dans les contraintes de la table (ADR 0001).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "contact_message")
public class ContactMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "message", nullable = false, length = 5000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContactStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    private ContactMessageEntity(String name, String email, String subject, String message, ContactStatus status,
                                 Instant createdAt, Instant updatedAt) {
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Seule écriture après création : les valeurs viennent d'une transition déjà validée par le domaine
     * ({@code ContactMessage.moveTo}).
     */
    public void changeStatus(ContactStatus status, Instant updatedAt) {
        this.status = status;
        this.updatedAt = updatedAt;
    }
}
