package com.scalke.portfolio.backend.shared.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Page de résultats renvoyée par un port, sans dépendance à Spring Data (D-V).
 * {@code content} est immuable et déjà trié ; les métadonnées suivent la sémantique de Spring Data.
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public PageResult {
        content = List.copyOf(content);
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
    }

    /**
     * Page vide pour une demande dont on sait déjà qu'elle ne trouvera rien (filtre sur un terme inconnu).
     */
    public static <T> PageResult<T> empty(PageQuery query) {
        return new PageResult<>(List.of(), query.page(), query.size(), 0);
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / size);
    }

    public boolean isFirst() {
        return page == 0;
    }

    public boolean isLast() {
        return page + 1 >= totalPages();
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(content.stream().<R>map(mapper).toList(), page, size, totalElements);
    }
}
