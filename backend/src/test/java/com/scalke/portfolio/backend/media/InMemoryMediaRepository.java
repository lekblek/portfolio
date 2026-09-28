package com.scalke.portfolio.backend.media;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;

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
    public List<Media> findAllById(Collection<Long> ids) {
        return media.stream().filter(stored -> ids.contains(stored.id())).toList();
    }

    @Override
    public void delete(Media deleted) {
        media.removeIf(stored -> stored.id().equals(deleted.id()));
    }

    public List<Media> media() {
        return media;
    }
}
