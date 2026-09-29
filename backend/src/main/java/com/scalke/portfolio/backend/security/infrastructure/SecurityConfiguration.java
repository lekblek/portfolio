package com.scalke.portfolio.backend.security.infrastructure;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Séparation des routes ({@code 05} §3, D-CK). Les chemins sont relatifs au contexte {@code /api}.
 * <ul>
 *   <li>{@code /public/**}, {@code /actuator/health} : ouverts ;</li>
 *   <li>documentation OpenAPI : ouverte, mais servie en profil {@code dev} seulement (KI-19) ;</li>
 *   <li>{@code /admin/**} : authentification obligatoire (compte administrateur et connexion : étapes 33 et 34) ;</li>
 *   <li>toute autre route : refusée.</li>
 * </ul>
 * Refus rendus par {@code GlobalExceptionHandler} ({@code ProblemDetail} et {@code code}, D-CL) : 401
 * {@code AUTHENTICATION_REQUIRED} sans authentification, 403 {@code ACCESS_DENIED} sinon (dont CSRF, actif par
 * défaut, réglé à l'étape 35). Aucune requête refusée n'est mémorisée en session : une API ne redirige pas après
 * connexion, et un visiteur anonyme ne doit pas recevoir de session.
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
        return http
            .authorizeHttpRequests(requests -> requests
                // Rendu d'une erreur par le conteneur : la décision a déjà été prise pour la requête d'origine.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/public/**", "/actuator/health").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/admin/**").authenticated()
                .anyRequest().denyAll())
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    exceptionResolver.resolveException(request, response, null, exception))
                .accessDeniedHandler((request, response, exception) ->
                    exceptionResolver.resolveException(request, response, null, exception)))
            .build();
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
