package com.scalke.portfolio.backend.testsupport;

import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;

/**
 * Jeton CSRF des tests, envoyé comme le fait Angular (D-CP) : même valeur dans le cookie {@code XSRF-TOKEN} et dans
 * l'en-tête {@code X-XSRF-TOKEN}, vérifiée par le vrai dépôt de l'application.
 * <p>
 * À utiliser à la place de {@code SecurityMockMvcRequestPostProcessors.csrf()} : ce dernier remplace durablement le
 * dépôt CSRF du filtre, partagé par tous les tests du contexte, par un dépôt en session ; les tests suivants
 * verraient alors des sessions et plus de cookie.
 */
public final class CsrfTestSupport {

    private static final String TOKEN = "jeton-csrf-de-test";

    private CsrfTestSupport() {
    }

    public static RequestPostProcessor xsrf() {
        return request -> {
            Cookie[] existing = request.getCookies();
            Cookie[] cookies = existing == null ? new Cookie[1] : Arrays.copyOf(existing, existing.length + 1);
            cookies[cookies.length - 1] = new Cookie("XSRF-TOKEN", TOKEN);
            request.setCookies(cookies);
            request.addHeader("X-XSRF-TOKEN", TOKEN);
            return request;
        };
    }
}
