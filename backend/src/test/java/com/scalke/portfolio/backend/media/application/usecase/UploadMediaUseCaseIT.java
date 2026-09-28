package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.Dimensions;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.infrastructure.storage.MediaStorageProperties;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.scalke.portfolio.backend.media.MediaSamples.PNG;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Envoi réel : catalogue PostgreSQL et stockage local sous {@code target/test-media}. Pas de transaction de
 * test : la transaction du cas d'usage est validée (ou annulée) comme en production ; les lignes et fichiers
 * créés sont supprimés après chaque test.
 */
class UploadMediaUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    UploadMediaUseCase uploadMediaUseCase;

    @Autowired
    MediaStorageProperties properties;

    @Autowired
    JdbcClient jdbcClient;

    private final List<Path> files = new ArrayList<>();

    @AfterEach
    void deleteWhatWasCreated() throws IOException {
        jdbcClient.sql("DELETE FROM media").update();
        for (Path file : files) {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void records_the_media_and_stores_its_file() throws IOException {
        Media media = uploadMediaUseCase.execute(new MediaUpload("capture.png", "Écran d'accueil", new ByteArrayInputStream(PNG)));
        Path file = properties.storageRoot().resolve(media.storageKey().value());
        files.add(file);

        assertThat(media.id()).isNotNull();
        assertThat(media.dimensions()).isEqualTo(new Dimensions(3, 2));
        assertThat(media.createdAt()).isEqualTo(NOW);
        assertThat(Files.readAllBytes(file)).isEqualTo(PNG);
        assertThat(jdbcClient.sql("SELECT storage_key FROM media").query(String.class).list())
            .containsExactly(media.storageKey().value());
    }

    /**
     * D-BU : si le fichier ne peut pas être écrit, l'inscription au catalogue est annulée. La racine du
     * stockage est remplacée par un fichier ordinaire le temps du test.
     */
    @Test
    void a_failed_write_leaves_no_media_in_the_catalogue() throws IOException {
        Path root = properties.storageRoot();
        Path moved = root.resolveSibling(root.getFileName() + "-moved");
        boolean rootExisted = Files.exists(root);
        if (rootExisted) {
            Files.move(root, moved);
        }
        Files.writeString(root, "pas un répertoire");
        try {
            assertThatThrownBy(() -> uploadMediaUseCase.execute(
                new MediaUpload("capture.png", null, new ByteArrayInputStream(PNG))))
                .isInstanceOf(RuntimeException.class);
            assertThat(jdbcClient.sql("SELECT count(*) FROM media").query(Long.class).single()).isZero();
        } finally {
            Files.delete(root);
            if (rootExisted) {
                Files.move(moved, root);
            }
        }
    }
}
