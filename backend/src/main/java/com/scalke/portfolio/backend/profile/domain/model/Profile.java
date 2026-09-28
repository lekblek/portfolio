package com.scalke.portfolio.backend.profile.domain.model;

import java.util.List;

/**
 * Agrégat du profil professionnel (singleton en V1). Les listes sont immuables et déjà
 * triées dans l'ordre d'affichage (D-L).
 * <p>
 * Avatar (une image) et CV (un PDF, D06) sont des références facultatives vers le catalogue {@code media},
 * par identifiant (ADR 0002, D-BX) ; PostgreSQL interdit de supprimer un média ainsi référencé (invariant 13).
 */
public record Profile(
    String displayName,
    String professionalTitle,
    String shortBio,
    String aboutMarkdown,
    String publicLocation,
    String publicEmail,
    List<ProfessionalLink> links,
    List<Skill> skills,
    List<Experience> experiences,
    List<Education> educations,
    List<Certification> certifications,
    Long avatarMediaId,
    Long cvMediaId
) {

    public Profile {
        links = List.copyOf(links);
        skills = List.copyOf(skills);
        experiences = List.copyOf(experiences);
        educations = List.copyOf(educations);
        certifications = List.copyOf(certifications);
    }
}
