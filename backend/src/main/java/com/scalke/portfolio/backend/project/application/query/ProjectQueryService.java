package com.scalke.portfolio.backend.project.application.query;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Façade de lecture du module {@code project} pour les autres modules (ADR 0002, D-CC).
 * <p>
 * Seuls les projets {@code PUBLISHED} en sortent (invariant 11, D-U) : un autre module ne réécrit jamais cette
 * règle. Transactionnelle en lecture : appelée depuis un cas d'usage, elle rejoint sa transaction.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryService {

    private final ProjectRepository projectRepository;

    /**
     * Pertinence des projets publiés dont le document de recherche (titre et technologies, description courte,
     * description, du plus fort au plus faible) correspond à {@code text}, lu comme une recherche web, par
     * identifiant. Une requête ; vide si rien ne correspond.
     */
    public Map<Long, Double> searchPublished(String text) {
        return projectRepository.searchPublished(text);
    }

    /**
     * Projets publiés parmi {@code ids}, indexés par identifiant ; les autres sont absents. Nombre de requêtes
     * constant (projets, technologies, captures) ; aucune si la collection est vide.
     */
    public Map<Long, Project> publishedById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return projectRepository.findPublishedByIds(ids).stream()
            .collect(Collectors.toUnmodifiableMap(Project::id, Function.identity()));
    }
}
