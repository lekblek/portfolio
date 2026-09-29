package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.entity.TagEntity;
import com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.mapper.TagPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adaptateur JPA du port {@link TagRepository}. Les écritures sont envoyées immédiatement ({@code flush}) pour
 * traduire un refus de PostgreSQL en erreur métier (D-CS) : nom ou slug pris entre la vérification et l'écriture
 * ({@code tag_name_unique_idx}, {@code tag_slug_unique}), tag encore utilisé ({@code publication_tag_tag_fk}).
 */
@Repository
@RequiredArgsConstructor
public class TagRepositoryAdapter implements TagRepository {

    private final TagJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> findBySlug(Slug slug) {
        return repository.findBySlug(slug.value()).map(TagPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> findAllById(Collection<Long> ids) {
        return repository.findByIdIn(ids).stream().map(TagPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> findAll() {
        return repository.findAllByName().stream().map(TagPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> findById(Long id) {
        return repository.findById(id).map(TagPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(Slug slug, Long excludedId) {
        return repository.existsBySlug(slug.value(), excludedId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name, Long excludedId) {
        return repository.existsByName(name, excludedId);
    }

    @Override
    @Transactional
    public Tag create(Tag tag) {
        if (tag.id() != null) {
            throw new IllegalArgumentException("create expects a new tag, got id " + tag.id());
        }
        try {
            return TagPersistenceMapper.toDomain(repository.saveAndFlush(TagPersistenceMapper.toNewEntity(tag)));
        } catch (DataIntegrityViolationException e) {
            throw TaxonomyConstraints.translate(e);
        }
    }

    @Override
    @Transactional
    public Tag update(Tag tag) {
        TagEntity entity = repository.findById(tag.id())
            .orElseThrow(() -> new IllegalStateException("tag " + tag.id() + " does not exist"));
        entity.change(tag.name(), tag.slug().value());
        try {
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw TaxonomyConstraints.translate(e);
        }
        return TagPersistenceMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        try {
            repository.deleteById(id);
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException(ErrorCode.TERM_STILL_USED,
                "Ce tag est encore utilisé par des publications : retirez-le d'abord de ces publications.");
        }
    }
}
