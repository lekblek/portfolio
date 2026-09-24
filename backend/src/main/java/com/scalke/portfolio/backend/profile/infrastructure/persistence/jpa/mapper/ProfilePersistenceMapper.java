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
            SkillPersistenceMapper.map(entity.getSkills())
        );
    }

    public static ProfileEntity toEntity(Profile profile) {
        ProfileEntity entity = new ProfileEntity(
            profile.displayName(),
            profile.professionalTitle(),
            profile.shortBio());
        entity.setAboutMarkdown(profile.aboutMarkdown());
        entity.setPublicLocation(profile.publicLocation());
        entity.setPublicEmail(profile.publicEmail());

        profile.links().forEach(link -> entity.addLink(link.label(), link.url(), link.displayOrder()));
        profile.skills().forEach(skill -> entity.addSkill(skill.name(), skill.category(), skill.displayOrder()));

        return entity;
    }
}
