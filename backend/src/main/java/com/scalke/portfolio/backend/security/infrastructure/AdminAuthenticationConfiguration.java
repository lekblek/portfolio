package com.scalke.portfolio.backend.security.infrastructure;

import com.scalke.portfolio.backend.security.domain.model.LoginAttempts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Authentification de l'administrateur (D-CN, D-CO, D-CQ) : encodeur, vérification des identifiants, stratégie de
 * session à la connexion, limite des tentatives. Séparée de {@link SecurityConfiguration}, que les tests de
 * tranche web importent seule : ils n'ont pas besoin de ces beans, ni du service des comptes dont ils dépendent.
 */
@Configuration
public class AdminAuthenticationConfiguration {

    /**
     * Coût bcrypt (D-DK) : 2^12 tours, environ un quart de seconde par calcul ; au-dessus du minimum recommandé (10,
     * défaut de Spring Security). Une connexion par administrateur unique, derrière une limite des essais (D-CQ).
     */
    static final int BCRYPT_COST = 12;

    /**
     * Encodeur délégué de Spring Security (D-CN) : empreintes préfixées ({@code {bcrypt}}) pour pouvoir changer
     * d'algorithme sans invalider les empreintes existantes. Seul bcrypt est déclaré, comme en SQL (invariant 15). Une
     * empreinte d'un coût inférieur est « à mettre à niveau » ({@code upgradeEncoding}) : recalculée au démarrage.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder(BCRYPT_COST)));
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
