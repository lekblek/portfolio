package com.scalke.portfolio.backend.shared.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlugTest {

    // ---- format (invariant 5, mêmes règles que les CHECK PostgreSQL) ----

    @ParameterizedTest
    @ValueSource(strings = {"java", "spring-boot", "spring-boot-4", "a1-b2"})
    void accepts_lowercase_words_separated_by_single_hyphens(String value) {
        assertThat(Slug.of(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Java", "spring boot", "spring--boot", "-java", "java-", "été", "c++"})
    void rejects_anything_else(String value) {
        assertThatThrownBy(() -> Slug.of(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_a_slug_longer_than_the_maximum() {
        assertThat(Slug.of("a".repeat(Slug.MAX_LENGTH)).value()).hasSize(Slug.MAX_LENGTH);
        assertThatThrownBy(() -> Slug.of("a".repeat(Slug.MAX_LENGTH + 1))).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Spring Boot", "../admin"})
    void parse_turns_untrusted_input_into_nothing_when_it_cannot_be_a_slug(String candidate) {
        assertThat(Slug.parse(candidate)).isEmpty();
    }

    @Test
    void parse_rejects_a_value_longer_than_the_maximum() {
        assertThat(Slug.parse("a".repeat(Slug.MAX_LENGTH + 1))).isEmpty();
    }

    @Test
    void parse_keeps_a_valid_slug() {
        assertThat(Slug.parse("spring-boot")).contains(Slug.of("spring-boot"));
    }

    // ---- génération (D-BA) ----

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Construire une API REST avec Spring Boot | construire-une-api-rest-avec-spring-boot",
        "Article planifié déjà visible             | article-planifie-deja-visible",
        "L'œuvre de Cœur à Noël                    | l-oeuvre-de-coeur-a-noel",
        "Straße, Ærø, Łódź                         | strasse-aero-lodz",
        "  Spring Boot 4.1 — nouveautés !          | spring-boot-4-1-nouveautes",
        "C++ & C#                                  | c-c",
        "PostgreSQL                                | postgresql",
    })
    void generates_a_readable_ascii_slug_from_a_title(String title, String expected) {
        assertThat(Slug.fromText(title)).isEqualTo(Slug.of(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "!!!", "—", "🙂"})
    void refuses_a_title_without_letter_or_digit(String title) {
        assertThatThrownBy(() -> Slug.fromText(title)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cuts_a_long_title_between_two_words() {
        String title = "mot ".repeat(30);

        Slug slug = Slug.fromText(title);

        assertThat(slug.value()).hasSizeLessThanOrEqualTo(Slug.MAX_GENERATED_LENGTH).endsWith("-mot").doesNotEndWith("-");
    }

    @Test
    void cuts_a_single_very_long_word_at_the_maximum() {
        assertThat(Slug.fromText("a".repeat(200)).value()).hasSize(Slug.MAX_GENERATED_LENGTH);
    }

    // ---- collisions (D-BA) ----

    @Test
    void keeps_a_free_slug() {
        Slug api = Slug.of("api");

        assertThat(api.firstAvailable(slug -> false)).isEqualTo(api);
    }

    @Test
    void appends_the_first_free_numeric_suffix() {
        Set<Slug> taken = Set.of(Slug.of("api"), Slug.of("api-2"), Slug.of("api-3"));

        assertThat(Slug.of("api").firstAvailable(taken::contains)).isEqualTo(Slug.of("api-4"));
    }

    @Test
    void shortens_the_base_to_keep_a_suffixed_slug_within_the_maximum() {
        Slug longest = Slug.of("a".repeat(Slug.MAX_LENGTH));

        Slug suffixed = longest.firstAvailable(Set.of(longest)::contains);

        assertThat(suffixed.value()).hasSize(Slug.MAX_LENGTH).endsWith("-2");
    }

    @Test
    void gives_up_when_every_suffix_is_taken() {
        assertThatThrownBy(() -> Slug.of("api").firstAvailable(slug -> true))
            .isInstanceOf(IllegalStateException.class);
    }
}
