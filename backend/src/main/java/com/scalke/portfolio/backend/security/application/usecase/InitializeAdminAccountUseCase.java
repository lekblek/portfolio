package com.scalke.portfolio.backend.security.application.usecase;

import com.scalke.portfolio.backend.security.domain.model.AdminAccount;
import com.scalke.portfolio.backend.security.domain.port.AdminAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Crée ou aligne le compte administrateur unique sur la configuration (D18, D-CN) : la configuration fait foi.
 * Changer {@code ADMIN_PASSWORD} puis redémarrer change le mot de passe ; un mot de passe inchangé n'est pas
 * haché de nouveau (une empreinte bcrypt change à chaque calcul), sauf si son encodage est à mettre à niveau.
 */
@Service
@RequiredArgsConstructor
public class InitializeAdminAccountUseCase {

    public enum Outcome {
        CREATED,
        UPDATED,
        UNCHANGED
    }

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public Outcome execute(AdminCredentials credentials) {
        Instant now = clock.instant();
        Optional<AdminAccount> existing = adminAccountRepository.find();
        if (existing.isEmpty()) {
            adminAccountRepository.create(
                AdminAccount.create(credentials.login(), passwordEncoder.encode(credentials.password()), now));
            return Outcome.CREATED;
        }
        AdminAccount account = existing.get();
        boolean samePassword = passwordEncoder.matches(credentials.password(), account.passwordHash())
            && !passwordEncoder.upgradeEncoding(account.passwordHash());
        if (samePassword && account.login().equals(credentials.login())) {
            return Outcome.UNCHANGED;
        }
        String passwordHash = samePassword ? account.passwordHash() : passwordEncoder.encode(credentials.password());
        adminAccountRepository.updateCredentials(account.withCredentials(credentials.login(), passwordHash, now));
        return Outcome.UPDATED;
    }
}
