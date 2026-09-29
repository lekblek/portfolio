package com.scalke.portfolio.backend.security.infrastructure;

import com.scalke.portfolio.backend.security.domain.model.LoginAttempts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;

import java.time.Duration;
import java.util.List;

/**
 * Authentification de l'administrateur (D-CN, D-CO, D-CQ) : encodeur, vérification des identifiants, stratégie de
 * session à la connexion, limite des tentatives. Séparée de {@link SecurityConfiguration}, que les tests de
 * tranche web importent seule : ils n'ont pas besoin de ces beans, ni du service des comptes dont ils dépendent.
 */
@Configuration
public class AdminAuthenticationConfiguration {

    /**
     * Encodeur délégué de Spring Security (D-CN) : bcrypt aujourd'hui, empreintes préfixées ({@code {bcrypt}}) pour
     * pouvoir changer d'algorithme sans invalider les empreintes existantes.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Vérification des identifiants par le compte unique ({@link AdminUserDetailsService}) : un identifiant inconnu
     * coûte le même calcul bcrypt qu'un mauvais mot de passe (D-CO).
     */
    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Limite des connexions manquées (D-CQ) : 5 échecs par adresse en 15 minutes glissantes.
     */
    @Bean
    LoginAttempts loginAttempts() {
        return new LoginAttempts(5, Duration.ofMinutes(15));
    }

    /**
     * À la connexion : nouvel identifiant de session (contre la fixation de session) et nouveau jeton CSRF.
     */
    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(CookieCsrfTokenRepository csrfTokenRepository) {
        return new CompositeSessionAuthenticationStrategy(List.of(
            new ChangeSessionIdAuthenticationStrategy(),
            new CsrfAuthenticationStrategy(csrfTokenRepository)));
    }
}
