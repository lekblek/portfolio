package com.scalke.portfolio.backend.publication.infrastructure.seed;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.media.domain.model.Media;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * <p>
 * Jeu représentatif (D-EU, F27) : les publications de {@code dev-seed/publications.json} sont créées de la même façon,
 * chacune dès que son slug manque : assez d'articles et d'actualités publiés pour deux pages de chaque liste, des
 * brouillons, une planification future et des archives, avec ou sans couverture ; trois articles riches (Mermaid,
 * formules, code, tableaux, figures légendées) dont le Markdown est dans {@code dev-seed/publications/}. Une image du
 * contenu s'écrit {@code {{media:nom-du-fichier}}}, remplacé par l'adresse publique du média de démonstration. Dates
 * fixes, sauf la planification future, relative à l'horloge ({@code +P21D}).
 */
@Component
@Profile("dev")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class PublicationSeeder implements ApplicationRunner {

    private final PublicationRepository publicationRepository;
    private final TaxonomyQueryService taxonomy;
    private final MediaQueryService media;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant now = clock.instant();
        if (!publicationRepository.existsAny()) {
            seedVisibilityCases(now);
        }
        seedScientificDemoIfMissing(now);
        seedDemoContentIfMissing(now);
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

    static final String SOURCE = "dev-seed/publications.json";
    static final String CONTENT_ROOT = "dev-seed/publications/";
    private static final Pattern MEDIA_REFERENCE = Pattern.compile("\\{\\{media:([^}]+)}}");

    private void seedDemoContentIfMissing(Instant now) {
        ClassPathResource source = new ClassPathResource(SOURCE);
        if (!source.exists()) {
            return;
        }
        int created = 0;
        for (DemoPublication demo : read(source, DemoPublications.class).publications()) {
            Slug slug = Slug.fromText(demo.title());
            if (publicationRepository.findBySlug(slug).isEmpty()) {
                publicationRepository.create(publication(demo, slug, now));
                created++;
            }
        }
        if (created > 0) {
            log.info("{} publications de démonstration créées (profil dev)", created);
        }
    }

    private Publication publication(DemoPublication demo, Slug slug, Instant now) {
        Instant publishedAt = demo.publishedAt() == null ? null
            : demo.publishedAt().startsWith("+") ? now.plus(Duration.parse(demo.publishedAt().substring(1)))
            : Instant.parse(demo.publishedAt());
        Instant written = publishedAt != null && publishedAt.isBefore(now) ? publishedAt : now;
        String content = demo.contentFile() == null ? demo.content() : text(CONTENT_ROOT + demo.contentFile());
        return new Publication(null, demo.type(), demo.title(), slug, demo.summary(), withMediaUrls(content),
            demo.status(), publishedAt, publishedAt, demo.featured(),
            demo.category() == null ? null : category(demo.category()), tags(demo.tags().toArray(String[]::new)),
            null, null, written, written, demo.cover() == null ? null : mediaOf(demo.cover()).map(Media::id).orElse(null));
    }

    /**
     * {@code {{media:fichier}}} → adresse publique du média de démonstration ; un média absent laisse une adresse
     * vide, que le rendu Markdown affiche comme un texte.
     */
    private String withMediaUrls(String content) {
        Matcher matcher = MEDIA_REFERENCE.matcher(content);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String url = mediaOf(matcher.group(1)).map(found -> MediaQueryService.publicUrl(found.storageKey())).orElse("");
            matcher.appendReplacement(result, Matcher.quoteReplacement(url));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private Optional<Media> mediaOf(String file) {
        return media.findByOriginalName(file);
    }

    private <T> T read(ClassPathResource source, Class<T> type) {
        try (InputStream content = source.getInputStream()) {
            return jsonMapper.readValue(content, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String text(String path) {
        try (InputStream content = new ClassPathResource(path).getInputStream()) {
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record DemoPublications(List<DemoPublication> publications) {
    }

    /**
     * {@code publishedAt} : instant ISO, durée relative à l'horloge ({@code +P21D}) ou absente ; le contenu est dans
     * {@code content} ou dans le fichier {@code contentFile}.
     */
    record DemoPublication(PublicationType type, String title, String summary, String content, String contentFile,
                           PublicationStatus status, String publishedAt, boolean featured, String category,
                           List<String> tags, String cover) {
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
