package com.scalke.portfolio.backend.publication.infrastructure.seed;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.taxonomy.application.query.TaxonomyQueryService;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
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
 * Exécuté avant le seed des séries ({@link Order} 1), qui range ces articles.
 * <p>
 * Exception additive (F15) : la publication de démonstration des formules et diagrammes est créée
 * dès que son slug manque, même dans une base déjà amorcée : une base de développement existante
 * la reçoit sans être recréée. Contenu de démonstration, pas un contenu scientifique réel.
 */
@Component
@Profile("dev")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class PublicationSeeder implements ApplicationRunner {

    private final PublicationRepository publicationRepository;
    private final TaxonomyQueryService taxonomy;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant now = clock.instant();
        if (!publicationRepository.existsAny()) {
            seedVisibilityCases(now);
        }
        seedScientificDemoIfMissing(now);
    }

    private void seedVisibilityCases(Instant now) {
        Long backend = category("backend");
        Long architecture = category("architecture");
        List.of(
            demo(PublicationType.ARTICLE, "Construire une API REST avec Spring Boot",
                PublicationStatus.PUBLISHED, now.minus(Duration.ofDays(30)), true, backend,
                tags("java", "spring-boot", "tests"), now),
            demo(PublicationType.NEWS, "Lancement du portfolio",
                PublicationStatus.PUBLISHED, now.minus(Duration.ofDays(7)), false, null,
                tags("angular"), now),
            demo(PublicationType.ARTICLE, "Article planifié déjà visible",
                PublicationStatus.SCHEDULED, now.minus(Duration.ofDays(1)), false, architecture,
                tags("postgresql"), now),
            demo(PublicationType.ARTICLE, "Article planifié à venir",
                PublicationStatus.SCHEDULED, now.plus(Duration.ofDays(7)), false, backend, tags("java"), now),
            demo(PublicationType.ARTICLE, "Brouillon de démonstration",
                PublicationStatus.DRAFT, null, false, null, Set.of(), now),
            demo(PublicationType.ARTICLE, "Article en relecture",
                PublicationStatus.IN_REVIEW, null, false, null, Set.of(), now),
            demo(PublicationType.NEWS, "Actualité archivée",
                PublicationStatus.ARCHIVED, now.minus(Duration.ofDays(365)), false, null, Set.of(), now)
        ).forEach(publicationRepository::create);
        log.info("Publications de démonstration créées (profil dev)");
    }

    static final String SCIENTIFIC_DEMO_TITLE = "Démonstration : formules, diagramme et code";

    static final String SCIENTIFIC_DEMO_CONTENT = """
        Cette publication de démonstration vérifie le rendu des formules, des diagrammes et du code. \
        Ce n'est pas un contenu réel.

        ## Formules

        Dans le texte : l'aire d'un disque vaut $A = \\pi r^2$ et une moyenne s'écrit \
        $\\bar{x} = \\frac{1}{n}\\sum_{i=1}^{n} x_i$.

        Formule centrée :

        $$
        \\nabla \\cdot \\mathbf{E} = \\frac{\\rho}{\\varepsilon_0}
        $$

        Formule longue, qui défile dans son cadre sur un petit écran :

        $$
        f(x) = a_0 + a_1 x + a_2 x^2 + a_3 x^3 + a_4 x^4 + a_5 x^5 + a_6 x^6 + a_7 x^7 + a_8 x^8 \
        + a_9 x^9 + a_{10} x^{10} + a_{11} x^{11} + a_{12} x^{12}
        $$

        Une formule invalide reste lisible : $\\frac{1}{$. Un montant n'est pas une formule : 5 $ et 6 $.

        ## Diagramme

        ```mermaid
        flowchart LR
          accTitle: Chaîne de traitement de démonstration
          accDescr: Les images sont reçues, analysées, puis les résultats sont publiés.
          A[Réception] --> B[Analyse]
          B --> C[Publication]
        ```

        ## Code

        ```java
        record Mesure(String nom, double valeur) {}
        ```

        Une note de démonstration[^note].

        [^note]: Les notes se placent dans la marge sur un grand écran.
        """;

    private void seedScientificDemoIfMissing(Instant now) {
        Slug slug = Slug.fromText(SCIENTIFIC_DEMO_TITLE);
        if (publicationRepository.findBySlug(slug).isPresent()) {
            return;
        }
        Instant publishedAt = now.minus(Duration.ofDays(2));
        publicationRepository.create(new Publication(null, PublicationType.ARTICLE, SCIENTIFIC_DEMO_TITLE, slug,
            "Publication de démonstration du profil dev : formules, diagramme, code et note.",
            SCIENTIFIC_DEMO_CONTENT, PublicationStatus.PUBLISHED, publishedAt, publishedAt, false,
            category("architecture"), tags("tests"), null, null, now, now, null));
        log.info("Publication de démonstration des formules et diagrammes créée (profil dev)");
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

    /**
     * Slug généré depuis le titre (D-BA). La première date de publication vaut la date de publication
     * (provisoire pour une planification future, D-AZ).
     */
    private static Publication demo(PublicationType type, String title, PublicationStatus status,
                                    Instant publishedAt, boolean featured, Long categoryId, Set<Long> tagIds,
                                    Instant now) {
        return new Publication(null, type, title, Slug.fromText(title),
            "Publication de démonstration : " + title + ".",
            "## " + title + "\n\nContenu de démonstration.",
            status, publishedAt, publishedAt, featured, categoryId, tagIds, null, null, now, now, null);
    }
}
