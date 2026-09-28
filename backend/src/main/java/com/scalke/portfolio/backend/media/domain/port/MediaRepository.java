package com.scalke.portfolio.backend.media.domain.port;

import com.scalke.portfolio.backend.media.domain.model.Media;

/**
 * Port de persistance du catalogue. Ne contient que les méthodes utilisées (ADR 0001) : {@code create}
 * par {@code UploadMediaUseCase}.
 */
public interface MediaRepository {

    Media create(Media media);
}
