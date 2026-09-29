package com.scalke.portfolio.backend.security.web.controller;

import com.scalke.portfolio.backend.security.application.usecase.AdminCredentials;
import com.scalke.portfolio.backend.security.application.usecase.InitializeAdminAccountUseCase;
import com.scalke.portfolio.backend.security.domain.model.LoginAttempts;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static com.scalke.portfolio.backend.testsupport.CsrfTestSupport.xsrf;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Session de l'administrateur de bout en bout (D-CO, D-CP) : connexion, refus uniforme, CSRF d'une application
 * monopage, fixation de session, déconnexion.
 */
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class AdminSessionIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "correct-horse-battery-staple";

    /**
     * Adresse de MockMvc par défaut.
     */
    private static final String CLIENT = "127.0.0.1";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    InitializeAdminAccountUseCase initializeAdminAccountUseCase;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    EntityManager entityManager;

    @Autowired
    LoginAttempts loginAttempts;

    /**
     * Le compteur de tentatives est partagé par tout le contexte : chaque test part d'une adresse sans échec.
     */
    @BeforeEach
    void givenTheAdministratorAccount() {
        initializeAdminAccountUseCase.execute(new AdminCredentials("admin", PASSWORD));
        loginAttempts.reset(CLIENT);
    }

    /**
     * D-CQ : au cinquième échec, la source attend 15 minutes, même avec le bon mot de passe, sans qu'il soit vérifié.
     */
    @Test
    void makes_a_source_wait_after_five_failures() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(login("admin", "mauvais-mot-de-passe").with(xsrf()))
                .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(login("admin", PASSWORD).with(xsrf()))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "900"))
            .andExpect(jsonPath("$.code").value("TOO_MANY_LOGIN_ATTEMPTS"));
        mockMvc.perform(login("admin", PASSWORD).with(xsrf()).with(request -> {
                request.setRemoteAddr("192.0.2.10");
                return request;
            }))
            .andExpect(status().isOk());
    }

    @Test
    void a_success_forgets_the_previous_failures() throws Exception {
        for (int attempt = 0; attempt < 4; attempt++) {
            mockMvc.perform(login("admin", "mauvais-mot-de-passe").with(xsrf()));
        }
        mockMvc.perform(login("admin", PASSWORD).with(xsrf())).andExpect(status().isOk());

        mockMvc.perform(login("admin", "mauvais-mot-de-passe").with(xsrf())).andExpect(status().isUnauthorized());
        mockMvc.perform(login("admin", PASSWORD).with(xsrf())).andExpect(status().isOk());
    }

    @Test
    void opens_a_session_and_records_the_login_date() throws Exception {
        MvcResult login = mockMvc.perform(login("admin", PASSWORD).with(xsrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("admin"))
            .andExpect(jsonPath("$.lastLoginAt").value("2026-06-15T10:00:00Z"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andReturn();

        mockMvc.perform(get("/api/admin/session").contextPath("/api").session(session(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("admin"));
        entityManager.flush();
        assertThat(jdbcClient.sql("SELECT last_login_at FROM admin_account").query(OffsetDateTime.class).single()
            .toInstant()).isEqualTo(NOW);
    }

    /**
     * Rien n'est révélé : identifiant inconnu et mauvais mot de passe donnent la même réponse, sans session.
     */
    @Test
    void refuses_a_wrong_password_and_an_unknown_login_the_same_way() throws Exception {
        for (MockHttpServletRequestBuilder attempt : new MockHttpServletRequestBuilder[]{
            login("admin", "mauvais-mot-de-passe"), login("inconnu", PASSWORD)}) {
            MvcResult result = mockMvc.perform(attempt.with(xsrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("Identifiant ou mot de passe incorrect."))
                .andReturn();
            assertThat(result.getRequest().getSession(false)).isNull();
        }
        entityManager.flush();
        assertThat(jdbcClient.sql("SELECT last_login_at IS NULL FROM admin_account").query(Boolean.class).single())
            .isTrue();
    }

    /**
     * bcrypt ne compare que les 72 premiers octets : sans garde, le bon mot de passe de 72 octets suivi de n'importe
     * quoi ouvrirait la session. Un mot de passe plus long est donc toujours faux (D-CO).
     */
    @Test
    void refuses_a_correct_password_followed_by_anything() throws Exception {
        String longest = "a".repeat(AdminCredentials.PASSWORD_MAX_BYTES);
        initializeAdminAccountUseCase.execute(new AdminCredentials("admin", longest));

        mockMvc.perform(login("admin", longest + "b").with(xsrf()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mockMvc.perform(login("admin", longest).with(xsrf()))
            .andExpect(status().isOk());
    }

    /**
     * Messages en français même pour un navigateur anglais (D-DJ).
     */
    @Test
    void requires_both_fields() throws Exception {
        mockMvc.perform(post("/api/admin/session").contextPath("/api").with(xsrf()).header("Accept-Language", "en")
                .contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"admin\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.detail").value("Certaines valeurs de la requête sont invalides."))
            .andExpect(jsonPath("$.errors[0].field").value("password"))
            .andExpect(jsonPath("$.errors[0].message").value("ne doit pas être vide"));
    }

    /**
     * Parcours d'Angular : une première requête d'administration (refusée) dépose le cookie {@code XSRF-TOKEN} ; la
     * connexion le renvoie dans l'en-tête {@code X-XSRF-TOKEN} et reçoit un nouveau jeton.
     */
    @Test
    void follows_the_csrf_flow_of_a_single_page_application() throws Exception {
        Cookie token = mockMvc.perform(get("/api/admin/session").contextPath("/api"))
            .andExpect(status().isUnauthorized())
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(login("admin", PASSWORD))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        Cookie renewed = mockMvc.perform(login("admin", PASSWORD).cookie(token).header("X-XSRF-TOKEN", token.getValue()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(renewed).isNotNull();
        assertThat(renewed.getValue()).isNotEqualTo(token.getValue());
    }

    @Test
    void puts_no_csrf_cookie_on_public_responses() throws Exception {
        mockMvc.perform(get("/api/public/projects").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(cookie().doesNotExist("XSRF-TOKEN"));
    }

    /**
     * Fixation de session : une session ouverte avant la connexion change d'identifiant.
     */
    @Test
    void changes_the_session_identifier_at_login() throws Exception {
        MockHttpSession before = new MockHttpSession();
        String identifierBefore = before.getId();

        MvcResult login = mockMvc.perform(login("admin", PASSWORD).with(xsrf()).session(before))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(login.getRequest().getSession(false).getId()).isNotEqualTo(identifierBefore);
    }

    @Test
    void closes_the_session() throws Exception {
        MockHttpSession session = session(mockMvc.perform(login("admin", PASSWORD).with(xsrf())).andReturn());

        mockMvc.perform(delete("/api/admin/session").contextPath("/api").session(session).with(xsrf()))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/session").contextPath("/api").session(session))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    /**
     * D-DF : chaque événement de la session laisse une ligne avec sa source, sans l'identifiant ni le mot de passe
     * saisis.
     */
    @Test
    void logs_the_security_events_of_the_session(CapturedOutput output) throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(login("admin-ou-mot-de-passe", "mauvais-mot-de-passe").with(xsrf()));
        }
        mockMvc.perform(login("admin", PASSWORD).with(xsrf())).andExpect(status().isTooManyRequests());
        loginAttempts.reset(CLIENT);
        MockHttpSession session = session(mockMvc.perform(login("admin", PASSWORD).with(xsrf())).andReturn());
        mockMvc.perform(delete("/api/admin/session").contextPath("/api").session(session).with(xsrf()))
            .andExpect(status().isNoContent());

        assertThat(output.getOut().lines().filter(line -> line.contains("Connexion refusée depuis 127.0.0.1")))
            .hasSize(5);
        assertThat(output)
            .contains("Connexion bloquée depuis 127.0.0.1 : trop d'échecs, nouvel essai dans 900 s")
            .contains("Connexion de l'administrateur admin depuis 127.0.0.1")
            .contains("Déconnexion de l'administrateur admin")
            .doesNotContain("admin-ou-mot-de-passe")
            .doesNotContain("mauvais-mot-de-passe")
            .doesNotContain(PASSWORD);
    }

    private static MockHttpServletRequestBuilder login(String login, String password) {
        return post("/api/admin/session").contextPath("/api")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"%s\",\"password\":\"%s\"}".formatted(login, password));
    }

    private static MockHttpSession session(MvcResult result) {
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
