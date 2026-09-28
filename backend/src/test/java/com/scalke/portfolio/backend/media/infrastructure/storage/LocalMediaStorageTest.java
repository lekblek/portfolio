package com.scalke.portfolio.backend.media.infrastructure.storage;

import com.scalke.portfolio.backend.media.domain.model.MediaContent;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalMediaStorageTest {

    private static final StorageKey KEY = new StorageKey("3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.png");

    @TempDir
    Path root;

    @Test
    void opens_the_file_stored_under_a_key() throws IOException {
        Files.write(root.resolve(KEY.value()), new byte[]{1, 2, 3});

        MediaContent content = storage().open(KEY).orElseThrow();

        assertThat(content.size()).isEqualTo(3);
        try (InputStream stream = content.stream()) {
            assertThat(stream.readAllBytes()).containsExactly(1, 2, 3);
        }
    }

    @Test
    void finds_nothing_for_an_unknown_key() {
        assertThat(storage().open(KEY)).isEmpty();
    }

    @Test
    void finds_nothing_when_the_root_does_not_exist_yet() {
        LocalMediaStorage storage = new LocalMediaStorage(new MediaStorageProperties(root.resolve("absent")));

        assertThat(storage.open(KEY)).isEmpty();
    }

    @Test
    void ignores_a_directory_named_like_a_key() throws IOException {
        Files.createDirectory(root.resolve(KEY.value()));

        assertThat(storage().open(KEY)).isEmpty();
    }

    /**
     * Une racine relative est résolue depuis le répertoire de travail, puis normalisée.
     */
    @Test
    void resolves_a_relative_root_from_the_working_directory() throws IOException {
        Path relative = Path.of("").toAbsolutePath().relativize(root);
        Files.write(root.resolve(KEY.value()), new byte[]{7});

        LocalMediaStorage storage = new LocalMediaStorage(new MediaStorageProperties(relative));

        assertThat(storage.open(KEY)).get().extracting(MediaContent::size).isEqualTo(1L);
    }

    @Test
    void refuses_an_empty_root() {
        assertThatThrownBy(() -> new MediaStorageProperties(Path.of("")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private LocalMediaStorage storage() {
        return new LocalMediaStorage(new MediaStorageProperties(root));
    }
}
