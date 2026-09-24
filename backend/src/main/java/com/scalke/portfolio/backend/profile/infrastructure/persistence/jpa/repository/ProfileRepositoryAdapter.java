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
     * Lecture en nombre constant de requêtes (D-O) : une pour le profil, puis une par collection,
     * déclenchée par le mapper lorsqu'il parcourt la collection. Le mapping a lieu dans la
     * transaction ; le résultat est un record, sans aucun chargement paresseux possible ensuite.
     * <p>
     * Pas de {@code join fetch} : joindre plusieurs collections produirait un produit cartésien
     * ({@code MultipleBagFetchException}) ; en joindre une seule n'économiserait qu'une requête.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Profile> find() {
        return repository.findById(ProfileEntity.SINGLETON_ID)
            .map(ProfilePersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public Profile save(Profile profile) {
        ProfileEntity saved = repository.save(ProfilePersistenceMapper.toEntity(profile));
        return ProfilePersistenceMapper.toDomain(saved);
    }
}
