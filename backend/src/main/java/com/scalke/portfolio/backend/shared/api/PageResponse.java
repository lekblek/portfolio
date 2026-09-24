package com.scalke.portfolio.backend.shared.api;

import com.scalke.portfolio.backend.shared.domain.model.PageResult;

import java.util.List;

/**
 * Contrat HTTP unique des collections paginées ({@code docs/05-conventions-api.md} §14).
 * Construit à partir du {@link PageResult} renvoyé par un cas d'usage : jamais à partir d'un
 * {@code Page} de Spring Data, qu'un port du domaine ne peut pas renvoyer (D-V).
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {

    public static <T> PageResponse<T> from(PageResult<T> page) {
        return new PageResponse<>(
            page.content(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages(),
            page.isFirst(),
            page.isLast());
    }
}
