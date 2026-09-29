package com.scalke.portfolio.backend.security.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.security.domain.model.AdminAccount;
import com.scalke.portfolio.backend.security.infrastructure.persistence.jpa.entity.AdminAccountEntity;

public final class AdminAccountPersistenceMapper {

    private AdminAccountPersistenceMapper() {
    }

    /**
     * Reconstruit l'{@link AdminAccount} du domaine : ses invariants sont revérifiés à chaque lecture.
     */
    public static AdminAccount toDomain(AdminAccountEntity entity) {
        return new AdminAccount(entity.getLogin(), entity.getPasswordHash(), entity.isEnabled(),
            entity.getLastLoginAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static AdminAccountEntity toNewEntity(AdminAccount account) {
        return AdminAccountEntity.builder()
            .login(account.login())
            .passwordHash(account.passwordHash())
            .enabled(account.enabled())
            .lastLoginAt(account.lastLoginAt())
            .createdAt(account.createdAt())
            .updatedAt(account.updatedAt())
            .build();
    }
}
