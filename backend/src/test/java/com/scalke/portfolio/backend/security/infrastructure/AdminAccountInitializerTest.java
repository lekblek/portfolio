package com.scalke.portfolio.backend.security.infrastructure;

import com.scalke.portfolio.backend.security.application.usecase.AdminCredentials;
import com.scalke.portfolio.backend.security.application.usecase.InitializeAdminAccountUseCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AdminAccountInitializerTest {

    private final InitializeAdminAccountUseCase useCase = mock(InitializeAdminAccountUseCase.class);

    @Test
    void leaves_the_account_alone_when_nothing_is_configured() {
        new AdminAccountInitializer(new AdminAccountProperties("", " "), useCase).run(null);

        verify(useCase, never()).execute(any());
    }

    /**
     * Une configuration à moitié renseignée est une erreur : le démarrage échoue plutôt que de l'ignorer.
     */
    @Test
    void refuses_a_login_without_password() {
        assertThatThrownBy(() -> new AdminAccountInitializer(new AdminAccountProperties("admin", null), useCase).run(null))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ADMIN_PASSWORD");
        verify(useCase, never()).execute(any());
    }

    @Test
    void initializes_the_configured_account() {
        new AdminAccountInitializer(new AdminAccountProperties(" admin ", "correct-horse-battery"), useCase).run(null);

        verify(useCase).execute(new AdminCredentials("admin", "correct-horse-battery"));
    }
}
