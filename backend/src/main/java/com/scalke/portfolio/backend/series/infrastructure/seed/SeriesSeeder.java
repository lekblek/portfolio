package com.scalke.portfolio.backend.series.infrastructure.seed;

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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Séries de démonstration du profil `dev`, créées uniquement si la base n'en contient aucune. Ce ne sont pas
 * des données réelles (voir docs/01-perimetre-v1.md §20).
 * <p>
 * Une série visible (un article publié, un planifié passé, un planifié futur : deux chapitres publics) et
 * une série invisible (brouillon et article en relecture). Les articles sont ceux du seed des publications
 * (exécuté avant, {@link Order} 2), obtenus par la façade du module {@code publication}.
 */
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class SeriesSeeder implements ApplicationRunner {

    private final SeriesRepository seriesRepository;
    private final PublicationQueryService publications;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (seriesRepository.existsAny()) {
            return;
        }
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
