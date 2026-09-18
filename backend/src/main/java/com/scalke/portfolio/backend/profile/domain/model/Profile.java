package com.scalke.portfolio.backend.profile.domain.model;

import java.util.List;

public record Profile(
  Long id,
  String displayName,
  String professionalTitle,
  String publicEmail,
  List<ProfessionalLink> links
){}
