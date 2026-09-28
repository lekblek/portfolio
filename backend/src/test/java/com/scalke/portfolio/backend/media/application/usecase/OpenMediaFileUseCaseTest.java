package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.MediaContent;
import com.scalke.portfolio.backend.media.domain.model.MediaFormat;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.media.domain.port.MediaStorage;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cas d'usage sans base de données (D-BQ) : testé en unitaire, avec un stockage en mémoire.
 */
class OpenMediaFileUseCaseTest {

    private static final String KEY = "3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.webp";

    private final List<StorageKey> opened = new ArrayList<>();

    @Test
    void serves_a_stored_file_with_the_format_of_its_key() {
        MediaStorage storage = key -> {
            opened.add(key);
            return Optional.of(new MediaContent(2, new ByteArrayInputStream(new byte[]{1, 2})));
        };

        MediaFile file = new OpenMediaFileUseCase(storage).execute(KEY);

        assertThat(file.format()).isEqualTo(MediaFormat.WEBP);
        assertThat(file.content().size()).isEqualTo(2);
        assertThat(opened).containsExactly(new StorageKey(KEY));
    }

    @Test
    void fails_for_an_unknown_key() {
        MediaStorage storage = key -> Optional.empty();

        assertThatThrownBy(() -> new OpenMediaFileUseCase(storage).execute(KEY))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Média introuvable.");
    }

    @Test
    void a_malformed_key_is_not_found_without_reaching_the_storage() {
        MediaStorage storage = key -> {
            opened.add(key);
            return Optional.empty();
        };

        assertThatThrownBy(() -> new OpenMediaFileUseCase(storage).execute("../application.yaml"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Média introuvable.");
        assertThat(opened).isEmpty();
    }
}
