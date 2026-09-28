package com.scalke.portfolio.backend.media.web.controller;

import com.scalke.portfolio.backend.media.application.usecase.MediaFile;
import com.scalke.portfolio.backend.media.application.usecase.OpenMediaFileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RequiredArgsConstructor
@RestController
@RequestMapping("/public/media")
public class PublicMediaController {

    /**
     * Une clé n'est jamais réutilisée pour un autre contenu (un nouvel envoi crée une nouvelle clé) : le fichier
     * peut rester en cache indéfiniment (D-BP).
     */
    private static final CacheControl IMMUTABLE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private final OpenMediaFileUseCase openMediaFileUseCase;

    /**
     * Contenu brut du fichier, avec le type MIME de son format ; {@code nosniff} interdit au navigateur d'en
     * deviner un autre. Le flux est fermé par Spring après l'écriture de la réponse.
     */
    @GetMapping("/{storageKey}")
    ResponseEntity<InputStreamResource> getMedia(@PathVariable String storageKey) {
        MediaFile file = openMediaFileUseCase.execute(storageKey);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.format().mimeType()))
            .contentLength(file.content().size())
            .cacheControl(IMMUTABLE)
            .header("X-Content-Type-Options", "nosniff")
            .body(new InputStreamResource(file.content().stream()));
    }
}
