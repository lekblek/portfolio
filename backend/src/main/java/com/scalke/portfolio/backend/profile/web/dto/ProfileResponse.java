package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Représentation publique du profil ({@code GET /api/public/profile}).
 * <p>
 * Tous les tableaux sont déjà triés dans l'ordre d'affichage et valent {@code []} lorsqu'ils sont vides ;
 * les champs optionnels sont présents avec {@code null} (C10).
 */
public record ProfileResponse(
    String displayName,
    String professionalTitle,
    String shortBio,
    String aboutMarkdown,
    String publicLocation,
    String publicEmail,
    List<ProfessionalLinkResponse> links,
    List<SkillGroupResponse> skillGroups,
    List<ExperienceResponse> experiences,
    List<EducationResponse> educations,
    List<CertificationResponse> certifications
) {

    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
            profile.displayName(),
            profile.professionalTitle(),
            profile.shortBio(),
            profile.aboutMarkdown(),
            profile.publicLocation(),
            profile.publicEmail(),
            profile.links().stream().map(ProfessionalLinkResponse::from).toList(),
            groupSkillsByCategory(profile.skills()),
            profile.experiences().stream().map(ExperienceResponse::from).toList(),
            profile.educations().stream().map(EducationResponse::from).toList(),
            profile.certifications().stream().map(CertificationResponse::from).toList());
    }

    /**
     * Groupes dans l'ordre de première apparition, compétences dans l'ordre d'affichage.
     */
    private static List<SkillGroupResponse> groupSkillsByCategory(List<Skill> skills) {
        return skills.stream()
            .collect(Collectors.groupingBy(
                Skill::category,
                LinkedHashMap::new,
                Collectors.mapping(SkillResponse::from, Collectors.toList())))
            .entrySet().stream()
            .map(entry -> new SkillGroupResponse(entry.getKey(), entry.getValue()))
            .toList();
    }
}
