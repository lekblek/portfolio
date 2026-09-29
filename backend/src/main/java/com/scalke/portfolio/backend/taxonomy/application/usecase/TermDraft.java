package com.scalke.portfolio.backend.taxonomy.application.usecase;

/**
 * Saisie d'un terme par l'administrateur (D-CS) : {@code slug} facultatif (généré depuis le nom à la création,
 * conservé à la modification), {@code description} facultative et réservée aux catégories. Les espaces de début et
 * de fin sont retirés ; une description vide vaut {@code null}.
 */
public record TermDraft(String name, String slug, String description) {

    public TermDraft {
        name = name == null ? null : name.strip();
        slug = slug == null || slug.isBlank() ? null : slug.strip();
        description = description == null || description.isBlank() ? null : description.strip();
    }
}
