package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;

public final class ProfilePersistenceMapper {

    private ProfilePersistenceMapper() {
    }

    public static Profile toDomain(ProfileEntity entity) {
        return new Profile(
            entity.getDisplayName(),
            entity.getProfessionalTitle(),
            entity.getShortBio(),
            entity.getAboutMarkdown(),
            entity.getPublicLocation(),
            entity.getPublicEmail(),
            ProfessionalLinkPersistenceMapper.map(entity.getLinks()),
            SkillPersistenceMapper.map(entity.getSkills()),
            ExperiencePersistenceMapper.map(entity.getExperiences()),
            EducationPersistenceMapper.map(entity.getEducations()),
            CertificationPersistenceMapper.map(entity.getCertifications()),
            entity.getAvatarMediaId(),
            entity.getCvMediaId());
    }

    public static ProfileEntity toEntity(Profile profile) {
        ProfileEntity entity = new ProfileEntity(
            profile.displayName(),
            profile.professionalTitle(),
            profile.shortBio());
        fill(entity, profile);
        return entity;
    }

    /**
     * Recopie dans l'entité les champs facultatifs, les médias et les collections du profil (ajoutées à celles de
     * l'entité : vides à la création, vidées par l'appelant avant un remplacement, D-CY).
     */
    public static void fill(ProfileEntity entity, Profile profile) {
        entity.setAboutMarkdown(profile.aboutMarkdown());
        entity.setPublicLocation(profile.publicLocation());
        entity.setPublicEmail(profile.publicEmail());
        entity.setAvatarMediaId(profile.avatarMediaId());
        entity.setCvMediaId(profile.cvMediaId());

        profile.links().forEach(link -> entity.addLink(link.label(), link.url(), link.displayOrder()));
        profile.skills().forEach(skill -> entity.addSkill(skill.name(), skill.category(), skill.displayOrder()));
        profile.experiences().forEach(experience ->
            entity.addExperience(ExperiencePersistenceMapper.toEntity(experience)));
        profile.educations().forEach(education ->
            entity.addEducation(EducationPersistenceMapper.toEntity(education)));
        profile.certifications().forEach(certification ->
            entity.addCertification(CertificationPersistenceMapper.toEntity(certification)));
    }
}
