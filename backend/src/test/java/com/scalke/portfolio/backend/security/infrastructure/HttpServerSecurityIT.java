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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce que seul un vrai serveur montre (D-CR, D-CT, D-DC) : attributs des cookies posés par Tomcat, traitement d'un
 * séparateur encodé par Tomcat (KI-33), limite d'une requête multipart, en-têtes du mandataire inverse. MockMvc ne passe
 * ni par Tomcat ni par sa configuration. Contexte distinct de {@code AbstractIntegrationTest} (serveur sur un port
 * aléatoire). Le client de test est en 127.0.0.1, réseau de confiance comme le mandataire en production.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({ContainersConfiguration.class, FixedClockConfiguration.class, MailTestConfiguration.class})
class HttpServerSecurityIT {

    private static final String PASSWORD = "correct-horse-battery-staple";
    // Adresses de documentation (RFC 5737), jamais routées.
    private static final String FIRST_CLIENT = "203.0.113.7";
    private static final String SECOND_CLIENT = "203.0.113.8";

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

        HttpResponse<String> login = login(value(csrfCookie));

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(cookie(login, "JSESSIONID")).hasValueSatisfying(session -> assertThat(session)
            .contains("HttpOnly").contains("Secure").containsIgnoringCase("SameSite=Strict"));
    }

    /**
     * D-CT : un fichier au-delà de 10 Mio est refusé par un 413 codé, et non par une connexion coupée : Tomcat lit
     * la fin de la requête refusée ({@code server.tomcat.max-swallow-size}) avant de répondre.
     */
    @Test
    void refuses_an_oversized_upload_with_a_coded_error() throws Exception {
        String token = value(cookie(send(HttpRequest.newBuilder(uri("/api/admin/session"))), "XSRF-TOKEN")
            .orElseThrow());
        String session = value(cookie(login(token), "JSESSIONID").orElseThrow());
        String boundary = "limite-de-la-requete";
        byte[] head = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"cv.pdf\"\r\n"
            + "Content-Type: application/pdf\r\n\r\n%PDF-1.7").getBytes(StandardCharsets.US_ASCII);
        byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes(head);
        body.writeBytes(new byte[11 * 1024 * 1024]);
        body.writeBytes(tail);

        // Longueur annoncée, comme un navigateur : Tomcat refuse la requête avant d'en lire le corps.
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/admin/media"))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .header("Cookie", "XSRF-TOKEN=" + token + "; JSESSIONID=" + session)
            .header("X-XSRF-TOKEN", token)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())));

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("\"code\":\"MEDIA_TOO_LARGE\"");
    }

    /**
     * D-DC : derrière le mandataire inverse, la limite des essais porte sur l'adresse du client transmise par
     * {@code X-Forwarded-For}, et non sur celle du mandataire. Sinon cinq échecs d'un inconnu bloqueraient tout le monde.
     */
    @Test
    void limits_login_attempts_per_client_behind_the_reverse_proxy() throws Exception {
        String token = value(cookie(send(HttpRequest.newBuilder(uri("/api/admin/session"))), "XSRF-TOKEN")
            .orElseThrow());
        try {
            for (int attempt = 0; attempt < 5; attempt++) {
                assertThat(wrongLoginFrom(FIRST_CLIENT, token).statusCode()).isEqualTo(401);
            }
            assertThat(wrongLoginFrom(FIRST_CLIENT, token).statusCode()).isEqualTo(429);
            assertThat(wrongLoginFrom(SECOND_CLIENT, token).statusCode()).isEqualTo(401);
        } finally {
            loginAttempts.reset(FIRST_CLIENT);
            loginAttempts.reset(SECOND_CLIENT);
        }
    }

    /**
     * D-DC : TLS est terminé par le mandataire ; {@code X-Forwarded-Proto: https} fait reconnaître la requête comme
     * sécurisée, d'où l'en-tête HSTS, jamais envoyé sur du HTTP.
     */
    @Test
    void recognizes_https_terminated_by_the_reverse_proxy() throws Exception {
        HttpResponse<String> plain = send(HttpRequest.newBuilder(uri("/api/public/projects")));
        HttpResponse<String> forwarded = send(HttpRequest.newBuilder(uri("/api/public/projects"))
            .header("X-Forwarded-Proto", "https"));

        assertThat(plain.headers().firstValue("Strict-Transport-Security")).isEmpty();
        assertThat(forwarded.headers().firstValue("Strict-Transport-Security")).isPresent();
    }

    private HttpResponse<String> wrongLoginFrom(String client, String csrfToken) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri("/api/admin/session"))
            .header("Content-Type", "application/json")
            .header("X-Forwarded-For", client)
            .header("Cookie", "XSRF-TOKEN=" + csrfToken)
            .header("X-XSRF-TOKEN", csrfToken)
            .POST(HttpRequest.BodyPublishers.ofString("{\"login\":\"admin\",\"password\":\"mauvais mot de passe\"}")));
    }

    private HttpResponse<String> login(String csrfToken) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri("/api/admin/session"))
            .header("Content-Type", "application/json")
            .header("Cookie", "XSRF-TOKEN=" + csrfToken)
            .header("X-XSRF-TOKEN", csrfToken)
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"login\":\"admin\",\"password\":\"" + PASSWORD + "\"}")));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    /**
     * Valeur d'un en-tête {@code Set-Cookie} : entre le nom et le premier attribut.
     */
    private static String value(String setCookie) {
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }

    private static Optional<String> cookie(HttpResponse<?> response, String name) {
        List<String> cookies = response.headers().allValues("Set-Cookie");
        return cookies.stream().filter(value -> value.startsWith(name + "=") && !value.startsWith(name + "=;"))
            .findFirst();
    }
}
