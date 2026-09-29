package com.scalke.portfolio.backend.security.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Trace des écritures d'administration (D-DF) : méthode, chemin, statut et administrateur, sans le corps. Enregistré
 * après la chaîne de Spring Security (ordre par défaut), il ne voit que les requêtes admises et lit l'authentification
 * de la session. La session elle-même est journalisée par la connexion et la déconnexion.
 */
@Component
@Slf4j
class AdminWriteLogFilter extends OncePerRequestFilter {

    private static final Set<String> READS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        chain.doFilter(request, response);
        if (isAdministrationWrite(request)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.info("Administration : {} {} → {} ({})", request.getMethod(), request.getRequestURI(),
                response.getStatus(), authentication == null ? "anonyme" : authentication.getName());
        }
    }

    private static boolean isAdministrationWrite(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !READS.contains(request.getMethod()) && path.startsWith("/admin/") && !path.equals("/admin/session");
    }
}
