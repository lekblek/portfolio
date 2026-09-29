package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.port.CategoryRepository;
import com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.entity.CategoryEntity;
import com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.mapper.CategoryPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adaptateur JPA du port {@link CategoryRepository}. Les écritures sont envoyées immédiatement ({@code flush}) pour
 * traduire un refus de PostgreSQL en erreur métier (D-CS) : nom ou slug pris entre la vérification et l'écriture
 * ({@code category_name_unique_idx}, {@code category_slug_unique}), category encore utilisée ({@code publication_category_fk}).
 */
@Repository
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findBySlug(Slug slug) {
        return repository.findBySlug(slug.value()).map(CategoryPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllById(Collection<Long> ids) {
        return repository.findByIdIn(ids).stream().map(CategoryPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return repository.findAllByName().stream().map(CategoryPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(Long id) {
        return repository.findById(id).map(CategoryPersistenceMapper::toDomain);
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
    public Category create(Category category) {
        if (category.id() != null) {
            throw new IllegalArgumentException("create expects a new category, got id " + category.id());
        }
        try {
            return CategoryPersistenceMapper.toDomain(repository.saveAndFlush(CategoryPersistenceMapper.toNewEntity(category)));
        } catch (DataIntegrityViolationException e) {
            throw TaxonomyConstraints.translate(e);
        }
    }

    @Override
    @Transactional
    public Category update(Category category) {
        CategoryEntity entity = repository.findById(category.id())
            .orElseThrow(() -> new IllegalStateException("category " + category.id() + " does not exist"));
        entity.change(category.name(), category.slug().value(), category.description());
        try {
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw TaxonomyConstraints.translate(e);
        }
        return CategoryPersistenceMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        try {
            repository.deleteById(id);
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException(ErrorCode.TERM_STILL_USED,
                "Cette catégorie est encore utilisée par des publications : retirez-la d'abord de ces publications.");
        }
    }
}
