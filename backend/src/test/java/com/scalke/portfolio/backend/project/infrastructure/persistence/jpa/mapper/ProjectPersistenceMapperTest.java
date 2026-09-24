package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.ProjectEntity;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectPersistenceMapperTest {

    @Test
    void keeps_every_field_through_a_round_trip() {
        Project project = new Project(
            null,
            "Portfolio full-stack",
            "portfolio-full-stack",
            "Résumé",
            "## Description",
            ProjectStage.COMPLETED,
            ProjectVisibility.ARCHIVED,
            DateRange.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)),
            "https://example.test/repo",
            "https://example.test/demo",
            true,
            3);

        ProjectEntity entity = ProjectPersistenceMapper.toNewEntity(project);

        assertThat(entity.getId()).isNull();
        assertThat(ProjectPersistenceMapper.toDomain(entity)).isEqualTo(project);
    }
}
