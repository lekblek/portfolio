package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.port.ProfileRepository;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper.ProfilePersistenceMapper;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProfileRepositoryAdapter implements ProfileRepository {

    private final ProfileJpaRepository repository;

    @Override
    public Optional<Profile> findById(Long id) {
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Profile> find() {
        return repository.findWithLinks()
            .map(profileEntity -> {
                repository.loadSkills(profileEntity.getId());
                return ProfilePersistenceMapper.toDomain(profileEntity);
            });
    }

    @Override
    public Profile save(Profile profile) {
        ProfileEntity entity = ProfilePersistenceMapper.toEntity(profile);

        ProfileEntity savedEntity = repository.save(entity);

        return ProfilePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public void delete(Profile profile) {

    }

    @Override
    public boolean exists() {
        return false;
    }
}
