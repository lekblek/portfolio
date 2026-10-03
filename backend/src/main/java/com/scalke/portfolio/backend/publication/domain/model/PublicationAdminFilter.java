package com.scalke.portfolio.backend.publication.domain.model;

/**
 * Critères de la liste d'administration des publications (F28) : type et statut <strong>observable</strong> (une
 * publication planifiée dont la date est passée est {@code PUBLISHED}, une planifiée à venir {@code SCHEDULED}).
 * {@code null} : aucun critère. Les deux se combinent.
 */
public record PublicationAdminFilter(PublicationType type, PublicationStatus status) {

    public boolean hasType() {
        return type != null;
    }

    public boolean hasStatus() {
        return status != null;
    }
}
