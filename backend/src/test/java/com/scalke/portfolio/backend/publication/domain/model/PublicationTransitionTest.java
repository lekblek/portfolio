package com.scalke.portfolio.backend.publication.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static com.scalke.portfolio.backend.publication.domain.model.PublicationStatus.ARCHIVED;
import static com.scalke.portfolio.backend.publication.domain.model.PublicationStatus.DRAFT;
import static com.scalke.portfolio.backend.publication.domain.model.PublicationStatus.IN_REVIEW;
import static com.scalke.portfolio.backend.publication.domain.model.PublicationStatus.PUBLISHED;
import static com.scalke.portfolio.backend.publication.domain.model.PublicationStatus.SCHEDULED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cycle de vie éditorial : {@link Publication#transitionTo} et {@link Publication#effectiveStatus}
 * (invariant 26, D-AV, D-AW), mémoire de la première publication (D-AZ) et stabilité du slug
 * (invariant 6, D11, D-BC).
 */
class PublicationTransitionTest {

    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");
    private static final Instant PAST = NOW.minus(Duration.ofDays(10));
    private static final Instant FUTURE = NOW.plus(Duration.ofDays(3));
    private static final Instant LATER = NOW.plus(Duration.ofDays(7));
    private static final Instant CREATED = NOW.minus(Duration.ofDays(30));

    /**
     * La table complète, sur des publications dont le statut stocké est aussi le statut effectif
     * (planification future, publication passée).
     */
    @ParameterizedTest(name = "{0} → {1} : {2}")
    @CsvSource({
        "DRAFT,     DRAFT,     false", "DRAFT,     IN_REVIEW, true",  "DRAFT,     SCHEDULED, true",
        "DRAFT,     PUBLISHED, true",  "DRAFT,     ARCHIVED,  false",
        "IN_REVIEW, DRAFT,     true",  "IN_REVIEW, IN_REVIEW, false", "IN_REVIEW, SCHEDULED, true",
        "IN_REVIEW, PUBLISHED, true",  "IN_REVIEW, ARCHIVED,  false",
        "SCHEDULED, DRAFT,     true",  "SCHEDULED, IN_REVIEW, false", "SCHEDULED, SCHEDULED, true",
        "SCHEDULED, PUBLISHED, true",  "SCHEDULED, ARCHIVED,  false",
        "PUBLISHED, DRAFT,     false", "PUBLISHED, IN_REVIEW, false", "PUBLISHED, SCHEDULED, false",
        "PUBLISHED, PUBLISHED, false", "PUBLISHED, ARCHIVED,  true",
        "ARCHIVED,  DRAFT,     true",  "ARCHIVED,  IN_REVIEW, false", "ARCHIVED,  SCHEDULED, false",
        "ARCHIVED,  PUBLISHED, true",  "ARCHIVED,  ARCHIVED,  false",
    })
    void follows_the_transition_table(PublicationStatus from, PublicationStatus to, boolean allowed) {
        Publication publication = in(from);
        Instant scheduledAt = to == SCHEDULED ? LATER : null;

        if (allowed) {
            assertThat(publication.transitionTo(to, scheduledAt, NOW).status()).isEqualTo(to);
        } else {
            assertRefused(() -> publication.transitionTo(to, scheduledAt, NOW));
        }
    }

    @Test
    void scheduling_makes_the_future_date_the_publication_date() {
        Publication scheduled = in(DRAFT).transitionTo(SCHEDULED, LATER, NOW);

        assertThat(scheduled.publishedAt()).isEqualTo(LATER);
        assertThat(scheduled.isVisibleAt(NOW)).isFalse();
        assertThat(scheduled.isVisibleAt(LATER)).isTrue();
    }

    @Test
    void scheduling_requires_a_strictly_future_date() {
        assertRefused(() -> in(DRAFT).transitionTo(SCHEDULED, null, NOW));
        assertRefused(() -> in(DRAFT).transitionTo(SCHEDULED, NOW, NOW));
        assertRefused(() -> in(DRAFT).transitionTo(SCHEDULED, PAST, NOW));
    }

    @Test
    void a_date_is_refused_for_any_transition_but_scheduling() {
        assertRefused(() -> in(DRAFT).transitionTo(PUBLISHED, LATER, NOW));
    }

    @Test
    void publishing_dates_a_new_publication_now() {
        assertThat(in(DRAFT).transitionTo(PUBLISHED, null, NOW).publishedAt()).isEqualTo(NOW);
    }

    @Test
    void publishing_a_planned_publication_early_dates_it_now() {
        assertThat(in(SCHEDULED).transitionTo(PUBLISHED, null, NOW).publishedAt()).isEqualTo(NOW);
    }

    @Test
    void restoring_an_archive_keeps_its_original_publication_date() {
        assertThat(in(ARCHIVED).transitionTo(PUBLISHED, null, NOW).publishedAt()).isEqualTo(PAST);
    }

    @Test
    void cancelling_a_planned_publication_clears_its_date() {
        assertThat(in(SCHEDULED).transitionTo(DRAFT, null, NOW).publishedAt()).isNull();
    }

    @Test
    void archiving_and_reworking_keep_the_publication_date() {
        Publication archived = in(PUBLISHED).transitionTo(ARCHIVED, null, NOW);

        assertThat(archived.publishedAt()).isEqualTo(PAST);
        assertThat(archived.transitionTo(DRAFT, null, NOW).publishedAt()).isEqualTo(PAST);
    }

    /**
     * Une planification échue est publiée (D03) : elle ne peut plus qu'être archivée.
     */
    @Test
    void a_planned_publication_whose_date_has_passed_behaves_as_published() {
        Publication due = publication(SCHEDULED, PAST);

        assertThat(due.effectiveStatus(NOW)).isEqualTo(PUBLISHED);
        assertThat(due.isVisibleAt(NOW)).isTrue();
        assertThat(due.transitionTo(ARCHIVED, null, NOW).status()).isEqualTo(ARCHIVED);
        assertRefused(() -> due.transitionTo(SCHEDULED, LATER, NOW));
        assertRefused(() -> due.transitionTo(DRAFT, null, NOW));
    }

    @Test
    void the_first_publication_date_follows_the_publication_date_until_it_has_passed() {
        Publication scheduled = in(DRAFT).transitionTo(SCHEDULED, LATER, NOW);
        Publication rescheduled = scheduled.transitionTo(SCHEDULED, FUTURE, NOW);

        assertThat(scheduled.firstPublishedAt()).isEqualTo(LATER);
        assertThat(rescheduled.firstPublishedAt()).isEqualTo(FUTURE);
        assertThat(rescheduled.transitionTo(PUBLISHED, null, NOW).firstPublishedAt()).isEqualTo(NOW);
        assertThat(rescheduled.transitionTo(DRAFT, null, NOW).firstPublishedAt()).isNull();
        assertThat(rescheduled.hasBeenPublic(NOW)).isFalse();
    }

    /**
     * Audit A01 : aucune suite de transitions n'efface le fait d'avoir été publique. Avant D-AZ, cette
     * séquence effaçait {@code publishedAt} et la publication paraissait n'avoir jamais été publiée.
     */
    @Test
    void no_sequence_of_transitions_forgets_a_past_publication() {
        Publication reworked = in(PUBLISHED)
            .transitionTo(ARCHIVED, null, NOW)
            .transitionTo(DRAFT, null, NOW)
            .transitionTo(SCHEDULED, LATER, NOW)
            .transitionTo(DRAFT, null, NOW);

        assertThat(reworked.status()).isEqualTo(DRAFT);
        assertThat(reworked.publishedAt()).isNull();
        assertThat(reworked.firstPublishedAt()).isEqualTo(PAST);
        assertThat(reworked.hasBeenPublic(NOW)).isTrue();
    }

    @Test
    void republishing_keeps_the_first_publication_date() {
        Publication rescheduled = in(PUBLISHED)
            .transitionTo(ARCHIVED, null, NOW)
            .transitionTo(DRAFT, null, NOW)
            .transitionTo(SCHEDULED, LATER, NOW);

        assertThat(rescheduled.publishedAt()).isEqualTo(LATER);
        assertThat(rescheduled.firstPublishedAt()).isEqualTo(PAST);
    }

    /**
     * Une planification échue a été publique sans qu'aucune écriture ne l'enregistre : c'est la date,
     * pas le statut stocké, qui fait foi.
     */
    @Test
    void a_planned_publication_whose_date_has_passed_has_been_public() {
        assertThat(publication(SCHEDULED, PAST).hasBeenPublic(NOW)).isTrue();
        assertThat(in(SCHEDULED).hasBeenPublic(NOW)).isFalse();
        assertThat(in(DRAFT).hasBeenPublic(NOW)).isFalse();
    }

    @Test
    void the_slug_can_change_until_the_first_publication() {
        Publication renamed = in(DRAFT).changeSlug(Slug.of("nouveau-titre"), NOW);

        assertThat(renamed.slug()).isEqualTo(Slug.of("nouveau-titre"));
        assertThat(renamed.updatedAt()).isEqualTo(NOW);
        assertThat(in(IN_REVIEW).changeSlug(Slug.of("nouveau-titre"), NOW).slug()).isEqualTo(Slug.of("nouveau-titre"));
        assertThat(in(SCHEDULED).changeSlug(Slug.of("nouveau-titre"), NOW).slug()).isEqualTo(Slug.of("nouveau-titre"));
    }

    /**
     * Invariant 6 (D11) : les liens publiés restent valides, même après archivage, retour en brouillon
     * ou échéance d'une planification.
     */
    @Test
    void the_slug_is_locked_once_the_publication_has_been_public() {
        Publication reworked = in(PUBLISHED).transitionTo(ARCHIVED, null, NOW).transitionTo(DRAFT, null, NOW);

        assertSlugLocked(() -> in(PUBLISHED).changeSlug(Slug.of("nouveau-titre"), NOW));
        assertSlugLocked(() -> in(ARCHIVED).changeSlug(Slug.of("nouveau-titre"), NOW));
        assertSlugLocked(() -> reworked.changeSlug(Slug.of("nouveau-titre"), NOW));
        assertSlugLocked(() -> publication(SCHEDULED, PAST).changeSlug(Slug.of("nouveau-titre"), NOW));
    }

    @Test
    void keeping_the_same_slug_is_always_allowed() {
        Publication published = in(PUBLISHED);

        assertThat(published.changeSlug(Slug.of("titre"), NOW)).isSameAs(published);
    }

    @Test
    void rejects_a_first_publication_date_after_the_publication_date() {
        assertThatThrownBy(() -> new Publication(1L, PublicationType.ARTICLE, "Titre", Slug.of("titre"), "Résumé",
            "Contenu", PUBLISHED, PAST, NOW, false, null, Set.of(), null, null, CREATED, CREATED, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void a_publication_date_requires_a_first_publication_date() {
        assertThatThrownBy(() -> new Publication(1L, PublicationType.ARTICLE, "Titre", Slug.of("titre"), "Résumé",
            "Contenu", ARCHIVED, PAST, null, false, null, Set.of(), null, null, CREATED, CREATED, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void a_transition_only_changes_the_status_the_date_and_updatedAt() {
        Publication draft = in(DRAFT);

        Publication review = draft.transitionTo(IN_REVIEW, null, NOW);

        assertThat(review.updatedAt()).isEqualTo(NOW);
        assertThat(review)
            .usingRecursiveComparison()
            .ignoringFields("status", "updatedAt")
            .isEqualTo(draft);
    }

    private static Publication in(PublicationStatus status) {
        return switch (status) {
            case DRAFT, IN_REVIEW -> publication(status, null);
            case SCHEDULED -> publication(status, FUTURE);
            case PUBLISHED, ARCHIVED -> publication(status, PAST);
        };
    }

    private static Publication publication(PublicationStatus status, Instant publishedAt) {
        return new Publication(1L, PublicationType.ARTICLE, "Titre", Slug.of("titre"), "Résumé", "Contenu",
            status, publishedAt, publishedAt, false, 3L, Set.of(4L), "SEO", null, CREATED, CREATED, null);
    }

    private static void assertRefused(ThrowingCallable transition) {
        assertThatThrownBy(transition)
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_PUBLICATION_TRANSITION));
    }

    private static void assertSlugLocked(ThrowingCallable change) {
        assertThatThrownBy(change)
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.SLUG_LOCKED));
    }
}
