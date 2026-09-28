package com.scalke.portfolio.backend.media.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.media.infrastructure.persistence.jpa.mapper.MediaPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptateur JPA du port {@link MediaRepository}. Identifiant attribué par PostgreSQL : l'insertion est
 * exécutée immédiatement, avant l'écriture du fichier (D-BU).
 */
@Repository
@RequiredArgsConstructor
public class MediaRepositoryAdapter implements MediaRepository {

    private final MediaJpaRepository repository;

    @Override
    @Transactional
    public Media create(Media media) {
        if (media.id() != null) {
            throw new IllegalArgumentException("create expects a new media, got id " + media.id());
        }
        return MediaPersistenceMapper.toDomain(repository.save(MediaPersistenceMapper.toNewEntity(media)));
    }
}
