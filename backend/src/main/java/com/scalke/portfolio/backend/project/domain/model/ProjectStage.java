package com.scalke.portfolio.backend.project.domain.model;

/**
 * État métier d'un projet, indépendant de sa visibilité (D19). Cohérent avec la période du projet
 * (invariant 21, D-T) : {@code IN_PROGRESS} si et seulement si la période est en cours.
 */
public enum ProjectStage {
    IN_PROGRESS,
    COMPLETED
}
