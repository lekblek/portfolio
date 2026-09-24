package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;

import java.util.List;

/**
 * Publication visible et ses termes de classement résolus, telle que la lisent les cas d'usage publics.
 * {@code category} vaut {@code null} si la publication n'est pas classée ; {@code tags} est trié
 * par nom ({@link Tag#BY_NAME}).
 */
public record VisiblePublication(Publication publication, Category category, List<Tag> tags) {

    public VisiblePublication {
        tags = List.copyOf(tags);
    }
}
