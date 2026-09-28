package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.InMemoryMediaStorage;
import com.scalke.portfolio.backend.media.domain.model.MediaFormat;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.shared.error.ContentTooLargeException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.UnsupportedContentException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.scalke.portfolio.backend.media.MediaSamples.JPEG;
import static com.scalke.portfolio.backend.media.MediaSamples.PDF;
import static com.scalke.portfolio.backend.media.MediaSamples.PNG;
import static com.scalke.portfolio.backend.media.MediaSamples.ofSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validation d'un envoi (D-BR) : format par signature, taille par format, lecture bornée.
 */
class UploadMediaUseCaseTest {

    private static final int MIB = 1024 * 1024;

    private final InMemoryMediaStorage storage = new InMemoryMediaStorage();
    private final UploadMediaUseCase uploadMediaUseCase = new UploadMediaUseCase(storage);

    @Test
    void stores_an_accepted_file_under_a_new_key_of_its_format() {
        StorageKey key = uploadMediaUseCase.execute(new ByteArrayInputStream(PNG));

        assertThat(key.format()).isEqualTo(MediaFormat.PNG);
        assertThat(storage.files()).containsOnlyKeys(key);
        assertThat(storage.files().get(key)).containsExactly(PNG);
    }

    /**
     * Le nom ou le type annoncé n'entrent pas en jeu : un SVG ou un fichier vide est refusé sur son contenu.
     */
    @Test
    void refuses_a_file_whose_content_is_not_an_accepted_format() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8);

        assertUnsupported(svg);
        assertUnsupported(new byte[0]);
        assertThat(storage.files()).isEmpty();
    }

    @Test
    void accepts_a_file_at_the_limit_of_its_format() {
        assertThat(uploadMediaUseCase.execute(new ByteArrayInputStream(ofSize(JPEG, 5 * MIB))).format())
            .isEqualTo(MediaFormat.JPEG);
        assertThat(uploadMediaUseCase.execute(new ByteArrayInputStream(ofSize(PDF, 10 * MIB))).format())
            .isEqualTo(MediaFormat.PDF);
    }

    /**
     * {@code 01} §12 : un PDF de 6 Mio est accepté, une image de 6 Mio non.
     */
    @Test
    void refuses_a_file_larger_than_the_limit_of_its_format() {
        assertThatThrownBy(() -> uploadMediaUseCase.execute(new ByteArrayInputStream(ofSize(PNG, 5 * MIB + 1))))
            .isInstanceOfSatisfying(ContentTooLargeException.class, exception -> {
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.MEDIA_TOO_LARGE);
                assertThat(exception).hasMessage("Fichier trop volumineux : 5 Mo au plus pour ce format.");
            });
        assertThat(uploadMediaUseCase.execute(new ByteArrayInputStream(ofSize(PDF, 6 * MIB))).format())
            .isEqualTo(MediaFormat.PDF);
        assertThatThrownBy(() -> uploadMediaUseCase.execute(new ByteArrayInputStream(ofSize(PDF, 10 * MIB + 1))))
            .isInstanceOf(ContentTooLargeException.class)
            .hasMessage("Fichier trop volumineux : 10 Mo au plus pour ce format.");
        assertThat(storage.files()).hasSize(1);
    }

    /**
     * Un flux sans fin n'est lu que jusqu'à 10 Mio + 1 octet : la mémoire consommée reste bornée.
     */
    @Test
    void never_reads_more_than_the_largest_accepted_size_plus_one_byte() {
        EndlessPdf endless = new EndlessPdf();

        assertThatThrownBy(() -> uploadMediaUseCase.execute(endless)).isInstanceOf(ContentTooLargeException.class);
        assertThat(endless.read).isEqualTo(10L * MIB + 1);
    }

    private void assertUnsupported(byte[] content) {
        assertThatThrownBy(() -> uploadMediaUseCase.execute(new ByteArrayInputStream(content)))
            .isInstanceOfSatisfying(UnsupportedContentException.class, exception -> {
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_FORMAT);
                assertThat(exception).hasMessage("Format de fichier non accepté : PNG, JPEG, WebP ou PDF.");
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
