package com.scalke.portfolio.backend.publication.infrastructure.seed;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.taxonomy.application.query.TaxonomyQueryService;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Publications de démonstration du profil `dev`, créées uniquement si la base n'en contient aucune.
 * Ce ne sont pas des données réelles (voir docs/01-perimetre-v1.md §20).
 * <p>
 * Un cas par règle de visibilité : deux publiées, une planifiée passée (visible), une planifiée
 * future, un brouillon, une en relecture et une archivée (invisibles). Dates relatives à l'horloge.
 * Les termes de classement sont ceux du seed de la taxonomie (exécuté avant), obtenus par sa façade.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class PublicationSeeder implements ApplicationRunner {

    private final PublicationRepository publicationRepository;
    private final TaxonomyQueryService taxonomy;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (publicationRepository.existsAny()) {
            return;
        }
        Instant now = clock.instant();
        Long backend = category("backend");
        Long architecture = category("architecture");
        List.of(
            demo(PublicationType.ARTICLE, "Construire une API REST avec Spring Boot", "construire-une-api-rest-avec-spring-boot",
                PublicationStatus.PUBLISHED, now.minus(Duration.ofDays(30)), true, backend,
                tags("java", "spring-boot", "tests"), now),
            demo(PublicationType.NEWS, "Lancement du portfolio", "lancement-du-portfolio",
                PublicationStatus.PUBLISHED, now.minus(Duration.ofDays(7)), false, null,
                tags("angular"), now),
            demo(PublicationType.ARTICLE, "Article planifié déjà visible", "article-planifie-deja-visible",
                PublicationStatus.SCHEDULED, now.minus(Duration.ofDays(1)), false, architecture,
                tags("postgresql"), now),
            demo(PublicationType.ARTICLE, "Article planifié à venir", "article-planifie-a-venir",
                PublicationStatus.SCHEDULED, now.plus(Duration.ofDays(7)), false, backend, tags("java"), now),
            demo(PublicationType.ARTICLE, "Brouillon de démonstration", "brouillon-de-demonstration",
                PublicationStatus.DRAFT, null, false, null, Set.of(), now),
            demo(PublicationType.ARTICLE, "Article en relecture", "article-en-relecture",
                PublicationStatus.IN_REVIEW, null, false, null, Set.of(), now),
            demo(PublicationType.NEWS, "Actualité archivée", "actualite-archivee",
                PublicationStatus.ARCHIVED, now.minus(Duration.ofDays(365)), false, null, Set.of(), now)
        ).forEach(publicationRepository::create);
        log.info("Publications de démonstration créées (profil dev)");
    }

    private Long category(String slug) {
        return taxonomy.findCategoryBySlug(slug).map(Category::id).orElse(null);
    }

    private Set<Long> tags(String... slugs) {
        return Stream.of(slugs)
            .map(taxonomy::findTagBySlug)
            .flatMap(tag -> tag.map(Tag::id).stream())
            .collect(Collectors.toSet());
    }

    private static Publication demo(PublicationType type, String title, String slug, PublicationStatus status,
                                    Instant publishedAt, boolean featured, Long categoryId, Set<Long> tagIds,
                                    Instant now) {
        return new Publication(null, type, title, slug,
            "Publication de démonstration : " + title + ".",
            "## " + title + "\n\nContenu de démonstration.",
            status, publishedAt, featured, categoryId, tagIds, null, null, now, now);
    }
}
