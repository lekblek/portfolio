package com.scalke.portfolio.backend.taxonomy.application.usecase;

import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.function.Predicate;

/**
 * Choix du slug d'un terme (D-BD, D-CS) : celui saisi, sinon celui du terme modifié, sinon un slug généré depuis le
 * nom ; puis le premier libre ({@code -2}, {@code -3}…), jamais plus long que la colonne du vocabulaire.
 */
final class TermSlugs {

    private TermSlugs() {
    }

    static Slug choose(TermDraft draft, Slug current, int maxLength, Predicate<Slug> isTaken) {
        Slug wanted = draft.slug() != null ? Slug.of(draft.slug())
            : current != null ? current
            : Slug.fromText(draft.name(), maxLength);
        return wanted.firstAvailable(isTaken, maxLength);
    }
}
