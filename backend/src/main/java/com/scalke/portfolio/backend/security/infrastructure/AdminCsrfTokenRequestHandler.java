package com.scalke.portfolio.backend.security.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * CSRF d'une application monopage (D-CP), sur le modèle du gestionnaire {@code spa()} de Spring Security, à une
 * différence près : le cookie {@code XSRF-TOKEN} n'est déposé que sur les réponses de {@code /admin/**}.
 * L'application d'administration le reçoit dès sa première requête (avant la connexion) ; les réponses publiques
 * restent sans cookie (cache partageable, aucun état pour un visiteur).
 * <p>
 * Jeton lu dans l'en-tête {@code X-XSRF-TOKEN} tel quel (valeur du cookie, envoyée par Angular), ou dans un paramètre
 * sous sa forme masquée (protection BREACH des formulaires rendus par le serveur).
 */
final class AdminCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private static final RequestMatcher ADMIN = PathPatternRequestMatcher.withDefaults().matcher("/admin/**");

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        xor.handle(request, response, csrfToken);
        if (ADMIN.matches(request)) {
            // Charger le jeton différé l'écrit dans son cookie.
            csrfToken.get();
        }
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String header = request.getHeader(csrfToken.getHeaderName());
        return (StringUtils.hasText(header) ? plain : xor).resolveCsrfTokenValue(request, csrfToken);
    }
}
