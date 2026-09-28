package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.MediaFormat;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.media.domain.port.MediaStorage;
import com.scalke.portfolio.backend.shared.error.ContentTooLargeException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.UnsupportedContentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Service
@RequiredArgsConstructor
public class UploadMediaUseCase {

    private static final long MIB = 1024 * 1024;

    private final MediaStorage mediaStorage;

    /**
     * Valide puis stocke un fichier envoyé et renvoie sa nouvelle clé (D-BR, D-BS). Le format est reconnu par
     * la signature du contenu ; la lecture s'arrête au-delà de la plus grande taille acceptée, quelle que soit
     * la taille réelle du flux. Le flux est lu, pas fermé : il appartient à l'appelant.
     * <p>
     * Pas de route HTTP avant l'administration protégée (étape 36, comme D-AU) ; pas de transaction tant que
     * rien n'est écrit en base (D-BQ) : le catalogue (étape 27) ajoutera les métadonnées.
     *
     * @throws UnsupportedContentException format non reconnu ({@code UNSUPPORTED_MEDIA_FORMAT}, 415)
     * @throws ContentTooLargeException fichier trop volumineux pour son format ({@code MEDIA_TOO_LARGE}, 413)
     */
    public StorageKey execute(InputStream content) {
        byte[] bytes = readAtMost(content, MediaFormat.maxAcceptedSize() + 1);
        MediaFormat format = MediaFormat.detect(bytes).orElseThrow(() -> new UnsupportedContentException(
            ErrorCode.UNSUPPORTED_MEDIA_FORMAT, "Format de fichier non accepté : PNG, JPEG, WebP ou PDF."));
        if (bytes.length > format.maxSize()) {
            throw new ContentTooLargeException(ErrorCode.MEDIA_TOO_LARGE,
                "Fichier trop volumineux : " + format.maxSize() / MIB + " Mo au plus pour ce format.");
        }
        StorageKey key = StorageKey.random(format);
        mediaStorage.store(key, bytes);
        return key;
    }

    private static byte[] readAtMost(InputStream content, long limit) {
        try {
            return content.readNBytes(Math.toIntExact(limit));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the uploaded file", e);
        }
    }
}
