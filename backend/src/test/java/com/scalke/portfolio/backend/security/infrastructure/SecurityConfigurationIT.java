package com.scalke.portfolio.backend.security.infrastructure;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static com.scalke.portfolio.backend.testsupport.CsrfTestSupport.xsrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règles de {@link SecurityConfiguration} (D-CK, D-CL) sur l'application complète, filtres de sécurité compris.
 */
class SecurityConfigurationIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    /**
     * Un visiteur anonyme lit l'API publique sans recevoir de session.
     */
    @Test
    void opens_the_public_api_without_creating_a_session() throws Exception {
        mockMvc.perform(get("/api/public/projects").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(noSessionCreated());
    }

    /**
     * D-CR : en-têtes de sécurité d'une API (recommandations de l'OWASP), sur toute réponse.
     */
    @Test
    void sends_the_security_headers_of_an_api() throws Exception {
        mockMvc.perform(get("/api/public/projects").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Content-Security-Policy", "frame-ancestors 'none'"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    void opens_the_health_endpoint() throws Exception {
        mockMvc.perform(get("/api/actuator/health").contextPath("/api"))
            .andExpect(status().isOk());
    }

    /**
     * {@code 05} §9 : route d'administration sans authentification → 401 codée ; la requête refusée n'est pas
     * mémorisée en session.
     */
    @Test
    void requires_an_authentication_for_the_administration() throws Exception {
        mockMvc.perform(get("/api/admin/projects").contextPath("/api"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(noSessionCreated());
    }

    /**
     * Authentifié, l'administrateur passe la sécurité sur tout {@code /api/admin/**} : une route inexistante y donne la
     * 404 du routage, et non un refus de sécurité.
     */
    @Test
    void lets_an_authenticated_administrator_reach_the_administration() throws Exception {
        mockMvc.perform(get("/api/admin/does-not-exist").contextPath("/api").with(user("admin")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    /**
     * Toute route non déclarée est refusée : 401 sans authentification, 403 même authentifié.
     */
    @Test
    void denies_every_other_route() throws Exception {
        mockMvc.perform(get("/api/internal").contextPath("/api"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/api/internal").contextPath("/api").with(user("admin")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    /**
     * CSRF actif par défaut (réglage à l'étape 35) : sans jeton, une écriture est refusée avant d'atteindre le
     * contrôleur ; avec jeton, elle l'atteint (ici 405 : la recherche n'accepte que GET).
     */
    @Test
    void refuses_a_write_without_csrf_token() throws Exception {
        mockMvc.perform(post("/api/public/search").contextPath("/api"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(post("/api/public/search").contextPath("/api").with(xsrf()))
            .andExpect(status().isMethodNotAllowed());
    }

    /**
     * Le pare-feu HTTP de Spring Security rejette avant toute route les chemins ambigus (ici un point-virgule ; la
     * double barre oblique aussi, vérifiée sur le serveur réel car MockMvc la normalise), avec une erreur codée. Un
     * séparateur encodé ({@code %2F}) est rejeté plus tôt encore par Tomcat (KI-33).
     */
    @Test
    void rejects_an_ambiguous_path_before_any_route() throws Exception {
        mockMvc.perform(get("/api/public/media/x;jsessionid=1").contextPath("/api"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    /**
     * KI-19 : la documentation OpenAPI n'est servie qu'en profil {@code dev}.
     */
    @Test
    void serves_no_api_documentation_outside_development() throws Exception {
        mockMvc.perform(get("/api/v3/api-docs").contextPath("/api"))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/swagger-ui.html").contextPath("/api"))
            .andExpect(status().isNotFound());
    }

    private static ResultMatcher noSessionCreated() {
        return result -> assertThat(result.getRequest().getSession(false)).isNull();
    }
}
