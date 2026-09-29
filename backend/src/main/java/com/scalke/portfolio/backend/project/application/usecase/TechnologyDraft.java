package com.scalke.portfolio.backend.project.application.usecase;

/**
 * Saisie d'une technologie par l'administrateur (D-CW) : {@code slug} facultatif (généré depuis le nom à la création,
 * conservé à la modification), ordre d'affichage (0 s'il est absent). Espaces de début et de fin retirés.
 */
public record TechnologyDraft(String name, String slug, int displayOrder) {

    public TechnologyDraft {
        name = name == null ? null : name.strip();
        slug = slug == null || slug.isBlank() ? null : slug.strip();
    }
}
