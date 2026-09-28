package com.scalke.portfolio.backend.profile.infrastructure.seed;

import com.scalke.portfolio.backend.media.application.usecase.MediaUpload;
import com.scalke.portfolio.backend.media.application.usecase.UploadMediaUseCase;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.profile.domain.model.Certification;
import com.scalke.portfolio.backend.profile.domain.model.Education;
import com.scalke.portfolio.backend.profile.domain.model.Experience;
import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import com.scalke.portfolio.backend.profile.domain.port.ProfileRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import java.time.LocalDate;
import java.util.List;

/**
 * Données de démonstration du profil `dev`, créées uniquement si la base ne contient aucun profil.
 * Ce ne sont pas des données réelles (voir docs/01-perimetre-v1.md §20).
 * <p>
 * Avatar (PNG uni) et CV (PDF d'une page blanche) sont générés à la volée et envoyés par le cas d'usage du
 * module {@code media}, dans la même transaction que le profil : aucun fichier binaire dans le dépôt.
 */
@Component
@org.springframework.context.annotation.Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class ProfileSeeder implements ApplicationRunner {

    private final ProfileRepository profileRepository;
    private final UploadMediaUseCase uploadMediaUseCase;

    /**
     * Version minimale d'un PDF d'une page blanche : la validation n'exige que la signature (D-BR).
     */
    private static final String DEMO_CV = """
        %PDF-1.4
        1 0 obj <</Type /Catalog /Pages 2 0 R>> endobj
        2 0 obj <</Type /Pages /Kids [3 0 R] /Count 1>> endobj
        3 0 obj <</Type /Page /Parent 2 0 R /MediaBox [0 0 595 842]>> endobj
        trailer <</Root 1 0 R>>
        %%EOF
        """;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (profileRepository.find().isPresent()) {
            return;
        }
        Media avatar = upload("avatar.png", "Avatar de démonstration", demoAvatar());
        Media cv = upload("cv.pdf", null, DEMO_CV.getBytes(StandardCharsets.US_ASCII));
        profileRepository.save(demoProfile(avatar.id(), cv.id()));
        log.info("Profil de démonstration créé (profil dev)");
    }

    private static Profile demoProfile(Long avatarMediaId, Long cvMediaId) {
        return new Profile(
            "Blek Gedeon Ngossanga",
            "Développeur full-stack",
            "Je conçois des solutions modernes et évolutives.",
            null,
            null,
            null,
            List.of(
                new ProfessionalLink(null, "GitHub", "https://example.test/gh", 0),
                new ProfessionalLink(null, "LinkedIn", "https://example.test/in", 1)),
            List.of(
                new Skill(null, "Angular", "Frontend", 0),
                new Skill(null, "Spring Boot", "Backend", 1)),
            List.of(
                new Experience(null, "Organisation de démonstration", "Développeur full-stack", "Tanger",
                    DateRange.ongoingSince(LocalDate.of(2024, 1, 1)), "Expérience de démonstration.", 0)),
            List.of(
                new Education(null, "École de démonstration", "Diplôme d'ingénieur", "Informatique", "Tanger",
                    DateRange.between(LocalDate.of(2018, 9, 1), LocalDate.of(2023, 6, 30)),
                    "Formation de démonstration.", 0)),
            List.of(
                new Certification(null, "Certification de démonstration", "Émetteur",
                    LocalDate.of(2025, 1, 1), null, null, 0)),
            avatarMediaId,
            cvMediaId);
    }

    private Media upload(String name, String altText, byte[] content) {
        return uploadMediaUseCase.execute(new MediaUpload(name, altText, new ByteArrayInputStream(content)));
    }

    private static byte[] demoAvatar() {
        BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(0x37474F));
        graphics.fillRect(0, 0, 400, 400);
        graphics.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return png.toByteArray();
    }
}
