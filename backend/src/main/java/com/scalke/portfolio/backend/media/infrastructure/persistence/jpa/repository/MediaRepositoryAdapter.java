package com.scalke.portfolio.backend.media.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.media.infrastructure.persistence.jpa.mapper.MediaPersistenceMapper;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    @Override
    @Transactional(readOnly = true)
    public Optional<Media> findById(Long id) {
        return repository.findById(id).map(MediaPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Media> findAllById(Collection<Long> ids) {
        return repository.findAllById(ids).stream().map(MediaPersistenceMapper::toDomain).toList();
    }

    /**
     * Les références sont des clés étrangères {@code ON DELETE RESTRICT} des modules qui affichent le média
     * (D-BV) : le catalogue n'a pas à les connaître, PostgreSQL refuse la suppression.
     */
    @Override
    @Transactional
    public void delete(Media media) {
        try {
            repository.deleteById(media.id());
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException(ErrorCode.MEDIA_STILL_REFERENCED,
                "Ce média est encore utilisé : retirez-le d'abord des contenus qui l'affichent.");
        }
    }
}
