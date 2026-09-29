package com.scalke.portfolio.backend.series.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Chapitres d'une série, dans l'ordre de lecture (D-CV) : la liste remplace l'ancienne ; vide, la série n'en a plus.
 */
public record ChangeSeriesChaptersRequest(@NotNull List<@NotNull Long> publicationIds) {
}
