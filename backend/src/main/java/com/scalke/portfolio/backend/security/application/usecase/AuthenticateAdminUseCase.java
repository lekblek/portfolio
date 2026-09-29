package com.scalke.portfolio.backend.security.application.usecase;

import com.scalke.portfolio.backend.security.domain.model.AdminAccount;
import com.scalke.portfolio.backend.security.domain.model.LoginAttempts;
import com.scalke.portfolio.backend.security.domain.port.AdminAccountRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Vérifie l'identifiant et le mot de passe de l'administrateur et enregistre la date de connexion (D-CO). Un échec
 * lève une {@code AuthenticationException} (401 {@code INVALID_CREDENTIALS}), identique quelle qu'en soit la cause.
 * Trop d'échecs depuis la même source : 429 {@code TOO_MANY_LOGIN_ATTEMPTS}, sans même vérifier le mot de passe
 * (D-CQ). L'ouverture de la session HTTP revient au contrôleur.
 */
@Service
@RequiredArgsConstructor
public class AuthenticateAdminUseCase {

    private final AuthenticationManager authenticationManager;
    private final AdminAccountRepository adminAccountRepository;
    private final LoginAttempts loginAttempts;
    private final Clock clock;

    /**
     * @param source adresse du client, clé de la limite des tentatives
     */
    @Transactional
    public AuthenticatedAdmin execute(String login, String password, String source) {
        // PostgreSQL garde la microseconde : la date renvoyée est celle qui sera relue.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Optional<Duration> retryAfter = loginAttempts.retryAfter(source, now);
        if (retryAfter.isPresent()) {
            throw new TooManyRequestsException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS,
                "Trop de tentatives de connexion : réessayer plus tard.", retryAfter.get());
        }
        Authentication authentication;
        try {
            authentication = authenticate(login, password);
        } catch (AuthenticationException failure) {
            loginAttempts.recordFailure(source, now);
            throw failure;
        }
        loginAttempts.reset(source);
        AdminAccount account = adminAccountRepository.find().orElseThrow();
        return new AuthenticatedAdmin(authentication, adminAccountRepository.recordLogin(account.loggedInAt(now)));
    }

    private Authentication authenticate(String login, String password) {
        // bcrypt ne compare que les 72 premiers octets : sans cette garde, le bon mot de passe suivi de n'importe quoi
        // serait accepté. Un mot de passe plus long ne peut pas être le bon (D-CN).
        if (password.getBytes(StandardCharsets.UTF_8).length > AdminCredentials.PASSWORD_MAX_BYTES) {
            throw new BadCredentialsException("password too long");
        }
        return authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(login, password));
    }
}
