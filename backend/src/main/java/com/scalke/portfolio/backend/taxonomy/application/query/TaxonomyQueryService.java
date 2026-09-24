package com.scalke.portfolio.backend.taxonomy.application.query;

import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.CategoryRepository;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Façade de lecture du module {@code taxonomy} pour les autres modules ({@code 04} §8, D-AN).
 * <p>
 * Un autre module ne connaît de la taxonomie que ce service et les records de {@code domain.model} :
 * jamais ses ports, ses entités ni ses tables (règle ArchUnit
 * {@code modules_only_use_each_other_through_domain_models_and_application_services}).
 * Transactionnelle en lecture : appelée depuis un cas d'usage, elle rejoint sa transaction.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxonomyQueryService {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public Optional<Category> findCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }

    public Optional<Tag> findTagBySlug(String slug) {
        return tagRepository.findBySlug(slug);
    }

    /**
     * Une seule requête, quel que soit le nombre d'identifiants ; aucune si la collection est vide.
     */
    public Map<Long, Category> categoriesById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return categoryRepository.findAllById(ids).stream()
            .collect(Collectors.toUnmodifiableMap(Category::id, Function.identity()));
    }

    /**
     * Une seule requête, quel que soit le nombre d'identifiants ; aucune si la collection est vide.
     */
    public Map<Long, Tag> tagsById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return tagRepository.findAllById(ids).stream()
            .collect(Collectors.toUnmodifiableMap(Tag::id, Function.identity()));
    }
}
