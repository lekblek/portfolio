package com.scalke.portfolio.backend.media.application.query;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Façade de lecture du catalogue pour les autres modules (ADR 0002, D-BW). Ils ne stockent que des
 * identifiants de médias et obtiennent ici leur forme publique : l'adresse d'un fichier reste l'affaire du
 * module {@code media}, qui pourra la faire pointer ailleurs (stockage S3) sans les modifier.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaQueryService {

    /**
     * Préfixe public des fichiers : chemin de {@code PublicMediaController} sous le contexte {@code /api}.
     */
    static final String PUBLIC_PATH = "/api/public/media/";

    private final MediaRepository mediaRepository;

    /**
     * Images parmi {@code ids}, sous leur forme publique ; un PDF ou un identifiant inconnu est absent. Une
     * seule requête ; aucune si la collection est vide.
     */
    public Map<Long, PublicImage> imagesById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return mediaRepository.findAllById(ids).stream()
            .filter(media -> media.format().isImage())
            .collect(Collectors.toUnmodifiableMap(Media::id, PublicImage::of));
    }

    /**
     * Documents PDF parmi {@code ids}, sous leur forme publique ; une image ou un identifiant inconnu est absent.
     * Une seule requête ; aucune si la collection est vide.
     */
    public Map<Long, PublicDocument> documentsById(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return mediaRepository.findAllById(ids).stream()
            .filter(media -> !media.format().isImage())
            .collect(Collectors.toUnmodifiableMap(Media::id, PublicDocument::of));
    }
}
