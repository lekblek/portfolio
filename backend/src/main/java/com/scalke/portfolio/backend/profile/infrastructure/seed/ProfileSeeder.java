package com.scalke.portfolio.backend.profile.infrastructure.seed;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.profile.application.usecase.ProfileDraft;
import com.scalke.portfolio.backend.profile.application.usecase.UpdateProfileUseCase;
import com.scalke.portfolio.backend.profile.domain.model.Certification;
import com.scalke.portfolio.backend.profile.domain.model.Education;
import com.scalke.portfolio.backend.profile.domain.model.Experience;
import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import com.scalke.portfolio.backend.profile.domain.port.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

/**
 * Profil de démonstration du profil {@code dev}, décrit par {@code dev-seed/profile.json} (D-EU) : avatar, CV,
 * présentation Markdown, liens, compétences par catégorie, expériences, formations et certifications, assez pour
 * éprouver la page À propos. Ce ne sont pas des données réelles (voir docs/01-perimetre-v1.md §20).
 * <p>
 * Sans profil, il est créé entier. Un profil existant n'est que <strong>complété</strong> : un champ facultatif vide
 * ou une liste vide reçoit sa valeur de démonstration ; une liste restée identique à l'ancien profil minimal de
 * démonstration est remplacée ; rien d'autre n'est modifié, une saisie manuelle est donc préservée. L'écriture passe
 * par le cas d'usage de l'administration (mêmes vérifications, avatar image et CV PDF compris).
 */
@Component
@org.springframework.context.annotation.Profile("dev")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ProfileSeeder implements ApplicationRunner {

    static final String SOURCE = "dev-seed/profile.json";

    /**
     * Listes de l'ancien profil minimal de démonstration (avant F27), remplacées par le jeu complet.
     */
    private static final List<ProfileDraft.Link> LEGACY_LINKS = List.of(
        new ProfileDraft.Link("GitHub", "https://example.test/gh"),
        new ProfileDraft.Link("LinkedIn", "https://example.test/in"));
    private static final List<ProfileDraft.Skill> LEGACY_SKILLS = List.of(
        new ProfileDraft.Skill("Angular", "Frontend"),
        new ProfileDraft.Skill("Spring Boot", "Backend"));
    private static final String LEGACY_ORGANIZATION = "Organisation de démonstration";
    private static final String LEGACY_INSTITUTION = "École de démonstration";
    private static final String LEGACY_CERTIFICATION = "Certification de démonstration";

    private final ProfileRepository profileRepository;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final MediaQueryService media;
    private final JsonMapper jsonMapper;

    @Override
    public void run(ApplicationArguments args) {
        ClassPathResource source = new ClassPathResource(SOURCE);
        if (!source.exists()) {
            return;
        }
        ProfileDraft demo = read(source);
        Optional<Profile> current = profileRepository.find();
        if (current.isEmpty()) {
            updateProfileUseCase.execute(demo);
            log.info("Profil de démonstration créé (profil dev)");
            return;
        }
        ProfileDraft existing = draftOf(current.get());
        ProfileDraft completed = complete(existing, demo);
        if (!completed.equals(existing)) {
            updateProfileUseCase.execute(completed);
            log.info("Profil complété avec les données de démonstration (profil dev)");
        }
    }

    static ProfileDraft complete(ProfileDraft existing, ProfileDraft demo) {
        return new ProfileDraft(
            existing.displayName(),
            existing.professionalTitle(),
            existing.shortBio(),
            existing.aboutMarkdown() != null ? existing.aboutMarkdown() : demo.aboutMarkdown(),
            existing.publicLocation() != null ? existing.publicLocation() : demo.publicLocation(),
            existing.publicEmail(),
            existing.avatarMediaId() != null ? existing.avatarMediaId() : demo.avatarMediaId(),
            existing.cvMediaId() != null ? existing.cvMediaId() : demo.cvMediaId(),
            replaceable(existing.links(), LEGACY_LINKS) ? demo.links() : existing.links(),
            replaceable(existing.skills(), LEGACY_SKILLS) ? demo.skills() : existing.skills(),
            existing.experiences().isEmpty() || existing.experiences().stream()
                .allMatch(entry -> LEGACY_ORGANIZATION.equals(entry.organization()))
                ? demo.experiences() : existing.experiences(),
            existing.educations().isEmpty() || existing.educations().stream()
                .allMatch(entry -> LEGACY_INSTITUTION.equals(entry.institution()))
                ? demo.educations() : existing.educations(),
            existing.certifications().isEmpty() || existing.certifications().stream()
                .allMatch(entry -> LEGACY_CERTIFICATION.equals(entry.name()))
                ? demo.certifications() : existing.certifications());
    }

    private static <T> boolean replaceable(List<T> existing, List<T> legacy) {
        return existing.isEmpty() || existing.equals(legacy);
    }

    private static ProfileDraft draftOf(Profile profile) {
        return new ProfileDraft(profile.displayName(), profile.professionalTitle(), profile.shortBio(),
            profile.aboutMarkdown(), profile.publicLocation(), profile.publicEmail(), profile.avatarMediaId(),
            profile.cvMediaId(),
            profile.links().stream().map(ProfileSeeder::link).toList(),
            profile.skills().stream().map(ProfileSeeder::skill).toList(),
            profile.experiences().stream().map(ProfileSeeder::experience).toList(),
            profile.educations().stream().map(ProfileSeeder::education).toList(),
            profile.certifications().stream().map(ProfileSeeder::certification).toList());
    }

    private static ProfileDraft.Link link(ProfessionalLink link) {
        return new ProfileDraft.Link(link.label(), link.url());
    }

    private static ProfileDraft.Skill skill(Skill skill) {
        return new ProfileDraft.Skill(skill.name(), skill.category());
    }

    private static ProfileDraft.Experience experience(Experience entry) {
        return new ProfileDraft.Experience(entry.organization(), entry.title(), entry.location(),
            entry.period().startDate(), entry.period().endDate(), entry.description());
    }

    private static ProfileDraft.Education education(Education entry) {
        return new ProfileDraft.Education(entry.institution(), entry.degree(), entry.field(), entry.location(),
            entry.period().startDate(), entry.period().endDate(), entry.description());
    }

    private static ProfileDraft.Certification certification(Certification entry) {
        return new ProfileDraft.Certification(entry.name(), entry.issuer(), entry.issuedAt(), entry.expiresAt(),
            entry.credentialUrl());
    }

    /**
     * Le fichier, avec l'avatar et le CV retrouvés parmi les médias de démonstration (absents : aucun).
     */
    private ProfileDraft read(ClassPathResource source) {
        DemoProfile demo;
        try (InputStream content = source.getInputStream()) {
            demo = jsonMapper.readValue(content, DemoProfile.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new ProfileDraft(demo.displayName(), demo.professionalTitle(), demo.shortBio(), demo.aboutMarkdown(),
            demo.publicLocation(), null, mediaId(demo.avatar()), mediaId(demo.cv()), demo.links(), demo.skills(),
            demo.experiences(), demo.educations(), demo.certifications());
    }

    private Long mediaId(String file) {
        return file == null ? null : media.findByOriginalName(file).map(Media::id).orElse(null);
    }

    record DemoProfile(String displayName, String professionalTitle, String shortBio, String aboutMarkdown,
                       String publicLocation, String avatar, String cv, List<ProfileDraft.Link> links,
                       List<ProfileDraft.Skill> skills, List<ProfileDraft.Experience> experiences,
                       List<ProfileDraft.Education> educations, List<ProfileDraft.Certification> certifications) {
    }
}
