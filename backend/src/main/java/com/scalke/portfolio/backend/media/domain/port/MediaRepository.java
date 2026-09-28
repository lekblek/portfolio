package com.scalke.portfolio.backend.media.domain.port;

import com.scalke.portfolio.backend.media.domain.model.Media;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port de persistance du catalogue. Ne contient que les méthodes utilisées (ADR 0001) : {@code create} par
 * {@code UploadMediaUseCase}, {@code findById} et {@code delete} par {@code DeleteMediaUseCase},
 * {@code findAllById} par la façade {@code MediaQueryService}.
 */
public interface MediaRepository {

    Media create(Media media);

    Optional<Media> findById(Long id);

    /**
     * Une seule requête, quel que soit le nombre d'identifiants ; les identifiants inconnus sont ignorés.
     */
    List<Media> findAllById(Collection<Long> ids);

    /**
     * Supprime l'entrée du catalogue, immédiatement (avant la fin de la transaction) pour que PostgreSQL
     * vérifie les références.
     *
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException
     *         {@code MEDIA_STILL_REFERENCED} si un contenu l'utilise encore (invariant 13)
     */
    void delete(Media media);
}
