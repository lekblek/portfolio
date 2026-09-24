package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Accès Spring Data au profil. Les collections restent paresseuses : elles sont chargées
 * une par une, dans la transaction de l'adaptateur (D-O).
 */
public interface ProfileJpaRepository extends Repository<ProfileEntity, Long> {

    Optional<ProfileEntity> findById(Long id);

    ProfileEntity save(ProfileEntity entity);
}
