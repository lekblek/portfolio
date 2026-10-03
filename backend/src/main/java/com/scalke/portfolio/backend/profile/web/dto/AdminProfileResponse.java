package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.Certification;
import com.scalke.portfolio.backend.profile.domain.model.Education;
import com.scalke.portfolio.backend.profile.domain.model.Experience;
import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

/**
 * Profil vu par l'administration (D-CY) : la forme même de {@link SaveProfileRequest}, pour qu'un formulaire relise
 * et renvoie ce qu'il a reçu ; médias par identifiant ; collections dans leur ordre d'affichage, sans identifiant
 * (elles sont remplacées d'un bloc).
 *
 * <p>Les records internes sont distincts de ceux de {@link SaveProfileRequest} pour que le contrat OpenAPI différencie
 * les schémas de requête (champs optionnels de validation) des schémas de réponse (champs toujours présents) (KI-36,
 * D-ES).</p>
 */
public record AdminProfileResponse(
    String displayName,
    String professionalTitle,
    String shortBio,
    @Nullable String aboutMarkdown,
    @Nullable String publicLocation,
    @Nullable String publicEmail,
    @Nullable Long avatarMediaId,
    @Nullable Long cvMediaId,
    List<AdminLink> links,
    List<AdminSkill> skills,
    List<AdminExperience> experiences,
    List<AdminEducation> educations,
    List<AdminCertification> certifications
) {

    public static AdminProfileResponse from(Profile profile) {
        return new AdminProfileResponse(
            profile.displayName(),
            profile.professionalTitle(),
            profile.shortBio(),
            profile.aboutMarkdown(),
            profile.publicLocation(),
            profile.publicEmail(),
            profile.avatarMediaId(),
            profile.cvMediaId(),
            profile.links().stream().map(AdminProfileResponse::link).toList(),
            profile.skills().stream().map(AdminProfileResponse::skill).toList(),
            profile.experiences().stream().map(AdminProfileResponse::experience).toList(),
            profile.educations().stream().map(AdminProfileResponse::education).toList(),
            profile.certifications().stream().map(AdminProfileResponse::certification).toList());
    }

    private static AdminLink link(ProfessionalLink link) {
        return new AdminLink(link.label(), link.url());
    }

    private static AdminSkill skill(Skill skill) {
        return new AdminSkill(skill.name(), skill.category());
    }

    private static AdminExperience experience(Experience experience) {
        return new AdminExperience(experience.organization(), experience.title(), experience.location(),
            experience.period().startDate(), experience.period().endDate(), experience.description());
    }

    private static AdminEducation education(Education education) {
        return new AdminEducation(education.institution(), education.degree(), education.field(),
            education.location(), education.period().startDate(), education.period().endDate(),
            education.description());
    }

    private static AdminCertification certification(Certification certification) {
        return new AdminCertification(certification.name(), certification.issuer(),
            certification.issuedAt(), certification.expiresAt(), certification.credentialUrl());
    }

    @Schema(name = "AdminLink")
    public record AdminLink(String label, String url) {}

    @Schema(name = "AdminSkill")
    public record AdminSkill(String name, String category) {}

    @Schema(name = "AdminExperience")
    public record AdminExperience(
        String organization,
        String title,
        String location,
        LocalDate startDate,
        @Nullable LocalDate endDate,
        String description
    ) {}

    @Schema(name = "AdminEducation")
    public record AdminEducation(
        String institution,
        String degree,
        String field,
        String location,
        LocalDate startDate,
        @Nullable LocalDate endDate,
        String description
    ) {}

    @Schema(name = "AdminCertification")
    public record AdminCertification(
        String name,
        String issuer,
        LocalDate issuedAt,
        @Nullable LocalDate expiresAt,
        @Nullable String credentialUrl
    ) {}
}
