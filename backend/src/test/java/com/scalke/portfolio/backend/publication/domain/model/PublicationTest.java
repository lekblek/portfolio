package com.scalke.portfolio.backend.publication.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicationTest {

    private static final Instant AT = Instant.parse("2026-06-01T09:00:00Z");

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, names = {"SCHEDULED", "PUBLISHED"})
    void requires_a_publication_date_once_scheduled_or_published(PublicationStatus status) {
        assertThatThrownBy(() -> publication(status, null, "Contenu"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, names = {"DRAFT", "IN_REVIEW", "ARCHIVED"})
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

    private static Publication publication(PublicationStatus status, Instant publishedAt, String content) {
        return new Publication(null, PublicationType.ARTICLE, "Titre", "titre", "Résumé", content,
            status, publishedAt, false, null, null, AT, AT);
    }
}
