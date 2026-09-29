package com.scalke.portfolio.backend.publication.application.query;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Façade de lecture du module {@code publication} pour les autres modules (ADR 0002, D-BH).
 * <p>
 * La règle de visibilité reste celle du module (D-AH), évaluée avec l'horloge applicative : un autre
 * module demande « lesquelles sont visibles », il ne la réécrit jamais. Transactionnelle en lecture :
 * appelée depuis un cas d'usage, elle rejoint sa transaction.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicationQueryService {

    private final PublicationRepository publicationRepository;
    private final Clock clock;

    /**
     * Identifiants, parmi {@code ids}, des publications visibles maintenant. Une seule requête, qui ne
     * charge pas les publications ; aucune si la collection est vide.
     */
    public Set<Long> visibleIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        return publicationRepository.findVisibleIds(ids, clock.instant());
    }

    /**
     * Publications visibles maintenant parmi {@code ids}, indexées par identifiant ; les autres sont
     * absentes. Nombre de requêtes constant ; aucune si la collection est vide.
     */
    public Map<Long, Publication> visibleById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return publicationRepository.findVisibleByIds(ids, clock.instant()).stream()
            .collect(Collectors.toUnmodifiableMap(Publication::id, Function.identity()));
    }

    /**
     * Pertinence des publications visibles maintenant dont le document de recherche (titre et tags, résumé,
     * contenu, du plus fort au plus faible) correspond à {@code text}, lu comme une recherche web, par
     * identifiant (D-CC). Deux requêtes au plus ; vide si rien ne correspond.
     */
    public Map<Long, Double> searchVisible(String text) {
        return publicationRepository.searchVisible(text, clock.instant());
    }

    /**
     * Publication visible maintenant portant ce slug ; vide si elle n'existe pas ou n'est pas visible (les deux
     * cas sont indiscernables, comme pour la lecture publique).
     */
    public Optional<Publication> findVisibleBySlug(Slug slug) {
        return publicationRepository.findVisibleBySlug(slug, clock.instant());
    }

    /**
     * Toute publication portant ce slug, quel que soit son statut : pour le seed de développement (et
     * l'administration), jamais pour une lecture publique.
     */
    public Optional<Publication> findBySlug(Slug slug) {
        return publicationRepository.findBySlug(slug);
    }
}
