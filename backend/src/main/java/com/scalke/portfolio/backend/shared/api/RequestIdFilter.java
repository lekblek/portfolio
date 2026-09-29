package com.scalke.portfolio.backend.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Identifiant de requête (D-DB) : chaque requête reçoit un identifiant, placé dans le contexte des journaux
 * ({@code requestId}) et renvoyé dans l'en-tête {@value #HEADER}. Il relie une réponse, en particulier une erreur 500,
 * aux lignes de journal qui l'expliquent.
 * <p>
 * Un identifiant reçu dans le même en-tête (posé par le mandataire inverse) est repris s'il est court et sans
 * caractère spécial ; sinon il est remplacé, pour qu'aucune valeur arbitraire n'entre dans les journaux. Premier
 * filtre de la chaîne : même une requête refusée par la sécurité porte son identifiant.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final Pattern ACCEPTED = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /**
     * Identifiant de la requête en cours, s'il y en a une ; sinon vide (traitement hors requête HTTP).
     */
    public static Optional<String> current() {
        return Optional.ofNullable(MDC.get(MDC_KEY));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String received = request.getHeader(HEADER);
        String requestId = received != null && ACCEPTED.matcher(received).matches()
            ? received
            : UUID.randomUUID().toString();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
