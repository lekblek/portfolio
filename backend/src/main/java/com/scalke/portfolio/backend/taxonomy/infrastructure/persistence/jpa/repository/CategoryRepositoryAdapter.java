package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.port.CategoryRepository;
import com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.mapper.CategoryPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adaptateur JPA du port {@link CategoryRepository}.
 */
@Repository
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findBySlug(String slug) {
        return repository.findBySlug(slug).map(CategoryPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllById(Collection<Long> ids) {
        return repository.findByIdIn(ids).stream().map(CategoryPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Category create(Category category) {
        if (category.id() != null) {
            throw new IllegalArgumentException("create expects a new category, got id " + category.id());
        }
        return CategoryPersistenceMapper.toDomain(repository.save(CategoryPersistenceMapper.toNewEntity(category)));
    }
}
