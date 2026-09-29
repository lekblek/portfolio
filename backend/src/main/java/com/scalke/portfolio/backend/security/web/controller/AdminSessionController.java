package com.scalke.portfolio.backend.security.web.controller;

import com.scalke.portfolio.backend.security.application.usecase.AuthenticateAdminUseCase;
import com.scalke.portfolio.backend.security.application.usecase.AuthenticatedAdmin;
import com.scalke.portfolio.backend.security.application.usecase.GetAdminAccountUseCase;
import com.scalke.portfolio.backend.security.web.dto.AdminSessionResponse;
import com.scalke.portfolio.backend.security.web.dto.OpenAdminSessionRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session de l'administrateur (D-CO) : {@code POST} l'ouvre (seule route d'administration ouverte sans
 * authentification), {@code GET} la décrit ; la fermeture ({@code DELETE}) est traitée par le filtre de
 * déconnexion de Spring Security ({@code SecurityConfiguration}).
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/session")
public class AdminSessionController {

    private final AuthenticateAdminUseCase authenticateAdminUseCase;
    private final GetAdminAccountUseCase getAdminAccountUseCase;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
        SecurityContextHolder.getContextHolderStrategy();

    /**
     * Connexion : identifiant de session et jeton CSRF renouvelés (fixation de session), puis authentification
     * enregistrée dans la session. Tentatives limitées par adresse du client (D-CQ) ; derrière le mandataire inverse
     * de production, l'adresse réelle viendra des en-têtes de transfert (étape 52).
     */
    @PostMapping
    AdminSessionResponse open(@Valid @RequestBody OpenAdminSessionRequest body, HttpServletRequest request,
                              HttpServletResponse response) {
        AuthenticatedAdmin admin = authenticateAdminUseCase.execute(body.login(), body.password(), request.getRemoteAddr());
        sessionAuthenticationStrategy.onAuthentication(admin.authentication(), request, response);
        // Seule la session porte l'authentification : la requête en cours n'en a plus besoin.
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(admin.authentication());
        securityContextRepository.saveContext(context, request, response);
        return AdminSessionResponse.from(admin.account());
    }

    @GetMapping
    AdminSessionResponse current() {
        return AdminSessionResponse.from(getAdminAccountUseCase.execute());
    }
}
