package com.scalke.portfolio.backend.security.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.security.domain.model.AdminAccount;
import com.scalke.portfolio.backend.security.domain.port.AdminAccountRepository;
import com.scalke.portfolio.backend.security.infrastructure.persistence.jpa.entity.AdminAccountEntity;
import com.scalke.portfolio.backend.security.infrastructure.persistence.jpa.mapper.AdminAccountPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptateur JPA du port {@link AdminAccountRepository} : la seule ligne est celle d'identifiant
 * {@link AdminAccountEntity#SINGLETON_ID}.
 */
@Repository
@RequiredArgsConstructor
public class AdminAccountRepositoryAdapter implements AdminAccountRepository {

    private final AdminAccountJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminAccount> find() {
        return repository.findById(AdminAccountEntity.SINGLETON_ID).map(AdminAccountPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public AdminAccount create(AdminAccount account) {
        return AdminAccountPersistenceMapper.toDomain(repository.save(AdminAccountPersistenceMapper.toNewEntity(account)));
    }

    /**
     * Modifie l'entité gérée ; l'écriture a lieu à la validation de la transaction (dirty checking).
     */
    @Override
    @Transactional
    public AdminAccount updateCredentials(AdminAccount account) {
        AdminAccountEntity entity = repository.findById(AdminAccountEntity.SINGLETON_ID)
            .orElseThrow(() -> new IllegalStateException("the administrator account does not exist"));
        entity.changeCredentials(account.login(), account.passwordHash(), account.updatedAt());
        return AdminAccountPersistenceMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public AdminAccount recordLogin(AdminAccount account) {
        AdminAccountEntity entity = repository.findById(AdminAccountEntity.SINGLETON_ID)
            .orElseThrow(() -> new IllegalStateException("the administrator account does not exist"));
        entity.recordLogin(account.lastLoginAt());
        return AdminAccountPersistenceMapper.toDomain(entity);
    }
}
