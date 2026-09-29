package com.scalke.portfolio.backend.project.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectTest {

    private static final DateRange ONGOING = DateRange.ongoingSince(LocalDate.of(2024, 1, 1));
    private static final DateRange ENDED = DateRange.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

    private static final Technology JAVA = new Technology(1L, "Java", Slug.of("java"), 0);
    private static final Technology ANGULAR = new Technology(2L, "Angular", Slug.of("angular"), 1);
    private static final Technology DOCKER = new Technology(3L, "Docker", Slug.of("docker"), 1);

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
        assertThatThrownBy(() -> new Project(null, "Titre", Slug.of("titre"), "Résumé", "# Titre",
            ProjectStage.IN_PROGRESS, null, ONGOING, null, null, false, 0, List.of(), null, List.of(), false))
            .isInstanceOf(NullPointerException.class);
    }

    /**
     * D-Z : l'ordre des technologies est celui du vocabulaire, quel que soit l'ordre fourni.
     */
    @Test
    void keeps_technologies_in_vocabulary_display_order() {
        Project project = project(List.of(DOCKER, JAVA, ANGULAR));

        assertThat(project.technologies()).containsExactly(JAVA, ANGULAR, DOCKER);
    }

    @Test
    void rejects_the_same_technology_twice() {
        assertThatThrownBy(() -> project(List.of(JAVA, ANGULAR, JAVA)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("java");
    }

    @Test
    void keeps_screenshots_in_display_order() {
        Project project = withScreenshots(
            new ProjectScreenshot(30L, null, 2), new ProjectScreenshot(20L, "Accueil", 0), new ProjectScreenshot(10L, null, 2));

        assertThat(project.screenshots()).extracting(ProjectScreenshot::mediaId).containsExactly(20L, 10L, 30L);
    }

    /**
     * Invariant 27.
     */
    @Test
    void rejects_the_same_screenshot_twice() {
        assertThatThrownBy(() -> withScreenshots(new ProjectScreenshot(10L, null, 0), new ProjectScreenshot(10L, null, 1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("screenshot 10");
    }

    @Test
    void validates_each_screenshot() {
        assertThatThrownBy(() -> new ProjectScreenshot(10L, null, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectScreenshot(10L, "a".repeat(301), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectScreenshot(null, null, 0)).isInstanceOf(NullPointerException.class);
    }

    /**
     * D-CX : un projet publié l'a toujours été ; la mémoire survit au retour en brouillon.
     */
    @Test
    void a_published_project_remembers_its_publication() {
        assertThat(Project.newProject(Slug.of("titre"), content(ProjectVisibility.PUBLISHED)).everPublished()).isTrue();

        Project draft = Project.newProject(Slug.of("titre"), content(ProjectVisibility.DRAFT));
        assertThat(draft.everPublished()).isFalse();
        Project published = draft.edit(Slug.of("titre"), content(ProjectVisibility.PUBLISHED));
        assertThat(published.edit(Slug.of("titre"), content(ProjectVisibility.DRAFT)).everPublished()).isTrue();
    }

    /**
     * D11 : le slug ne change plus après la publication ; avant, il peut changer, même dans la modification qui publie.
     */
    @Test
    void locks_the_slug_once_published() {
        Project draft = Project.newProject(Slug.of("titre"), content(ProjectVisibility.DRAFT));

        Project published = draft.edit(Slug.of("nouveau"), content(ProjectVisibility.PUBLISHED));
        assertThat(published.slug()).isEqualTo(Slug.of("nouveau"));
        assertThat(published.edit(Slug.of("nouveau"), content(ProjectVisibility.ARCHIVED)).visibility())
            .isEqualTo(ProjectVisibility.ARCHIVED);
        assertThatThrownBy(() -> published.edit(Slug.of("autre"), content(ProjectVisibility.ARCHIVED)))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.SLUG_LOCKED));
    }

    @Test
    void bounds_the_text_fields() {
        assertThatThrownBy(() -> withText(" ", "Résumé", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withText("t".repeat(161), "Résumé", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withText("Titre", " ", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withText("Titre", "r".repeat(501), "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withText("Titre", "Résumé", "d".repeat(100_001)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(withText("t".repeat(160), "r".repeat(500), "d".repeat(100_000))).isNotNull();
    }

    private static ProjectContent content(ProjectVisibility visibility) {
        return new ProjectContent("Titre", "Résumé", "# Titre", ProjectStage.IN_PROGRESS, visibility, ONGOING, null,
            null, false, 0, List.of(JAVA), null, List.of());
    }

    private static Project withText(String title, String shortDescription, String description) {
        return new Project(null, title, Slug.of("titre"), shortDescription, description, ProjectStage.IN_PROGRESS,
            ProjectVisibility.DRAFT, ONGOING, null, null, false, 0, List.of(), null, List.of(), false);
    }

    private static Project withScreenshots(ProjectScreenshot... screenshots) {
        return new Project(null, "Titre", Slug.of("titre"), "Résumé", "# Titre",
            ProjectStage.IN_PROGRESS, ProjectVisibility.DRAFT, ONGOING, null, null, false, 0, List.of(),
            7L, List.of(screenshots), false);
    }

    private static Project project(ProjectStage stage, DateRange period) {
        return new Project(null, "Titre", Slug.of("titre"), "Résumé", "# Titre",
            stage, ProjectVisibility.DRAFT, period, null, null, false, 0, List.of(), null, List.of(), false);
    }

    private static Project project(List<Technology> technologies) {
        return new Project(null, "Titre", Slug.of("titre"), "Résumé", "# Titre",
            ProjectStage.IN_PROGRESS, ProjectVisibility.DRAFT, ONGOING, null, null, false, 0, technologies, null, List.of(), false);
    }
}
