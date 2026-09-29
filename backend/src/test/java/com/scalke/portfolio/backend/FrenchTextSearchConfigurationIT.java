package com.scalke.portfolio.backend;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Configuration {@code french_unaccent} de {@code V017} (D-BZ) : ce que la recherche considère comme le même mot.
 */
class FrenchTextSearchConfigurationIT extends AbstractIntegrationTest {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void ignores_accents_and_case() {
        assertThat(matches("Le Développement d'une API", "developpement")).isTrue();
        assertThat(matches("Le developpement d'une API", "DÉVELOPPEMENT")).isTrue();
    }

    @Test
    void reduces_words_to_their_stem() {
        assertThat(matches("Développer une API", "développement")).isTrue();
        assertThat(matches("Les écoles", "école")).isTrue();
    }

    @Test
    void treats_a_ligature_like_its_spelled_out_form() {
        assertThat(matches("Au cœur du système", "coeur")).isTrue();
    }

    /**
     * Les mots vides sont retirés avant les accents : désaccentués, « été », « à » et « où » ne seraient plus
     * reconnus comme mots vides et seraient indexés.
     */
    @Test
    void drops_french_stop_words_even_when_accented() {
        assertThat(document("été à où de la")).isEmpty();
        assertThat(document("Un été à Paris")).isEqualTo("'paris':4");
    }

    /**
     * {@code websearch_to_tsquery} accepte n'importe quelle saisie : guillemets, {@code or} et {@code -}
     * ont un sens, le reste est ignoré, jamais une erreur de syntaxe.
     */
    @Test
    void reads_any_user_input_as_a_web_search() {
        assertThat(query("\"spring boot\" -java or angular")).isEqualTo("'spring' <-> 'boot' & !'jav' | 'angular'");
        assertThat(query("(((&|!:*")).isEmpty();
    }

    private boolean matches(String text, String search) {
        return jdbcClient.sql("""
                    SELECT to_tsvector('french_unaccent', :text) @@ websearch_to_tsquery('french_unaccent', :search)
                    """)
            .param("text", text)
            .param("search", search)
            .query(Boolean.class)
            .single();
    }

    private String document(String text) {
        return jdbcClient.sql("SELECT to_tsvector('french_unaccent', :text)::text")
            .param("text", text)
            .query(String.class)
            .single();
    }

    private String query(String search) {
        return jdbcClient.sql("SELECT websearch_to_tsquery('french_unaccent', :search)::text")
            .param("search", search)
            .query(String.class)
            .single();
    }
}
