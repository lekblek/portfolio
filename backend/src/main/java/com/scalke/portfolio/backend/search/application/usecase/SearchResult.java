package com.scalke.portfolio.backend.search.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.time.Instant;
import java.util.Objects;

/**
 * Contenu public trouvé par la recherche (D-CD), avec ce qu'il faut pour l'afficher et le lier : {@code summary}
 * est le résumé d'une publication ou la description courte d'un projet ; {@code publishedAt} est la date de
 * publication d'un article ou d'une news, {@code null} pour un projet.
 */
public record SearchResult(Type type, String title, Slug slug, String summary, Instant publishedAt) {

    public enum Type {
        ARTICLE,
        NEWS,
        PROJECT
    }

    public SearchResult {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(slug, "slug");
    }

    static SearchResult of(Publication publication) {
        Type type = switch (publication.type()) {
            case ARTICLE -> Type.ARTICLE;
            case NEWS -> Type.NEWS;
        };
        return new SearchResult(type, publication.title(), publication.slug(), publication.summary(),
            publication.publishedAt());
    }

    static SearchResult of(Project project) {
        return new SearchResult(Type.PROJECT, project.title(), project.slug(), project.shortDescription(), null);
    }
}
