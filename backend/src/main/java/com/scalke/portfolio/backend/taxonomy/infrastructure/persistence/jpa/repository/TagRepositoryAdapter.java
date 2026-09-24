package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.mapper.TagPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adaptateur JPA du port {@link TagRepository}.
 */
@Repository
@RequiredArgsConstructor
public class TagRepositoryAdapter implements TagRepository {

    private final TagJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> findBySlug(String slug) {
        return repository.findBySlug(slug).map(TagPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> findAllById(Collection<Long> ids) {
        return repository.findByIdIn(ids).stream().map(TagPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Tag create(Tag tag) {
        if (tag.id() != null) {
            throw new IllegalArgumentException("create expects a new tag, got id " + tag.id());
        }
        return TagPersistenceMapper.toDomain(repository.save(TagPersistenceMapper.toNewEntity(tag)));
    }
}
