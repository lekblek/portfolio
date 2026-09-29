package com.scalke.portfolio.backend.contact.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ContactMessagePersistenceMapperTest {

    @Test
    void keeps_every_field_through_a_round_trip() {
        ContactMessage message = new ContactMessage(null, "Camille", "camille@example.com", "Mission", "Bonjour.",
            ContactStatus.READ, Instant.parse("2026-06-15T10:00:00Z"), Instant.parse("2026-06-15T11:00:00Z"));

        assertThat(ContactMessagePersistenceMapper.toDomain(ContactMessagePersistenceMapper.toNewEntity(message)))
            .isEqualTo(message);
    }
}
