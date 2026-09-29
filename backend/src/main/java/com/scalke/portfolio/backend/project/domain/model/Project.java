package com.scalke.portfolio.backend.project.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Projet du portfolio (racine d'agrégat).
 * <p>
 * Invariants :
 * <ul>
 *   <li>la période ne se termine jamais avant de commencer ({@link DateRange}, invariant 17) ;</li>
 *   <li>{@code stage} est cohérent avec la période : {@code IN_PROGRESS} si et seulement si
 *       {@code period.isOngoing()} (invariant 21, D-T). Doublé par {@code project_stage_matches_dates_check} ;</li>
 *   <li>une technologie apparaît au plus une fois (invariant 23). Doublé par {@code project_technology_pk} ;</li>
 *   <li>une capture apparaît au plus une fois (invariant 27). Doublé par {@code project_screenshot_pk}.</li>
 * </ul>
 * Couverture ({@code coverMediaId}, facultative) et captures sont des références vers le catalogue
 * {@code media}, par identifiant (ADR 0002, D-BV) ; PostgreSQL interdit de supprimer un média ainsi référencé
 * (invariant 13). Les captures sont toujours rangées dans leur ordre d'affichage.
 * Les technologies sont des références vers un autre agrégat, toujours rangées dans l'ordre du
 * vocabulaire ({@link Technology#DISPLAY_ORDER}, D-Z). Le slug est un {@link Slug} (format vérifié par le
 * domaine, D-BB), unique par PostgreSQL (D-X). Il ne change plus dès que le projet a été publié
 * ({@code everPublished}, D11, D-CX, {@link #edit}) ; un projet {@code PUBLISHED} l'a toujours été (doublé par
 * {@code project_ever_published_check}).
 * <p>
 * Bornes (D-CX) : titre non vide de 160 caractères au plus, description courte non vide de 500, description de
 * {@value #DESCRIPTION_MAX_LENGTH} (doublée par {@code V023}), adresses de 2 048.
 */
public record Project(
    Long id,
    String title,
    Slug slug,
    String shortDescription,
    String descriptionMarkdown,
    ProjectStage stage,
    ProjectVisibility visibility,
    DateRange period,
    String repositoryUrl,
    String demoUrl,
    boolean featured,
    int displayOrder,
    List<Technology> technologies,
    Long coverMediaId,
    List<ProjectScreenshot> screenshots,
    boolean everPublished
) {

    public static final int TITLE_MAX_LENGTH = 160;
    public static final int SHORT_DESCRIPTION_MAX_LENGTH = 500;
    public static final int DESCRIPTION_MAX_LENGTH = 100_000;
    public static final int URL_MAX_LENGTH = 2048;

    public Project {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(shortDescription, "shortDescription");
        Objects.requireNonNull(descriptionMarkdown, "descriptionMarkdown");
        Objects.requireNonNull(slug, "slug");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(visibility, "visibility");
        Objects.requireNonNull(period, "period");
        if (title.isBlank() || title.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("title must be 1 to 160 characters");
        }
        if (shortDescription.isBlank() || shortDescription.length() > SHORT_DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("short description must be 1 to 500 characters");
        }
        if (descriptionMarkdown.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("description must be at most " + DESCRIPTION_MAX_LENGTH + " characters");
        }
        if (tooLong(repositoryUrl) || tooLong(demoUrl)) {
            throw new IllegalArgumentException("an address must be at most " + URL_MAX_LENGTH + " characters");
        }
        everPublished = everPublished || visibility == ProjectVisibility.PUBLISHED;
        if ((stage == ProjectStage.IN_PROGRESS) != period.isOngoing()) {
            throw new IllegalArgumentException(
                "stage " + stage + " contradicts the period " + period + ": IN_PROGRESS requires no end date");
        }
        technologies = Objects.requireNonNull(technologies, "technologies").stream()
            .sorted(Technology.DISPLAY_ORDER)
            .toList();
        Set<Slug> slugs = new HashSet<>();
        for (Technology technology : technologies) {
            if (!slugs.add(technology.slug())) {
                throw new IllegalArgumentException("technology " + technology.slug() + " appears twice");
            }
        }
        screenshots = Objects.requireNonNull(screenshots, "screenshots").stream()
            .sorted(ProjectScreenshot.DISPLAY_ORDER)
            .toList();
        Set<Long> media = new HashSet<>();
        for (ProjectScreenshot screenshot : screenshots) {
            if (!media.add(screenshot.mediaId())) {
                throw new IllegalArgumentException("screenshot " + screenshot.mediaId() + " appears twice");
            }
        }
    }

    /**
     * Nouveau projet (D-CX), dans la visibilité saisie : publié d'emblée, il est aussitôt marqué comme publié.
     */
    public static Project newProject(Slug slug, ProjectContent content) {
        return from(null, slug, content, false);
    }

    /**
     * Remplace la saisie de l'administrateur (D-CX), visibilité comprise. Un slug différent est refusé si le projet a
     * déjà été publié, avec {@link ErrorCode#SLUG_LOCKED} (409, D11) ; la publication de ce changement même compte
     * ensuite.
     */
    public Project edit(Slug newSlug, ProjectContent content) {
        Objects.requireNonNull(newSlug, "newSlug");
        if (everPublished && !newSlug.equals(slug)) {
            throw new BusinessRuleViolationException(ErrorCode.SLUG_LOCKED,
                "Le slug d'un projet déjà publié ne peut plus changer.");
        }
        return from(id, newSlug, content, everPublished);
    }

    private static Project from(Long id, Slug slug, ProjectContent content, boolean everPublished) {
        Objects.requireNonNull(content, "content");
        return new Project(id, content.title(), slug, content.shortDescription(), content.descriptionMarkdown(),
            content.stage(), content.visibility(), content.period(), content.repositoryUrl(), content.demoUrl(),
            content.featured(), content.displayOrder(), content.technologies(), content.coverMediaId(),
            content.screenshots(), everPublished);
    }

    private static boolean tooLong(String url) {
        return url != null && url.length() > URL_MAX_LENGTH;
    }
}
