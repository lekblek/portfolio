package com.scalke.portfolio.backend.contact.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.port.ContactMessageRepository;
import com.scalke.portfolio.backend.contact.infrastructure.persistence.jpa.entity.ContactMessageEntity;
import com.scalke.portfolio.backend.contact.infrastructure.persistence.jpa.mapper.ContactMessagePersistenceMapper;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptateur JPA du port {@link ContactMessageRepository}. Chaque méthode est transactionnelle pour rester
 * correcte hors cas d'usage (seed de développement) ; appelée depuis un cas d'usage, elle rejoint sa transaction.
 */
@Repository
@RequiredArgsConstructor
public class ContactMessageRepositoryAdapter implements ContactMessageRepository {

    /**
     * Les plus récents d'abord, tri total (D-CZ).
     */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ContactMessageJpaRepository repository;

    @Override
    @Transactional
    public ContactMessage create(ContactMessage message) {
        if (message.id() != null) {
            throw new IllegalArgumentException("create expects a new message, got id " + message.id());
        }
        return ContactMessagePersistenceMapper.toDomain(
            repository.save(ContactMessagePersistenceMapper.toNewEntity(message)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContactMessage> findById(Long id) {
        return repository.findById(id).map(ContactMessagePersistenceMapper::toDomain);
    }

    /**
     * Modifie l'entité gérée ; l'écriture a lieu à la validation de la transaction (dirty checking).
     */
    @Override
    @Transactional
    public ContactMessage updateStatus(ContactMessage message) {
        ContactMessageEntity entity = repository.findById(message.id())
            .orElseThrow(() -> new IllegalStateException("contact message " + message.id() + " does not exist"));
        entity.changeStatus(message.status(), message.updatedAt());
        return ContactMessagePersistenceMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void delete(ContactMessage message) {
        repository.deleteById(message.id());
        repository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAny() {
        return repository.count() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ContactMessage> findPage(ContactStatus status, PageQuery query) {
        Pageable pageable = PageRequest.of(query.page(), query.size(), NEWEST_FIRST);
        Page<ContactMessage> page = (status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable))
            .map(ContactMessagePersistenceMapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
