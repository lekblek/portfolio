package com.scalke.portfolio.backend.publication.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicationTest {

    private static final Instant AT = Instant.parse("2026-06-01T09:00:00Z");

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, names = {"SCHEDULED", "PUBLISHED", "ARCHIVED"})
    void requires_a_publication_date_once_scheduled_published_or_archived(PublicationStatus status) {
        assertThatThrownBy(() -> publication(status, null, "Contenu"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, names = {"DRAFT", "IN_REVIEW"})
    void accepts_no_publication_date_otherwise(PublicationStatus status) {
        assertThat(publication(status, null, "Contenu").publishedAt()).isNull();
    }

    @Test
    void a_short_text_takes_at_least_one_minute_to_read() {
        assertThat(publication(PublicationStatus.DRAFT, null, "").readingTimeMinutes()).isEqualTo(1);
    }

    @Test
    void reading_time_is_rounded_up_to_the_next_minute() {
        String twoHundredWords = "mot ".repeat(Publication.WORDS_PER_MINUTE);

        assertThat(publication(PublicationStatus.DRAFT, null, twoHundredWords).readingTimeMinutes()).isEqualTo(1);
        assertThat(publication(PublicationStatus.DRAFT, null, twoHundredWords + "encore").readingTimeMinutes())
            .isEqualTo(2);
    }

    /**
     * « - l'API full-stack » compte 2 mots : le tiret de liste n'est pas un mot, « l'API » et
     * « full-stack » en sont un chacun. 100 répétitions = 200 mots = 1 minute ; un mot de plus = 2.
     */
    @Test
    void markdown_symbols_are_not_words_and_joined_words_count_once() {
        String twoHundredWords = "## - l'API full-stack **\n".repeat(100);

        assertThat(publication(PublicationStatus.DRAFT, null, twoHundredWords).readingTimeMinutes()).isEqualTo(1);
        assertThat(publication(PublicationStatus.DRAFT, null, twoHundredWords + "encore").readingTimeMinutes())
            .isEqualTo(2);
    }

    @Test
    void keeps_an_immutable_copy_of_its_tags() {
        Set<Long> tags = new HashSet<>(Set.of(1L, 2L));
        Publication publication = new Publication(null, PublicationType.ARTICLE, "Titre", Slug.of("titre"), "Résumé", "Contenu",
            PublicationStatus.DRAFT, null, null, false, 7L, tags, null, null, AT, AT, null);

        tags.add(3L);

        assertThat(publication.tagIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThatThrownBy(() -> publication.tagIds().add(4L)).isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * D-CU : bornes de la saisie, doublées par PostgreSQL.
     */
    @Test
    void bounds_the_text_fields() {
        assertThatThrownBy(() -> withText(" ", "Résumé", "Contenu", null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withText("t".repeat(161), "Résumé", "Contenu", null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withText("Titre", "r".repeat(501), "Contenu", null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withText("Titre", "Résumé", "c".repeat(100_001), null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withText("Titre", "Résumé", "Contenu", "s".repeat(121), null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withText("Titre", "Résumé", "Contenu", null, "d".repeat(301)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(withText("t".repeat(160), "r".repeat(500), "c".repeat(100_000), "s".repeat(120), "d".repeat(300)))
            .isNotNull();
    }

    @Test
    void a_new_draft_has_no_publication_date() {
        PublicationContent content = content(" Mon article ", Set.of(3L));

        Publication draft = Publication.newDraft(PublicationType.NEWS, Slug.of("mon-article"), content, AT);

        assertThat(draft.status()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(draft.type()).isEqualTo(PublicationType.NEWS);
        assertThat(draft.title()).isEqualTo("Mon article");
        assertThat(draft.publishedAt()).isNull();
        assertThat(draft.firstPublishedAt()).isNull();
        assertThat(draft.tagIds()).containsExactly(3L);
        assertThat(draft.createdAt()).isEqualTo(AT);
        assertThat(draft.updatedAt()).isEqualTo(AT);
    }

    /**
     * D-CU : la modification remplace la saisie, jamais le type, le statut ni ses dates.
     */
    @Test
    void editing_keeps_the_type_the_status_and_its_dates() {
        Instant later = AT.plusSeconds(3_600);
        Publication published = publication(PublicationStatus.PUBLISHED, AT, "Contenu");

        Publication edited = published.edit(published.slug(), content("Nouveau titre", Set.of(5L)), later);

        assertThat(edited.title()).isEqualTo("Nouveau titre");
        assertThat(edited.tagIds()).containsExactly(5L);
        assertThat(edited.coverMediaId()).isEqualTo(9L);
        assertThat(edited.seoTitle()).isNull();
        assertThat(edited.type()).isEqualTo(published.type());
        assertThat(edited.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(edited.publishedAt()).isEqualTo(AT);
        assertThat(edited.firstPublishedAt()).isEqualTo(AT);
        assertThat(edited.createdAt()).isEqualTo(AT);
        assertThat(edited.updatedAt()).isEqualTo(later);
    }

    /**
     * D-BC : la modification passe par la règle du slug.
     */
    @Test
    void editing_cannot_change_the_slug_of_a_publication_already_public() {
        Publication published = publication(PublicationStatus.PUBLISHED, AT, "Contenu");

        assertThatThrownBy(() -> published.edit(Slug.of("autre"), content("Titre", Set.of()), AT.plusSeconds(1)))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.SLUG_LOCKED));
        assertThat(publication(PublicationStatus.DRAFT, null, "Contenu")
            .edit(Slug.of("autre"), content("Titre", Set.of()), AT).slug()).isEqualTo(Slug.of("autre"));
    }

    @Test
    void the_content_is_normalized() {
        PublicationContent content = new PublicationContent(" Titre ", " Résumé ", "Contenu", false, null, null, null,
            "  ", " Description ");

        assertThat(content.title()).isEqualTo("Titre");
        assertThat(content.summary()).isEqualTo("Résumé");
        assertThat(content.tagIds()).isEmpty();
        assertThat(content.seoTitle()).isNull();
        assertThat(content.seoDescription()).isEqualTo("Description");
    }

    private static PublicationContent content(String title, Set<Long> tagIds) {
        return new PublicationContent(title, "Résumé", "Contenu", true, null, tagIds, 9L, " ", null);
    }

    private static Publication withText(String title, String summary, String content, String seoTitle,
                                        String seoDescription) {
        return new Publication(null, PublicationType.ARTICLE, title, Slug.of("titre"), summary, content,
            PublicationStatus.DRAFT, null, null, false, null, Set.of(), seoTitle, seoDescription, AT, AT, null);
    }

    private static Publication publication(PublicationStatus status, Instant publishedAt, String content) {
        return new Publication(null, PublicationType.ARTICLE, "Titre", Slug.of("titre"), "Résumé", content,
            status, publishedAt, publishedAt, false, null, Set.of(), null, null, AT, AT, null);
    }
}
