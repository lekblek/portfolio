package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.media.application.query.PublicImage;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;

import java.util.List;

/**
 * Publication visible et ses termes de classement résolus, telle que la lisent les cas d'usage publics.
 * {@code category} vaut {@code null} si la publication n'est pas classée ; {@code tags} est trié
 * par nom ({@link Tag#BY_NAME}) ; {@code cover} est la couverture sous forme publique, {@code null} si aucune (D-BY).
 */
public record VisiblePublication(Publication publication, Category category, List<Tag> tags, PublicImage cover) {

    public VisiblePublication {
        tags = List.copyOf(tags);
    }
}
