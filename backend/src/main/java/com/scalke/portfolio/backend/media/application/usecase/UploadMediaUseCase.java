package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.Dimensions;
import com.scalke.portfolio.backend.media.domain.model.ImageDimensions;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.model.MediaFormat;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.media.domain.port.MediaStorage;
import com.scalke.portfolio.backend.shared.error.ContentTooLargeException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.UnsupportedContentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class UploadMediaUseCase {

    private static final long MIB = 1024 * 1024;

    private final MediaStorage mediaStorage;
    private final MediaRepository mediaRepository;
    private final Clock clock;

    /**
     * Valide un fichier envoyé, l'inscrit au catalogue puis le stocke (D-BR, D-BU). Le format est reconnu par
     * la signature du contenu, les dimensions d'une image par son en-tête ; la lecture s'arrête au-delà de la
     * plus grande taille acceptée. Le flux est lu, pas fermé : il appartient à l'appelant.
     * <p>
     * Ordre : insertion d'abord, écriture du fichier ensuite. Un échec d'écriture annule l'insertion (même
     * transaction) ; un échec de validation de la transaction laisse au pire un fichier orphelin, jamais
     * publié puisque sa clé n'est connue de personne. Route : {@code POST /api/admin/media} (D-CT).
     *
     * @throws UnsupportedContentException format non reconnu ou image illisible ({@code UNSUPPORTED_MEDIA_FORMAT}, 415)
     * @throws ContentTooLargeException fichier trop volumineux pour son format ({@code MEDIA_TOO_LARGE}, 413)
     */
    @Transactional
    public Media execute(MediaUpload upload) {
        byte[] bytes = readAtMost(upload.content(), MediaFormat.maxAcceptedSize() + 1);
        MediaFormat format = MediaFormat.detect(bytes).orElseThrow(() -> new UnsupportedContentException(
            ErrorCode.UNSUPPORTED_MEDIA_FORMAT, "Format de fichier non accepté : PNG, JPEG, WebP ou PDF."));
        if (bytes.length > format.maxSize()) {
            throw new ContentTooLargeException(ErrorCode.MEDIA_TOO_LARGE,
                "Fichier trop volumineux : " + format.maxSize() / MIB + " Mo au plus pour ce format.");
        }
        Dimensions dimensions = format.isImage()
            ? ImageDimensions.read(format, bytes).orElseThrow(() -> new UnsupportedContentException(
                ErrorCode.UNSUPPORTED_MEDIA_FORMAT, "Image illisible : ses dimensions ne peuvent pas être lues."))
            : null;
        StorageKey key = StorageKey.random(format);
        Media media = mediaRepository.create(new Media(null, key, fileName(upload.originalName(), format),
            bytes.length, dimensions, Media.normalizeAltText(upload.altText()), clock.instant()));
        mediaStorage.store(key, bytes);
        return media;
    }

    /**
     * Dernier segment du nom annoncé (certains navigateurs envoient un chemin), sans espaces superflus, tronqué
     * à 255 caractères ; un nom vide devient {@code media.<extension>}.
     */
    static String fileName(String announced, MediaFormat format) {
        String name = announced == null ? "" : announced.substring(
            Math.max(announced.lastIndexOf('/'), announced.lastIndexOf('\\')) + 1).strip();
        if (name.isEmpty()) {
            return "media." + format.extension();
        }
        return name.length() > Media.MAX_ORIGINAL_NAME_LENGTH ? name.substring(0, Media.MAX_ORIGINAL_NAME_LENGTH) : name;
    }

    private static byte[] readAtMost(InputStream content, long limit) {
        try {
            return content.readNBytes(Math.toIntExact(limit));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the uploaded file", e);
        }
    }
}
