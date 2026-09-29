package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.infrastructure.storage.MediaStorageProperties;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static com.scalke.portfolio.backend.media.MediaSamples.PNG;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * D-DE : le catalogue (PostgreSQL) et les fichiers restent cohérents quand la transaction échoue. Le test ouvre
 * lui-même la transaction englobante et décide de son issue ; le cas d'usage la rejoint. Pas de transaction de test :
 * seul un vrai commit ou un vrai rollback déclenche les actions différées. Le cas validé est couvert par
 * {@code DeleteMediaUseCaseIT} et {@code UploadMediaUseCaseIT}.
 */
class MediaFileConsistencyIT extends AbstractIntegrationTest {

    @Autowired
    UploadMediaUseCase uploadMediaUseCase;

    @Autowired
    DeleteMediaUseCase deleteMediaUseCase;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    MediaStorageProperties properties;

    @Autowired
    JdbcClient jdbcClient;

    @AfterEach
    void deleteWhatWasCreated() throws IOException {
        for (String key : jdbcClient.sql("SELECT storage_key FROM media").query(String.class).list()) {
            Files.deleteIfExists(file(key));
        }
        jdbcClient.sql("DELETE FROM media").update();
    }

    /**
     * Envoi annulé après l'écriture du fichier (ici, par la transaction englobante) : ni ligne, ni fichier orphelin.
     */
    @Test
    void a_rolled_back_upload_leaves_no_file() throws IOException {
        long filesBefore = storedFiles();

        String key = transactionTemplate.execute(status -> {
            Media media = uploadMediaUseCase.execute(new MediaUpload("logo.png", null, new ByteArrayInputStream(PNG)));
            assertThat(file(media.storageKey().value())).exists();
            status.setRollbackOnly();
            return media.storageKey().value();
        });

        assertThat(count()).isZero();
        assertThat(file(key)).doesNotExist();
        assertThat(storedFiles()).isEqualTo(filesBefore);
    }

    /**
     * Suppression annulée après l'effacement de la ligne : la ligne revient, le fichier ne doit jamais avoir disparu.
     */
    @Test
    void a_rolled_back_deletion_keeps_the_file() {
        Media media = uploadMediaUseCase.execute(new MediaUpload("logo.png", null, new ByteArrayInputStream(PNG)));

        transactionTemplate.executeWithoutResult(status -> {
            deleteMediaUseCase.execute(media.id());
            assertThat(file(media.storageKey().value())).exists();
            status.setRollbackOnly();
        });

        assertThat(count()).isEqualTo(1);
        assertThat(file(media.storageKey().value())).exists();
    }

    private Path file(String key) {
        return properties.storageRoot().resolve(key);
    }

    private long storedFiles() throws IOException {
        if (!Files.isDirectory(properties.storageRoot())) {
            return 0;
        }
        try (Stream<Path> files = Files.list(properties.storageRoot())) {
            return files.count();
        }
    }

    private long count() {
        return jdbcClient.sql("SELECT count(*) FROM media").query(Long.class).single();
    }
}
