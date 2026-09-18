package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfessionalLinkEntity;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.SkillEntity;

import java.util.List;

public class SkillPersistenceMapper {

    private SkillPersistenceMapper() {}

    public static Skill toDomain(SkillEntity entity) {
        return  new Skill(
            entity.getId(),
            entity.getName(),
            entity.getCategory(),
            entity.getDisplayOrder()
        );
    }

    public static List<Skill> map(List<SkillEntity> entities) {
        return entities.stream().map(
            SkillPersistenceMapper::toDomain
        ).toList();
    }
}
