package com.scalke.portfolio.backend.publication.infrastructure.seed;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.taxonomy.application.query.TaxonomyQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F15 : la publication de démonstration des formules et diagrammes s'ajoute à une base déjà amorcée,
 * une seule fois ; le jeu des cas de visibilité reste réservé à une base vide. D-EU : le jeu représentatif de
 * {@code dev-seed/publications.json} s'ajoute publication par publication, et suffit à paginer chaque liste.
 */
class PublicationSeederTest {

    private static final Slug DEMO_SLUG = Slug.fromText(PublicationSeeder.SCIENTIFIC_DEMO_TITLE);

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");
    private static final int DEMO_CONTENT = 26;

    private final PublicationRepository repository = mock(PublicationRepository.class);
    private final TaxonomyQueryService taxonomy = mock(TaxonomyQueryService.class);
    private final PublicationSeeder seeder = new PublicationSeeder(repository, taxonomy, mock(MediaQueryService.class),
        JsonMapper.builder().build(), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void adds_the_scientific_demo_to_an_already_seeded_database() {
        when(repository.existsAny()).thenReturn(true);
        when(repository.findBySlug(DEMO_SLUG)).thenReturn(Optional.empty());

        seeder.run(null);

        ArgumentCaptor<Publication> created = ArgumentCaptor.forClass(Publication.class);
        verify(repository, times(1 + DEMO_CONTENT)).create(created.capture());
        Publication demo = created.getAllValues().getFirst();
        assertThat(demo.slug()).isEqualTo(DEMO_SLUG);
        assertThat(demo.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(demo.contentMarkdown()).contains("$$", "```mermaid", "accTitle:", "[^note]");
    }

    @Test
    void does_not_add_a_demo_publication_twice() {
        when(repository.existsAny()).thenReturn(true);
        when(repository.findBySlug(any())).thenReturn(Optional.of(mock(Publication.class)));

        seeder.run(null);

        verify(repository, never()).create(any());
    }

    @Test
    void seeds_every_visibility_case_then_the_demo_in_an_empty_database() {
        when(repository.existsAny()).thenReturn(false);
        when(repository.findBySlug(DEMO_SLUG)).thenReturn(Optional.empty());

        seeder.run(null);

        verify(repository, times(8 + DEMO_CONTENT)).create(any());
    }

    @Test
    void the_demo_content_fills_two_pages_of_each_list_and_every_state() {
        when(repository.existsAny()).thenReturn(true);
        when(repository.findBySlug(DEMO_SLUG)).thenReturn(Optional.of(mock(Publication.class)));

        seeder.run(null);

        ArgumentCaptor<Publication> captor = ArgumentCaptor.forClass(Publication.class);
        verify(repository, times(DEMO_CONTENT)).create(captor.capture());
        List<Publication> created = captor.getAllValues();
        assertThat(visible(created, PublicationType.ARTICLE)).isGreaterThan(10);
        assertThat(visible(created, PublicationType.NEWS)).isGreaterThan(9);
        assertThat(created).extracting(Publication::status).contains(PublicationStatus.DRAFT,
            PublicationStatus.SCHEDULED, PublicationStatus.ARCHIVED);
        Publication scheduled = created.stream().filter(p -> p.status() == PublicationStatus.SCHEDULED).findFirst()
            .orElseThrow();
        assertThat(scheduled.publishedAt()).isAfter(NOW);
        assertThat(created).allSatisfy(p -> assertThat(p.contentMarkdown()).doesNotContain("{{media:"));
        assertThat(created).anySatisfy(p -> assertThat(p.contentMarkdown())
            .contains("```mermaid", "$$", "```python", "| Condition |", "\"Détections sur une scène urbaine"));
    }

    private static long visible(List<Publication> created, PublicationType type) {
        return created.stream()
            .filter(p -> p.type() == type && p.status() == PublicationStatus.PUBLISHED)
            .count();
    }
}
