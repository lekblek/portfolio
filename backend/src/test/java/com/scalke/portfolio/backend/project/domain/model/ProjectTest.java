package com.scalke.portfolio.backend.project.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectTest {

    private static final DateRange ONGOING = DateRange.ongoingSince(LocalDate.of(2024, 1, 1));
    private static final DateRange ENDED = DateRange.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

    @Test
    void an_in_progress_project_has_no_end_date() {
        assertThat(project(ProjectStage.IN_PROGRESS, ONGOING).period().isOngoing()).isTrue();
    }

    @Test
    void a_completed_project_has_an_end_date() {
        assertThat(project(ProjectStage.COMPLETED, ENDED).period().endDate()).isEqualTo(LocalDate.of(2024, 6, 30));
    }

    @Test
    void rejects_an_in_progress_project_with_an_end_date() {
        assertThatThrownBy(() -> project(ProjectStage.IN_PROGRESS, ENDED))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_a_completed_project_without_an_end_date() {
        assertThatThrownBy(() -> project(ProjectStage.COMPLETED, ONGOING))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requires_a_visibility() {
        assertThatThrownBy(() -> new Project(null, "Titre", "titre", "Résumé", "# Titre",
            ProjectStage.IN_PROGRESS, null, ONGOING, null, null, false, 0))
            .isInstanceOf(NullPointerException.class);
    }

    private static Project project(ProjectStage stage, DateRange period) {
        return new Project(null, "Titre", "titre", "Résumé", "# Titre",
            stage, ProjectVisibility.DRAFT, period, null, null, false, 0);
    }
}
