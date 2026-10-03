package com.scalke.portfolio.backend.media.infrastructure.seed;

import com.scalke.portfolio.backend.media.application.usecase.MediaUpload;
import com.scalke.portfolio.backend.media.application.usecase.UploadMediaOnceUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Médias de démonstration du profil {@code dev} (D-EU) : chaque fichier de {@code dev-seed/media/} (photographies
 * libres de droits, captures d'interfaces fictives, CV de démonstration ; provenance dans {@code SOURCES.md}) est
 * envoyé dans la médiathèque par le cas d'usage d'envoi, sauf si un média porte déjà son nom. Exécuté en premier :
 * le profil, les projets et les publications de démonstration retrouvent ensuite ces médias par leur nom.
 * <p>
 * Les fichiers sont exclus du jar ({@code pom.xml}) : sans eux, rien n'est envoyé et les contenus de démonstration
 * sont créés sans images.
 */
@Component
@Profile("dev")
@Order(-1)
@RequiredArgsConstructor
@Slf4j
public class DemoMediaSeeder implements ApplicationRunner {

    static final String ROOT = "dev-seed/media/";

    private final UploadMediaOnceUseCase uploadMediaOnceUseCase;
    private final JsonMapper jsonMapper;

    @Override
    public void run(ApplicationArguments args) {
        ClassPathResource manifest = new ClassPathResource(ROOT + "media.json");
        if (!manifest.exists()) {
            log.warn("Médias de démonstration absents ({}) : contenus de démonstration sans images", ROOT);
            return;
        }
        List<DemoMedia> files = read(manifest);
        for (DemoMedia file : files) {
            try (InputStream content = new ClassPathResource(ROOT + file.file()).getInputStream()) {
                uploadMediaOnceUseCase.execute(new MediaUpload(file.file(), file.altText(), content));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        log.info("{} médias de démonstration présents dans la médiathèque (profil dev)", files.size());
    }

    private List<DemoMedia> read(ClassPathResource manifest) {
        try (InputStream content = manifest.getInputStream()) {
            return jsonMapper.readValue(content, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Ligne de {@code media.json} : nom du fichier (et nom d'origine du média), texte alternatif d'une image.
     */
    record DemoMedia(String file, String altText) {
    }
}
