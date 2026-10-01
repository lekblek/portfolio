package com.scalke.portfolio.backend.security.infrastructure;

import jakarta.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Séparation des routes ({@code 05} §3, D-CK) et session de l'administrateur (D-CO, D-CP). Les chemins sont relatifs
 * au contexte {@code /api}.
 * <ul>
 *   <li>{@code /public/**}, {@code /actuator/health} : ouverts ;</li>
 *   <li>documentation OpenAPI : ouverte, mais servie en profil {@code dev} seulement (KI-19) ;</li>
 *   <li>{@code POST /admin/session} (connexion) : ouvert ; {@code DELETE /admin/session} : déconnexion ;</li>
 *   <li>le reste de {@code /admin/**} : session authentifiée obligatoire ;</li>
 *   <li>toute autre route : refusée.</li>
 * </ul>
 * Refus rendus par {@code GlobalExceptionHandler} ({@code ProblemDetail} et {@code code}, D-CL). Aucune requête
 * refusée n'est mémorisée en session : un visiteur anonyme ne reçoit jamais de session. CSRF pour une application
 * monopage (D-CP) : jeton dans le cookie {@code XSRF-TOKEN}, renvoyé dans l'en-tête {@code X-XSRF-TOKEN}. En-têtes et
 * attributs des cookies : D-CR. Seul {@code POST /public/contact-messages}, anonyme, se passe du jeton CSRF
 * (D-EJ).
 */
@Configuration
@Slf4j
public class SecurityConfiguration {

    private static final String SESSION = "/admin/session";
    private static final String CONTACT_MESSAGES = "/public/contact-messages";

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CookieCsrfTokenRepository csrfTokenRepository,
        SecurityContextRepository securityContextRepository,
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
        return http
            .authorizeHttpRequests(requests -> requests
                // Rendu d'une erreur par le conteneur : la décision a déjà été prise pour la requête d'origine.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/public/**", "/actuator/health").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, SESSION).permitAll()
                .requestMatchers("/admin/**").authenticated()
                .anyRequest().denyAll())
            .securityContext(context -> context.securityContextRepository(securityContextRepository))
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new AdminCsrfTokenRequestHandler())
                // Envoi anonyme du formulaire de contact (D-EJ) : aucune session ni aucun droit à détourner, et un
                // visiteur n'a pas de jeton ; le piège à robots et la limitation de débit protègent la route.
                .ignoringRequestMatchers(
                    PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, CONTACT_MESSAGES)))
            .logout(logout -> logout
                .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.DELETE, SESSION))
                // Journal des événements de sécurité (D-DF) ; sans session, la déconnexion ne ferme rien.
                .addLogoutHandler((request, response, authentication) -> {
                    if (authentication != null) {
                        log.info("Déconnexion de l'administrateur {}", authentication.getName());
                    }
                })
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                .deleteCookies("JSESSIONID"))
            // En plus des en-têtes par défaut (nosniff, X-Frame-Options DENY, Cache-Control no-store, HSTS en HTTPS) :
            // aucune réponse ne s'affiche dans un cadre, aucune adresse n'est transmise en référent (D-CR).
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'none'"))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    exceptionResolver.resolveException(request, response, null, exception))
                .accessDeniedHandler((request, response, exception) ->
                    exceptionResolver.resolveException(request, response, null, exception)))
            .build();
    }

    /**
     * Jeton CSRF en cookie lisible par l'application Angular (D-CP), avec les mêmes attributs que le cookie de session
     * ({@code server.servlet.session.cookie.*}, D-CR) : {@code Secure} et {@code SameSite=Strict}.
     */
    @Bean
    CookieCsrfTokenRepository csrfTokenRepository(
        @Value("${server.servlet.session.cookie.secure}") boolean secure,
        @Value("${server.servlet.session.cookie.same-site}") String sameSite) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.secure(secure).sameSite(sameSite));
        return repository;
    }

    /**
     * L'authentification est conservée dans la session HTTP : même dépôt pour l'écrire à la connexion et la relire.
     */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Requête rejetée par le pare-feu HTTP de Spring Security avant tout filtre (séparateur encodé {@code %2F},
     * {@code ..}, caractère interdit) : 400 {@code MALFORMED_REQUEST} de même forme que les autres erreurs (D-CL),
     * au lieu d'une 400 sans corps.
     */
    @Bean
    RequestRejectedHandler requestRejectedHandler(
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        return (request, response, exception) -> exceptionResolver.resolveException(request, response, null, exception);
    }
}
