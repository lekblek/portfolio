package com.scalke.portfolio.backend.security.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.security.domain.model.AdminAccount;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAccountPersistenceMapperTest {

    @Test
    void keeps_every_field_through_a_round_trip() {
        AdminAccount account = new AdminAccount("admin",
            "{bcrypt}$2a$10$zPzd.q.vE0pkkBWPr9yFtemfAy/x6WkCtf4NRVNwC/fWmS0RxZ90u", true,
            Instant.parse("2026-06-15T09:00:00Z"), Instant.parse("2026-06-01T10:00:00Z"),
            Instant.parse("2026-06-15T10:00:00Z"));

        assertThat(AdminAccountPersistenceMapper.toDomain(AdminAccountPersistenceMapper.toNewEntity(account)))
            .isEqualTo(account);
    }
}
