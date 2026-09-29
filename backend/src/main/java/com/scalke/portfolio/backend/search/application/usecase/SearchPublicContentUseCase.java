package com.scalke.portfolio.backend.search.application.usecase;

import com.scalke.portfolio.backend.project.application.query.ProjectQueryService;
import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.publication.application.query.PublicationQueryService;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.search.domain.model.SearchHit;
import com.scalke.portfolio.backend.search.domain.model.SearchHit.Source;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recherche publique (D09, D-CD) : articles et news visibles, projets publiés. Le module {@code search} n'a
 * aucune table : chaque module propriétaire cherche dans ses contenus et applique sa propre règle de
 * visibilité, par sa façade (ADR 0002) ; ce cas d'usage classe l'ensemble et pagine.
 */
@Service
@RequiredArgsConstructor
public class SearchPublicContentUseCase {

    private final PublicationQueryService publications;
    private final ProjectQueryService projects;

    /**
     * Classe ensemble les contenus trouvés ({@link SearchHit#BY_RELEVANCE}) et ne charge que ceux de la page
     * demandée. Coût constant : trois requêtes pour classer, cinq au plus pour charger la page. Un texte vide
     * ou blanc ne trouve rien, sans requête.
     */
    @Transactional(readOnly = true)
    public PageResult<SearchResult> execute(String text, PageQuery query) {
        if (text.isBlank()) {
            return PageResult.empty(query);
        }
        List<SearchHit> hits = new ArrayList<>();
        publications.searchVisible(text).forEach((id, rank) -> hits.add(new SearchHit(Source.PUBLICATION, id, rank)));
        projects.searchPublished(text).forEach((id, rank) -> hits.add(new SearchHit(Source.PROJECT, id, rank)));
        PageResult<SearchHit> page = SearchHit.page(hits, query);

        Map<Long, Publication> pagePublications = publications.visibleById(idsOf(page, Source.PUBLICATION));
        Map<Long, Project> pageProjects = projects.publishedById(idsOf(page, Source.PROJECT));
        // Un contenu retiré par une écriture concurrente entre le classement et le chargement est omis.
        List<SearchResult> results = page.content().stream()
            .map(hit -> switch (hit.source()) {
                case PUBLICATION -> Optional.ofNullable(pagePublications.get(hit.id())).map(SearchResult::of);
                case PROJECT -> Optional.ofNullable(pageProjects.get(hit.id())).map(SearchResult::of);
            })
            .flatMap(Optional::stream)
            .toList();
        return new PageResult<>(results, page.page(), page.size(), page.totalElements());
    }

    private static Set<Long> idsOf(PageResult<SearchHit> page, Source source) {
        return page.content().stream()
            .filter(hit -> hit.source() == source)
            .map(SearchHit::id)
            .collect(Collectors.toSet());
    }
}
