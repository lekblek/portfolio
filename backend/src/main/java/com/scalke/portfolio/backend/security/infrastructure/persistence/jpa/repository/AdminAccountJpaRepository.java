package com.scalke.portfolio.backend.security.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.security.infrastructure.persistence.jpa.entity.AdminAccountEntity;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Accès Spring Data au compte administrateur. Seules les méthodes utilisées par l'adaptateur sont déclarées.
 */
public interface AdminAccountJpaRepository extends Repository<AdminAccountEntity, Long> {

    Optional<AdminAccountEntity> findById(Long id);

    AdminAccountEntity save(AdminAccountEntity entity);
}
