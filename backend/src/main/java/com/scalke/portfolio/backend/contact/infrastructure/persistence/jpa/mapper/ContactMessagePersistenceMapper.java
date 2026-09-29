package com.scalke.portfolio.backend.contact.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.infrastructure.persistence.jpa.entity.ContactMessageEntity;

public final class ContactMessagePersistenceMapper {

    private ContactMessagePersistenceMapper() {
    }

    /**
     * Reconstruit le {@link ContactMessage} du domaine : ses invariants sont revérifiés à chaque lecture.
     */
    public static ContactMessage toDomain(ContactMessageEntity entity) {
        return new ContactMessage(entity.getId(), entity.getName(), entity.getEmail(), entity.getSubject(),
            entity.getMessage(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    /**
     * Nouvelle entité, sans identifiant : attribué par PostgreSQL à l'insertion.
     */
    public static ContactMessageEntity toNewEntity(ContactMessage message) {
        return ContactMessageEntity.builder()
            .name(message.name())
            .email(message.email())
            .subject(message.subject())
            .message(message.message())
            .status(message.status())
            .createdAt(message.createdAt())
            .updatedAt(message.updatedAt())
            .build();
    }
}
