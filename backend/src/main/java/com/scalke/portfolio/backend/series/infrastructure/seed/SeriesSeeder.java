package com.scalke.portfolio.backend.series.infrastructure.seed;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.publication.application.query.PublicationQueryService;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.model.SeriesItem;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Séries de démonstration du profil `dev`, créées uniquement si la base n'en contient aucune. Ce ne sont pas
 * des données réelles (voir docs/01-perimetre-v1.md §20).
 * <p>
 * Une série visible (un article publié, un planifié passé, un planifié futur : deux chapitres publics) et
 * une série invisible (brouillon et article en relecture). Les articles sont ceux du seed des publications
 * (exécuté avant, {@link Order} 2), obtenus par la façade du module {@code publication}.
 * <p>
 * Jeu représentatif (D-EU, F27) : les séries de {@code dev-seed/series.json} sont créées dès que leur slug manque,
 * chapitres désignés par le titre de leur article, couverture parmi les médias de démonstration.
 */
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class SeriesSeeder implements ApplicationRunner {

    private final SeriesRepository seriesRepository;
    private final PublicationQueryService publications;
    private final MediaQueryService media;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seriesRepository.existsAny()) {
            seedVisibilityCases();
        }
        seedDemoSeriesIfMissing();
    }

    private void seedVisibilityCases() {
        seriesRepository.create(demo("Spring Boot de zéro à la production",
            "construire-une-api-rest-avec-spring-boot", "article-planifie-deja-visible", "article-planifie-a-venir"));
        seriesRepository.create(demo("Angular moderne",
            "brouillon-de-demonstration", "article-en-relecture"));
        log.info("Séries de démonstration créées (profil dev)");
    }

    /**
     * Articles aux positions 1, 2, … dans l'ordre donné ; un slug absent ou désignant une NEWS est ignoré.
     * Slug de la série généré depuis son titre (D-BA).
     */
    static final String SOURCE = "dev-seed/series.json";

    private void seedDemoSeriesIfMissing() {
        ClassPathResource source = new ClassPathResource(SOURCE);
        if (!source.exists()) {
            return;
        }
        for (DemoSeries demo : read(source).series()) {
            Slug slug = Slug.fromText(demo.title());
            if (seriesRepository.findBySlug(slug).isPresent()) {
                continue;
            }
            List<SeriesItem> items = new ArrayList<>();
            for (String title : demo.chapters()) {
                publications.findBySlug(Slug.fromText(title))
                    .filter(publication -> publication.type() == PublicationType.ARTICLE)
                    .ifPresent(article -> items.add(new SeriesItem(article.id(), items.size() + 1)));
            }
            Long cover = demo.cover() == null ? null : media.findByOriginalName(demo.cover()).map(Media::id).orElse(null);
            seriesRepository.create(new Series(null, demo.title(), slug, demo.description(), items, cover));
            log.info("Série de démonstration « {} » créée (profil dev)", demo.title());
        }
    }

    private DemoSeriesList read(ClassPathResource source) {
        try (InputStream content = source.getInputStream()) {
            return jsonMapper.readValue(content, DemoSeriesList.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record DemoSeriesList(List<DemoSeries> series) {
    }

    record DemoSeries(String title, String description, List<String> chapters, String cover) {
    }

    private Series demo(String title, String... articleSlugs) {
        List<SeriesItem> items = new ArrayList<>();
        for (String slug : articleSlugs) {
            publications.findBySlug(Slug.of(slug))
                .filter(publication -> publication.type() == PublicationType.ARTICLE)
                .ifPresent(article -> items.add(new SeriesItem(article.id(), items.size() + 1)));
        }
        return new Series(null, title, Slug.fromText(title),
            "Série de démonstration : " + title + ".", items, null);
    }
}
