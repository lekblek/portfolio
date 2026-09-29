package com.scalke.portfolio.backend.security.application.usecase;

import com.scalke.portfolio.backend.security.application.usecase.InitializeAdminAccountUseCase.Outcome;
import com.scalke.portfolio.backend.security.domain.model.AdminAccount;
import com.scalke.portfolio.backend.security.domain.port.AdminAccountRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Initialisation du compte depuis la configuration (D18, D-CN), avec le vrai encodeur. Le contexte de test ne
 * configure aucun compte : chaque test part d'une table vide.
 */
@Transactional
class InitializeAdminAccountUseCaseIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "correct-horse-battery-staple";

    @Autowired
    InitializeAdminAccountUseCase initializeAdminAccountUseCase;

    @Autowired
    AdminAccountRepository adminAccountRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EntityManager entityManager;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void creates_the_account_with_a_hash_of_the_password() {
        assertThat(initializeAdminAccountUseCase.execute(new AdminCredentials("admin", PASSWORD))).isEqualTo(Outcome.CREATED);
        flushAndClear();

        AdminAccount account = adminAccountRepository.find().orElseThrow();
        assertThat(account.login()).isEqualTo("admin");
        assertThat(account.passwordHash()).startsWith("{bcrypt}").doesNotContain(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, account.passwordHash())).isTrue();
        assertThat(account.createdAt()).isEqualTo(NOW);
    }

    /**
     * Même configuration au redémarrage : rien n'est écrit, l'empreinte reste la même.
     */
    @Test
    void leaves_an_aligned_account_unchanged() {
        initializeAdminAccountUseCase.execute(new AdminCredentials("admin", PASSWORD));
        flushAndClear();
        String hash = storedHash();

        assertThat(initializeAdminAccountUseCase.execute(new AdminCredentials("admin", PASSWORD)))
            .isEqualTo(Outcome.UNCHANGED);
        flushAndClear();
        assertThat(storedHash()).isEqualTo(hash);
    }

    /**
     * La configuration fait foi : un nouveau mot de passe remplace l'ancien, qui ne permet plus de se connecter.
     */
    @Test
    void replaces_the_password_when_the_configuration_changes() {
        initializeAdminAccountUseCase.execute(new AdminCredentials("admin", PASSWORD));
        flushAndClear();

        assertThat(initializeAdminAccountUseCase.execute(new AdminCredentials("admin", "un-autre-mot-de-passe-long")))
            .isEqualTo(Outcome.UPDATED);
        flushAndClear();
        assertThat(passwordEncoder.matches("un-autre-mot-de-passe-long", storedHash())).isTrue();
        assertThat(passwordEncoder.matches(PASSWORD, storedHash())).isFalse();
    }

    @Test
    void renames_the_account_and_keeps_its_password() {
        initializeAdminAccountUseCase.execute(new AdminCredentials("admin", PASSWORD));
        flushAndClear();
        String hash = storedHash();

        assertThat(initializeAdminAccountUseCase.execute(new AdminCredentials("blek@example.com", PASSWORD)))
            .isEqualTo(Outcome.UPDATED);
        flushAndClear();
        assertThat(adminAccountRepository.find().orElseThrow().login()).isEqualTo("blek@example.com");
        assertThat(storedHash()).isEqualTo(hash);
    }

    private String storedHash() {
        return jdbcClient.sql("SELECT password_hash FROM admin_account").query(String.class).single();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
