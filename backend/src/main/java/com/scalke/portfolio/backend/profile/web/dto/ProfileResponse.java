package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;

import java.util.List;

public record ProfileResponse(
  String displayName,
  String professionalTitle,
  String shortBio,
  String aboutMarkdown,
  String publicLocation,
  String publicEmail,
  List<ProfessionalLinkResponse> links
){
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
            ).toList()
        );
    }
}
