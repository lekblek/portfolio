package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.mapper.TechnologyPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptateur JPA du port {@link TechnologyRepository}.
 */
@Repository
@RequiredArgsConstructor
public class TechnologyRepositoryAdapter implements TechnologyRepository {

    private final TechnologyJpaRepository repository;

    @Override
    @Transactional
    public Technology create(Technology technology) {
        if (technology.id() != null) {
            throw new IllegalArgumentException("create expects a new technology, got id " + technology.id());
        }
        return TechnologyPersistenceMapper.toDomain(
            repository.save(TechnologyPersistenceMapper.toNewEntity(technology)));
    }
}
