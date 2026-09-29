package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Publication vue par l'administration (D-CU), lue à un instant donné : statut observable (une planification échue
 * est publiée, D03) et slug verrouillé ou non (première publication passée, D-BC).
 */
public record AdminPublication(Publication publication, PublicationStatus status, boolean slugLocked) {

    public AdminPublication {
        Objects.requireNonNull(publication, "publication");
        Objects.requireNonNull(status, "status");
    }

    static AdminPublication at(Publication publication, Instant now) {
        return new AdminPublication(publication, publication.effectiveStatus(now), publication.hasBeenPublic(now));
    }
}
