package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfessionalLinkEntity;

import java.util.List;

public class ProfessionalLinkPersistenceMapper {

    private ProfessionalLinkPersistenceMapper() {}

    public static ProfessionalLink toDomain(ProfessionalLinkEntity entity) {
        return  new ProfessionalLink(
            entity.getId(),
            entity.getLabel(),
            entity.getUrl(),
            entity.getDisplayOrder()
        );
    }

    public static List<ProfessionalLink> map(List<ProfessionalLinkEntity> entities) {
        return entities.stream().map(
            ProfessionalLinkPersistenceMapper::toDomain
        ).toList();
    }
}
