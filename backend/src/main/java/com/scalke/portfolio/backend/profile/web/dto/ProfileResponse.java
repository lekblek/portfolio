package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;

import java.util.List;

public record ProfileResponse(
  Long id,
  String displayName,
  String professionalTitle,
  String publicEmail,
  List<ProfessionalLink> links
){
    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
            profile.id(),
            profile.professionalTitle(),
            profile.displayName(),
            profile.publicEmail(),
            profile.links()
        );
    }
}
