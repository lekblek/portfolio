package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.port.ProfileRepository;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper.ProfilePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptateur JPA du port {@link ProfileRepository}.
 * <p>
 * Chaque méthode est transactionnelle pour rester correcte lorsqu'elle est appelée hors cas d'usage
 * (seed de développement) ; appelée depuis un cas d'usage, elle rejoint sa transaction.
 */
@Repository
@RequiredArgsConstructor
public class ProfileRepositoryAdapter implements ProfileRepository {

    private final ProfileJpaRepository repository;

    /**
     * Une requête par collection, dans la même transaction : la seconde requête initialise
     * la collection de l'entité déjà présente dans le contexte de persistance.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Profile> find() {
        return repository.findWithLinks()
            .map(profile -> {
                repository.loadSkills(profile.getId());
                return ProfilePersistenceMapper.toDomain(profile);
            });
    }

    @Override
    @Transactional
    public Profile save(Profile profile) {
        ProfileEntity saved = repository.save(ProfilePersistenceMapper.toEntity(profile));
        return ProfilePersistenceMapper.toDomain(saved);
    }
}
