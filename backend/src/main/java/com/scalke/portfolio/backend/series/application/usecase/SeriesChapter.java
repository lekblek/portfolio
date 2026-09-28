package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;

/**
 * Chapitre public d'une série : un article visible et sa position publique, rang à partir de 1 parmi les
 * articles visibles de la série (D-BG).
 */
public record SeriesChapter(int position, Publication publication) {
}
