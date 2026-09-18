package com.scalke.portfolio.backend.profile.domain.model;

import java.util.List;


public record Profile(
  String displayName,
  String professionalTitle,
  String shortBio,
  String aboutMarkdown,
  String publicLocation,
  String publicEmail,
  List<ProfessionalLink> links,
  List<Skill> skills
){

    public static Profile create(
        String displayName,
        String professionalTitle,
        String shortBio,
        String publicEmail,
        List<ProfessionalLink> links,
        List<Skill> skills
    ){
        return new Profile(
            displayName,
            professionalTitle,
            shortBio,
            null,
            null,
            publicEmail,
            links,
            skills
        );
    }
}
