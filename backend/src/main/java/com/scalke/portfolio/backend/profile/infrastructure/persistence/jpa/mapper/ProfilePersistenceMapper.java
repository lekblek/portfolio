package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;

public class ProfilePersistenceMapper {
    private ProfilePersistenceMapper(){}

    public static Profile toDomain(ProfileEntity entity) {
        return new Profile(
            entity.getDisplayName(),
            entity.getProfessionalTitle(),
            entity.getShortBio(),
            entity.getAboutMarkdown(),
            entity.getPublicLocation(),
            entity.getPublicEmail(),
           ProfessionalLinkPersistenceMapper.map(entity.getLinks())
        );
    }
}
