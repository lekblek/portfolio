package com.scalke.portfolio.backend.security.infrastructure;

import com.scalke.portfolio.backend.security.application.usecase.AdminCredentials;
import com.scalke.portfolio.backend.security.application.usecase.InitializeAdminAccountUseCase;
import com.scalke.portfolio.backend.security.domain.model.LoginAttempts;
import com.scalke.portfolio.backend.testsupport.ContainersConfiguration;
import com.scalke.portfolio.backend.testsupport.FixedClockConfiguration;
import com.scalke.portfolio.backend.testsupport.MailTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce que seul un vrai serveur montre (D-CR) : attributs des cookies posés par Tomcat, traitement d'un séparateur
 * encodé par Tomcat (KI-33). MockMvc ne passe ni par Tomcat ni par sa configuration de session. Contexte distinct de
 * {@code AbstractIntegrationTest} (serveur sur un port aléatoire).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({ContainersConfiguration.class, FixedClockConfiguration.class, MailTestConfiguration.class})
class HttpServerSecurityIT {

    private static final String PASSWORD = "correct-horse-battery-staple";

    @LocalServerPort
    int port;

    @Autowired
    InitializeAdminAccountUseCase initializeAdminAccountUseCase;

    @Autowired
    LoginAttempts loginAttempts;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void givenTheAdministratorAccount() {
        initializeAdminAccountUseCase.execute(new AdminCredentials("admin", PASSWORD));
        loginAttempts.reset("127.0.0.1");
    }

    /**
     * KI-33 : un {@code %2F} atteint le pare-feu de Spring Security, qui le rejette avec une erreur codée, au lieu de la
     * page HTML de Tomcat.
     */
    @Test
    void rejects_an_encoded_separator_with_a_coded_error() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/public/media/..%2Fapplication.yaml")));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
            assertThat(type).startsWith("application/problem+json"));
        assertThat(response.body()).contains("\"code\":\"MALFORMED_REQUEST\"");
    }

    /**
     * Les deux cookies de l'administration : {@code Secure} et {@code SameSite=Strict} ; la session n'est jamais lisible
     * par JavaScript, le jeton CSRF doit l'être (Angular le recopie dans l'en-tête).
     */
    @Test
    void sets_secure_same_site_cookies_for_the_administration() throws Exception {
        HttpResponse<String> first = send(HttpRequest.newBuilder(uri("/api/admin/session")));
        String csrfCookie = cookie(first, "XSRF-TOKEN").orElseThrow();
        assertThat(csrfCookie).contains("Secure").containsIgnoringCase("SameSite=Strict").doesNotContain("HttpOnly");

        String token = csrfCookie.substring("XSRF-TOKEN=".length(), csrfCookie.indexOf(';'));
        HttpResponse<String> login = send(HttpRequest.newBuilder(uri("/api/admin/session"))
            .header("Content-Type", "application/json")
            .header("Cookie", "XSRF-TOKEN=" + token)
            .header("X-XSRF-TOKEN", token)
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"login\":\"admin\",\"password\":\"" + PASSWORD + "\"}")));

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(cookie(login, "JSESSIONID")).hasValueSatisfying(session -> assertThat(session)
            .contains("HttpOnly").contains("Secure").containsIgnoringCase("SameSite=Strict"));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static Optional<String> cookie(HttpResponse<?> response, String name) {
        List<String> cookies = response.headers().allValues("Set-Cookie");
        return cookies.stream().filter(value -> value.startsWith(name + "=") && !value.startsWith(name + "=;"))
            .findFirst();
    }
}
