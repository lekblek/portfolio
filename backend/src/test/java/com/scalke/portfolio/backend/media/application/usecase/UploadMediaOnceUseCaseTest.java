package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.InMemoryMediaRepository;
import com.scalke.portfolio.backend.media.InMemoryMediaStorage;
import com.scalke.portfolio.backend.media.domain.model.Media;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.scalke.portfolio.backend.media.MediaSamples.PNG;
import static com.scalke.portfolio.backend.media.MediaSamples.WEBP;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Médias de démonstration (D-EU) : un nom d'origine déjà présent n'est pas envoyé une seconde fois.
 */
class UploadMediaOnceUseCaseTest {

    private final InMemoryMediaStorage storage = new InMemoryMediaStorage();
    private final InMemoryMediaRepository repository = new InMemoryMediaRepository();
    private final UploadMediaOnceUseCase uploadMediaOnceUseCase = new UploadMediaOnceUseCase(repository,
        new UploadMediaUseCase(storage, repository, new MediaFileLifecycle(storage),
            Clock.fixed(Instant.parse("2026-10-03T10:00:00Z"), ZoneOffset.UTC)));

    @BeforeEach
    void openTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void closeTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void uploads_a_new_name_then_reuses_it() {
        Media first = uploadMediaOnceUseCase.execute(new MediaUpload("couverture.webp", "Couverture",
            new ByteArrayInputStream(WEBP)));
        Media again = uploadMediaOnceUseCase.execute(new MediaUpload("couverture.webp", "Autre texte",
            new ByteArrayInputStream(PNG)));
        Media other = uploadMediaOnceUseCase.execute(new MediaUpload("capture.png", null,
            new ByteArrayInputStream(PNG)));

        assertThat(again).isEqualTo(first);
        assertThat(other.id()).isNotEqualTo(first.id());
        assertThat(repository.media()).containsExactly(first, other);
        assertThat(storage.files()).hasSize(2);
    }
}
