package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public record ProfileResponse(
    String displayName,
    String professionalTitle,
    String shortBio,
    String aboutMarkdown,
    String publicLocation,
    String publicEmail,
    List<ProfessionalLinkResponse> links,
    List<SkillGroupResponse> skillGroups
) {
    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
            profile.displayName(),
            profile.professionalTitle(),
            profile.shortBio(),
            profile.aboutMarkdown(),
            profile.publicLocation(),
            profile.publicEmail(),
            profile.links().stream().map(
                ProfessionalLinkResponse::from
            ).toList(),
            profile.skills().stream()
                .collect(Collectors.groupingBy(
                    Skill::category,
                    LinkedHashMap::new,
                    Collectors.mapping(SkillResponse::from, Collectors.toList())))
                .entrySet().stream()
                .map(entry -> new SkillGroupResponse(entry.getKey(), entry.getValue()))
                .toList()
        );
    }
}
