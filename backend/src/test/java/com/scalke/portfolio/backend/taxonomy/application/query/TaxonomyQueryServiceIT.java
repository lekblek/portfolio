package com.scalke.portfolio.backend.taxonomy.application.query;

import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.CategoryRepository;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TaxonomyQueryServiceIT extends AbstractIntegrationTest {

    @Autowired
    TaxonomyQueryService taxonomyQueryService;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void finds_a_category_and_a_tag_by_slug() {
        Category backend = categoryRepository.create(new Category(null, "Backend", "backend", "Spring Boot"));
        Tag java = tagRepository.create(new Tag(null, "Java", "java"));

        assertThat(taxonomyQueryService.findCategoryBySlug("backend")).contains(backend);
        assertThat(taxonomyQueryService.findTagBySlug("java")).contains(java);
        assertThat(taxonomyQueryService.findCategoryBySlug("inconnue")).isEmpty();
        assertThat(taxonomyQueryService.findTagBySlug("inconnu")).isEmpty();
    }

    @Test
    void resolves_several_terms_by_id_in_one_query_each() {
        Category backend = categoryRepository.create(new Category(null, "Backend", "backend", null));
        Category frontend = categoryRepository.create(new Category(null, "Frontend", "frontend", null));
        List<Tag> tags = List.of(
            tagRepository.create(new Tag(null, "Java", "java")),
            tagRepository.create(new Tag(null, "Angular", "angular")),
            tagRepository.create(new Tag(null, "Tests", "tests")));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        Map<Long, Category> categories = taxonomyQueryService.categoriesById(Set.of(backend.id(), frontend.id()));
        Map<Long, Tag> tagsById = taxonomyQueryService.tagsById(tags.stream().map(Tag::id).toList());

        assertThat(categories).containsOnlyKeys(backend.id(), frontend.id());
        assertThat(tagsById.values()).containsExactlyInAnyOrderElementsOf(tags);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void resolving_no_identifier_costs_no_query() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        assertThat(taxonomyQueryService.categoriesById(Set.of())).isEmpty();
        assertThat(taxonomyQueryService.tagsById(Set.of())).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }
}
