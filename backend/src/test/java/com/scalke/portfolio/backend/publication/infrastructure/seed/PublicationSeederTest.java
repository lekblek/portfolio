package com.scalke.portfolio.backend.publication.infrastructure.seed;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.taxonomy.application.query.TaxonomyQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
 * une seule fois ; le reste du jeu d'amorçage reste réservé à une base vide.
 */
class PublicationSeederTest {

    private static final Slug DEMO_SLUG = Slug.fromText(PublicationSeeder.SCIENTIFIC_DEMO_TITLE);

    private final PublicationRepository repository = mock(PublicationRepository.class);
    private final TaxonomyQueryService taxonomy = mock(TaxonomyQueryService.class);
    private final PublicationSeeder seeder = new PublicationSeeder(repository, taxonomy,
        Clock.fixed(Instant.parse("2026-10-01T08:00:00Z"), ZoneOffset.UTC));

    @Test
    void adds_the_scientific_demo_to_an_already_seeded_database() {
        when(repository.existsAny()).thenReturn(true);
        when(repository.findBySlug(DEMO_SLUG)).thenReturn(Optional.empty());

        seeder.run(null);

        ArgumentCaptor<Publication> created = ArgumentCaptor.forClass(Publication.class);
        verify(repository, times(1)).create(created.capture());
        Publication demo = created.getValue();
        assertThat(demo.slug()).isEqualTo(DEMO_SLUG);
        assertThat(demo.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(demo.contentMarkdown()).contains("$$", "```mermaid", "accTitle:", "[^note]");
    }

    @Test
    void does_not_add_the_scientific_demo_twice() {
        when(repository.existsAny()).thenReturn(true);
        when(repository.findBySlug(DEMO_SLUG)).thenReturn(Optional.of(mock(Publication.class)));

        seeder.run(null);

        verify(repository, never()).create(any());
    }

    @Test
    void seeds_every_visibility_case_then_the_demo_in_an_empty_database() {
        when(repository.existsAny()).thenReturn(false);
        when(repository.findBySlug(DEMO_SLUG)).thenReturn(Optional.empty());

        seeder.run(null);

        verify(repository, times(8)).create(any());
    }
}
