package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.InMemoryMediaStorage;
import com.scalke.portfolio.backend.media.domain.model.MediaFormat;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cas d'usage sans base de données (D-BQ) : testé en unitaire, avec un stockage en mémoire.
 */
class OpenMediaFileUseCaseTest {

    private static final StorageKey KEY = new StorageKey("3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.webp");

    private final InMemoryMediaStorage storage = new InMemoryMediaStorage();
    private final OpenMediaFileUseCase openMediaFileUseCase = new OpenMediaFileUseCase(storage);

    @Test
    void serves_a_stored_file_with_the_format_of_its_key() {
        storage.store(KEY, new byte[]{1, 2});

        MediaFile file = openMediaFileUseCase.execute(KEY.value());

        assertThat(file.format()).isEqualTo(MediaFormat.WEBP);
        assertThat(file.content().size()).isEqualTo(2);
    }

    @Test
    void fails_for_an_unknown_key() {
        assertThatThrownBy(() -> openMediaFileUseCase.execute(KEY.value()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Média introuvable.");
    }

    @Test
    void a_malformed_key_is_not_found_without_reaching_the_storage() {
        assertThatThrownBy(() -> openMediaFileUseCase.execute("../application.yaml"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Média introuvable.");
        assertThat(storage.opened()).isEmpty();
    }
}
