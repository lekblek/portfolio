package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.infrastructure.storage.MediaStorageProperties;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.scalke.portfolio.backend.media.MediaSamples.PNG;
import static com.scalke.portfolio.backend.project.ProjectFixtures.published;
import static com.scalke.portfolio.backend.project.ProjectFixtures.withImages;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Suppression contrôlée (invariant 13, D-BV) sur PostgreSQL et le stockage réel. Pas de transaction de test :
 * une suppression refusée par une clé étrangère interrompt la transaction PostgreSQL, qui ne pourrait plus
 * servir aux vérifications ; les données créées sont supprimées après chaque test.
 */
class DeleteMediaUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    DeleteMediaUseCase deleteMediaUseCase;

    @Autowired
    UploadMediaUseCase uploadMediaUseCase;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    MediaStorageProperties properties;

    @Autowired
    JdbcClient jdbcClient;

    private final List<Path> files = new ArrayList<>();

    @AfterEach
    void deleteWhatWasCreated() throws IOException {
        jdbcClient.sql("DELETE FROM project").update();
        jdbcClient.sql("DELETE FROM media").update();
        for (Path file : files) {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void deletes_an_unused_media_and_its_file() {
        Media media = upload();

        deleteMediaUseCase.execute(media.id());

        assertThat(count()).isZero();
        assertThat(file(media)).doesNotExist();
    }

    /**
     * Invariant 13 : la suppression est refusée et rien ne disparaît, ni l'entrée ni le fichier.
     */
    @Test
    void refuses_to_delete_a_media_still_used_by_a_project() {
        Media cover = upload();
        projectRepository.create(withImages(published("avec-couverture", LocalDate.of(2026, 1, 1), 0), cover.id()));

        assertThatThrownBy(() -> deleteMediaUseCase.execute(cover.id()))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception -> {
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.MEDIA_STILL_REFERENCED);
                assertThat(exception).hasMessage(
                    "Ce média est encore utilisé : retirez-le d'abord des contenus qui l'affichent.");
            });
        assertThat(count()).isEqualTo(1);
        assertThat(file(cover)).exists();
    }

    @Test
    void fails_for_an_unknown_media() {
        assertThatThrownBy(() -> deleteMediaUseCase.execute(999_999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Média introuvable.");
    }

    private Media upload() {
        Media media = uploadMediaUseCase.execute(new MediaUpload("image.png", null, new ByteArrayInputStream(PNG)));
        files.add(file(media));
        return media;
    }

    private Path file(Media media) {
        return properties.storageRoot().resolve(media.storageKey().value());
    }

    private long count() {
        return jdbcClient.sql("SELECT count(*) FROM media").query(Long.class).single();
    }
}
