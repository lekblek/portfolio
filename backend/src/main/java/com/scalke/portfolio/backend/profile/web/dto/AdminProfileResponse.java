package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.Certification;
import com.scalke.portfolio.backend.profile.domain.model.Education;
import com.scalke.portfolio.backend.profile.domain.model.Experience;
import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Profil vu par l'administration (D-CY) : la forme même de {@link SaveProfileRequest}, pour qu'un formulaire relise
 * et renvoie ce qu'il a reçu ; médias par identifiant ; collections dans leur ordre d'affichage, sans identifiant
 * (elles sont remplacées d'un bloc).
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
    List<SaveProfileRequest.Link> links,
    List<SaveProfileRequest.Skill> skills,
    List<SaveProfileRequest.Experience> experiences,
    List<SaveProfileRequest.Education> educations,
    List<SaveProfileRequest.Certification> certifications
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

    private static SaveProfileRequest.Link link(ProfessionalLink link) {
        return new SaveProfileRequest.Link(link.label(), link.url());
    }

    private static SaveProfileRequest.Skill skill(Skill skill) {
        return new SaveProfileRequest.Skill(skill.name(), skill.category());
    }

    private static SaveProfileRequest.Experience experience(Experience experience) {
        return new SaveProfileRequest.Experience(experience.organization(), experience.title(), experience.location(),
            experience.period().startDate(), experience.period().endDate(), experience.description());
    }

    private static SaveProfileRequest.Education education(Education education) {
        return new SaveProfileRequest.Education(education.institution(), education.degree(), education.field(),
            education.location(), education.period().startDate(), education.period().endDate(),
            education.description());
    }

    private static SaveProfileRequest.Certification certification(Certification certification) {
        return new SaveProfileRequest.Certification(certification.name(), certification.issuer(),
            certification.issuedAt(), certification.expiresAt(), certification.credentialUrl());
    }
}
