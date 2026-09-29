package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.port.ProfileRepository;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper.ProfilePersistenceMapper;
import com.scalke.portfolio.backend.shared.error.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * Crée le profil ou remplace le profil existant, collections comprises (D-CY) ; écriture exécutée immédiatement.
     * Un média supprimé entre la vérification et l'écriture est refusé sur son champ.
     */
    @Override
    @Transactional
    public Profile save(Profile profile) {
        Optional<ProfileEntity> existing = repository.findById(ProfileEntity.SINGLETON_ID);
        ProfileEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.rename(profile.displayName(), profile.professionalTitle(), profile.shortBio());
            entity.clearCollections();
            // Suppressions envoyées avant les insertions : Hibernate ferait l'inverse (skill_unique_name).
            repository.flush();
            ProfilePersistenceMapper.fill(entity, profile);
        } else {
            entity = repository.save(ProfilePersistenceMapper.toEntity(profile));
        }
        try {
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
        return ProfilePersistenceMapper.toDomain(entity);
    }

    private static RuntimeException translate(DataIntegrityViolationException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage());
        if (message.contains("profile_avatar_media_fk")) {
            return new InvalidInputException("avatarMediaId", "Image inconnue.");
        }
        if (message.contains("profile_cv_media_fk")) {
            return new InvalidInputException("cvMediaId", "Document inconnu.");
        }
        return e;
    }
}
