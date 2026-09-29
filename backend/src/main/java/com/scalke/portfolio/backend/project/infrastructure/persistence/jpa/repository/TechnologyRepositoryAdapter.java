package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.TechnologyEntity;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.mapper.TechnologyPersistenceMapper;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.infrastructure.persistence.ViolatedConstraint;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adaptateur JPA du port {@link TechnologyRepository}. Écritures envoyées immédiatement : un refus de PostgreSQL
 * survenu entre la vérification et l'écriture est traduit d'après la contrainte (D-CW, comme D-CS).
 */
@Repository
@RequiredArgsConstructor
public class TechnologyRepositoryAdapter implements TechnologyRepository {

    private final TechnologyJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<Technology> findAll() {
        return TechnologyPersistenceMapper.map(repository.findAllInDisplayOrder());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Technology> findById(Long id) {
        return repository.findById(id).map(TechnologyPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Technology> findAllById(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : TechnologyPersistenceMapper.map(repository.findByIdIn(ids));
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
    public Technology create(Technology technology) {
        if (technology.id() != null) {
            throw new IllegalArgumentException("create expects a new technology, got id " + technology.id());
        }
        try {
            return TechnologyPersistenceMapper.toDomain(
                repository.saveAndFlush(TechnologyPersistenceMapper.toNewEntity(technology)));
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    @Override
    @Transactional
    public Technology update(Technology technology) {
        TechnologyEntity entity = repository.findById(technology.id())
            .orElseThrow(() -> new IllegalStateException("technology " + technology.id() + " does not exist"));
        entity.change(technology.name(), technology.slug().value(), technology.displayOrder());
        try {
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
        return TechnologyPersistenceMapper.toDomain(entity);
    }

    /**
     * Invariant 22 : une technologie utilisée par un projet n'est pas supprimée ({@code ON DELETE RESTRICT}).
     */
    @Override
    @Transactional
    public void delete(Long id) {
        try {
            repository.deleteById(id);
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException(ErrorCode.TERM_STILL_USED,
                "Cette technologie est encore utilisée par des projets : retirez-la d'abord de ces projets.");
        }
    }

    private static RuntimeException translate(DataIntegrityViolationException e) {
        return switch (ViolatedConstraint.of(e).orElse("")) {
            case "technology_slug_unique" ->
                new BusinessRuleViolationException(ErrorCode.SLUG_ALREADY_USED, "Ce slug est déjà utilisé.");
            case "technology_name_unique_idx" ->
                new BusinessRuleViolationException(ErrorCode.NAME_ALREADY_USED, "Ce nom est déjà utilisé.");
            default -> e;
        };
    }
}
