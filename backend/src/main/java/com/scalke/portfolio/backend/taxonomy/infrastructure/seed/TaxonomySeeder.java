package com.scalke.portfolio.backend.taxonomy.infrastructure.seed;

import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.CategoryRepository;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catégories et tags de démonstration du profil `dev` (pas des données réelles : docs/01-perimetre-v1.md §20).
 * <p>
 * Idempotent : un terme n'est créé que si son slug est absent. Exécuté avant les autres seeds
 * ({@link Order} 0), car les publications de démonstration y font référence.
 */
@Component
@Profile("dev")
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class TaxonomySeeder implements ApplicationRunner {

    static final List<Category> CATEGORIES = List.of(
        category("Backend", "Spring Boot, API, persistance."),
        category("Frontend", "Angular, rendu serveur, accessibilité."),
        category("Architecture", null),
        category("Vision par ordinateur", "Détection, suivi, évaluation et déploiement embarqué."),
        category("DevOps", "Conteneurs, intégration continue, déploiement et supervision."));

    static final List<Tag> TAGS = List.of(tag("Java"), tag("Spring Boot"), tag("Angular"), tag("PostgreSQL"), tag("Tests"),
        tag("Python"), tag("PyTorch"), tag("OpenCV"), tag("Docker"), tag("Kubernetes"), tag("CI/CD"), tag("TypeScript"),
        tag("Accessibilité"), tag("Performance"), tag("Observabilité"));

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;
        for (Category category : CATEGORIES) {
            if (categoryRepository.findBySlug(category.slug()).isEmpty()) {
                categoryRepository.create(category);
                created++;
            }
        }
        for (Tag tag : TAGS) {
            if (tagRepository.findBySlug(tag.slug()).isEmpty()) {
                tagRepository.create(tag);
                created++;
            }
        }
        if (created > 0) {
            log.info("{} catégories et tags de démonstration créés (profil dev)", created);
        }
    }

    /**
     * Slug généré depuis le nom, comme le fera l'administration (D-BA).
     */
    private static Category category(String name, String description) {
        return new Category(null, name, Slug.fromText(name), description);
    }

    private static Tag tag(String name) {
        return new Tag(null, name, Slug.fromText(name));
    }
}
