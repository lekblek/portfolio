package com.scalke.portfolio.backend.contact.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import com.scalke.portfolio.backend.contact.infrastructure.persistence.jpa.entity.ContactMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Accès Spring Data aux messages de contact. Seules les méthodes utilisées par l'adaptateur sont déclarées.
 */
public interface ContactMessageJpaRepository extends Repository<ContactMessageEntity, Long> {

    Optional<ContactMessageEntity> findById(Long id);

    long count();

    ContactMessageEntity save(ContactMessageEntity entity);

    Page<ContactMessageEntity> findAll(Pageable pageable);

    Page<ContactMessageEntity> findByStatus(ContactStatus status, Pageable pageable);
}
