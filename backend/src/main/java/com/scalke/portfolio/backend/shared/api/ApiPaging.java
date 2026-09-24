package com.scalke.portfolio.backend.shared.api;

/**
 * Tailles de page de l'API (D16, {@code docs/05-conventions-api.md} §13).
 * <p>
 * {@code PUBLIC_PAGE_SIZE} et {@code ADMIN_PAGE_SIZE} se déclarent par contrôleur avec
 * {@code @PageableDefault}. {@code MAX_PAGE_SIZE} est appliqué à la résolution HTTP par
 * {@code spring.data.web.pageable.max-page-size} ({@code application.yaml}) ; les deux valeurs
 * doivent rester égales, ce que vérifie {@code PublicProjectControllerTest}.
 */
public class ApiPaging {

    public static final int PUBLIC_PAGE_SIZE = 10;
    public static final int ADMIN_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private ApiPaging() {
    }
}
