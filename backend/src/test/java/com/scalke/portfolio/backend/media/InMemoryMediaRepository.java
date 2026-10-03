package com.scalke.portfolio.backend.media;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Catalogue en mémoire pour les tests unitaires des cas d'usage : attribue des identifiants croissants. Les
 * références (clés étrangères) n'existent pas ici : elles se testent en intégration.
 */
public class InMemoryMediaRepository implements MediaRepository {

    private final List<Media> media = new ArrayList<>();
    private long nextId = 1;

    @Override
    public Media create(Media created) {
        Media stored = new Media(nextId++, created.storageKey(), created.originalName(), created.size(),
            created.dimensions(), created.altText(), created.createdAt());
        media.add(stored);
        return stored;
    }

    @Override
    public Optional<Media> findById(Long id) {
        return media.stream().filter(stored -> stored.id().equals(id)).findFirst();
    }

    @Override
    public Optional<Media> findFirstByOriginalName(String originalName) {
        return media.stream().filter(stored -> stored.originalName().equals(originalName)).findFirst();
    }

    @Override
    public List<Media> findAllById(Collection<Long> ids) {
        return media.stream().filter(stored -> ids.contains(stored.id())).toList();
    }

    @Override
    public void delete(Media deleted) {
        media.removeIf(stored -> stored.id().equals(deleted.id()));
    }

    /**
     * Non utilisée par les tests unitaires : la pagination se teste en intégration.
     */
    @Override
    public PageResult<Media> findPage(PageQuery query) {
        throw new UnsupportedOperationException("paging is tested against PostgreSQL");
    }

    @Override
    public Media updateAltText(Media updated) {
        media.replaceAll(stored -> stored.id().equals(updated.id()) ? updated : stored);
        return updated;
    }

    public List<Media> media() {
        return media;
    }
}
