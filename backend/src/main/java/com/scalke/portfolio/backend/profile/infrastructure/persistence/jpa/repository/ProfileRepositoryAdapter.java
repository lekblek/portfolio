package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.port.ProfileRepository;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper.ProfilePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProfileRepositoryAdapter implements ProfileRepository {

    private final SpringDataProfileRepository repository;

    @Override
    public Optional<Profile> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Profile> find() {
        return repository.findWithLinks()
            .map(ProfilePersistenceMapper::toDomain);
    }

    @Override
    public Profile save(Profile profile) {
        return null;
    }

    @Override
    public void delete(Profile profile) {

    }

    @Override
    public boolean exists() {
        return false;
    }
}
