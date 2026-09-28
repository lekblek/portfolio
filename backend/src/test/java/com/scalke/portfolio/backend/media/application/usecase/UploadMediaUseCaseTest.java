package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.InMemoryMediaRepository;
import com.scalke.portfolio.backend.media.InMemoryMediaStorage;
import com.scalke.portfolio.backend.media.domain.model.Dimensions;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.model.MediaFormat;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.shared.error.ContentTooLargeException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.UnsupportedContentException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

import static com.scalke.portfolio.backend.media.MediaSamples.JPEG;
import static com.scalke.portfolio.backend.media.MediaSamples.PDF;
import static com.scalke.portfolio.backend.media.MediaSamples.PNG;
import static com.scalke.portfolio.backend.media.MediaSamples.WEBP;
import static com.scalke.portfolio.backend.media.MediaSamples.ofSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validation et inscription d'un envoi (D-BR, D-BU), avec un catalogue et un stockage en mémoire.
 */
class UploadMediaUseCaseTest {

    private static final int MIB = 1024 * 1024;
    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");

    private final InMemoryMediaStorage storage = new InMemoryMediaStorage();
    private final InMemoryMediaRepository repository = new InMemoryMediaRepository();
    private final UploadMediaUseCase uploadMediaUseCase =
        new UploadMediaUseCase(storage, repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void records_and_stores_an_image_with_its_dimensions() {
        Media media = upload("photos/couverture.webp", "  Vue du tableau de bord  ", WEBP);

        assertThat(media.id()).isNotNull();
        assertThat(media.format()).isEqualTo(MediaFormat.WEBP);
        assertThat(media.dimensions()).isEqualTo(new Dimensions(640, 480));
        assertThat(media.originalName()).isEqualTo("couverture.webp");
        assertThat(media.altText()).isEqualTo("Vue du tableau de bord");
        assertThat(media.size()).isEqualTo(WEBP.length);
        assertThat(media.createdAt()).isEqualTo(NOW);
        assertThat(repository.media()).containsExactly(media);
        assertThat(storage.files().get(media.storageKey())).containsExactly(WEBP);
    }

    @Test
    void records_a_pdf_without_dimensions() {
        Media media = upload("cv.pdf", " ", PDF);

        assertThat(media.dimensions()).isNull();
        assertThat(media.altText()).isNull();
        assertThat(media.mimeType()).isEqualTo("application/pdf");
    }

    /**
     * Le nom annoncé peut être un chemin (anciens navigateurs) ou vide.
     */
    @Test
    void keeps_only_the_last_segment_of_the_announced_name() {
        assertThat(UploadMediaUseCase.fileName("C:\\Users\\moi\\cv.pdf", MediaFormat.PDF)).isEqualTo("cv.pdf");
        assertThat(UploadMediaUseCase.fileName("  ", MediaFormat.PNG)).isEqualTo("media.png");
        assertThat(UploadMediaUseCase.fileName(null, MediaFormat.PDF)).isEqualTo("media.pdf");
        assertThat(UploadMediaUseCase.fileName("a".repeat(300), MediaFormat.PDF)).hasSize(255);
    }

    /**
     * Le nom ou le type annoncé n'entrent pas en jeu : un SVG ou un fichier vide est refusé sur son contenu.
     */
    @Test
    void refuses_a_file_whose_content_is_not_an_accepted_format() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8);

        assertUnsupported(svg, "Format de fichier non accepté : PNG, JPEG, WebP ou PDF.");
        assertUnsupported(new byte[0], "Format de fichier non accepté : PNG, JPEG, WebP ou PDF.");
        assertThat(repository.media()).isEmpty();
        assertThat(storage.files()).isEmpty();
    }

    /**
     * D-BU : une signature PNG suivie d'un en-tête illisible n'est pas une image acceptable.
     */
    @Test
    void refuses_an_image_whose_dimensions_cannot_be_read() {
        assertUnsupported(Arrays.copyOf(PNG, 12), "Image illisible : ses dimensions ne peuvent pas être lues.");
        assertThat(repository.media()).isEmpty();
    }

    @Test
    void accepts_a_file_at_the_limit_of_its_format() {
        assertThat(upload("photo.jpg", null, ofSize(JPEG, 5 * MIB)).format()).isEqualTo(MediaFormat.JPEG);
        assertThat(upload("cv.pdf", null, ofSize(PDF, 10 * MIB)).format()).isEqualTo(MediaFormat.PDF);
    }

    /**
     * {@code 01} §12 : un PDF de 6 Mio est accepté, une image de 6 Mio non.
     */
    @Test
    void refuses_a_file_larger_than_the_limit_of_its_format() {
        assertThatThrownBy(() -> upload("photo.png", null, ofSize(PNG, 5 * MIB + 1)))
            .isInstanceOfSatisfying(ContentTooLargeException.class, exception -> {
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.MEDIA_TOO_LARGE);
                assertThat(exception).hasMessage("Fichier trop volumineux : 5 Mo au plus pour ce format.");
            });
        assertThat(upload("cv.pdf", null, ofSize(PDF, 6 * MIB)).format()).isEqualTo(MediaFormat.PDF);
        assertThatThrownBy(() -> upload("cv.pdf", null, ofSize(PDF, 10 * MIB + 1)))
            .isInstanceOf(ContentTooLargeException.class)
            .hasMessage("Fichier trop volumineux : 10 Mo au plus pour ce format.");
        assertThat(repository.media()).hasSize(1);
    }

    /**
     * Un flux sans fin n'est lu que jusqu'à 10 Mio + 1 octet : la mémoire consommée reste bornée.
     */
    @Test
    void never_reads_more_than_the_largest_accepted_size_plus_one_byte() {
        EndlessPdf endless = new EndlessPdf();

        assertThatThrownBy(() -> uploadMediaUseCase.execute(new MediaUpload("cv.pdf", null, endless)))
            .isInstanceOf(ContentTooLargeException.class);
        assertThat(endless.read).isEqualTo(10L * MIB + 1);
    }

    /**
     * D-BU : le média est inscrit avant l'écriture du fichier ; un échec d'écriture remonte (et annule
     * l'inscription, dans la transaction du cas d'usage : voir {@code UploadMediaUseCaseIT}).
     */
    @Test
    void records_the_media_before_storing_its_file() {
        InMemoryMediaStorage failing = new InMemoryMediaStorage() {
            @Override
            public void store(StorageKey key, byte[] content) {
                assertThat(repository.media()).extracting(Media::storageKey).containsExactly(key);
                throw new IllegalStateException("disque plein");
            }
        };
        UploadMediaUseCase useCase = new UploadMediaUseCase(failing, repository, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> useCase.execute(new MediaUpload("cv.pdf", null, new ByteArrayInputStream(PDF))))
            .hasMessage("disque plein");
    }

    private Media upload(String name, String altText, byte[] content) {
        return uploadMediaUseCase.execute(new MediaUpload(name, altText, new ByteArrayInputStream(content)));
    }

    private void assertUnsupported(byte[] content, String message) {
        assertThatThrownBy(() -> upload("fichier", null, content))
            .isInstanceOfSatisfying(UnsupportedContentException.class, exception -> {
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_FORMAT);
                assertThat(exception).hasMessage(message);
            });
    }

    /**
     * Flux infini commençant par la signature PDF ; compte les octets lus.
     */
    private static final class EndlessPdf extends InputStream {

        private long read;

        @Override
        public int read() {
            return read < PDF.length ? PDF[(int) read++] : zero();
        }

        private int zero() {
            read++;
            return 0;
        }
    }
}
