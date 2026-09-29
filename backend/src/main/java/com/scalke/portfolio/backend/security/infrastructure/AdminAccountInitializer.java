package com.scalke.portfolio.backend.security.infrastructure;

import com.scalke.portfolio.backend.security.application.usecase.AdminCredentials;
import com.scalke.portfolio.backend.security.application.usecase.InitializeAdminAccountUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Initialise le compte administrateur au démarrage, dans tous les profils (D18, D-CN).
 * <ul>
 *   <li>rien de configuré : avertissement, l'administration reste fermée, le site public fonctionne ;</li>
 *   <li>configuration incomplète ou invalide : échec du démarrage, avec la règle en cause (jamais le mot de passe) ;</li>
 *   <li>sinon : compte créé, aligné ou inchangé.</li>
 * </ul>
 */
@Component
@EnableConfigurationProperties(AdminAccountProperties.class)
@RequiredArgsConstructor
@Slf4j
class AdminAccountInitializer implements ApplicationRunner {

    private final AdminAccountProperties properties;
    private final InitializeAdminAccountUseCase initializeAdminAccountUseCase;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.warn("ADMIN_USERNAME et ADMIN_PASSWORD absents : compte administrateur non initialisé");
            return;
        }
        AdminCredentials credentials = new AdminCredentials(
            nonNull(properties.username()).strip(), nonNull(properties.password()));
        InitializeAdminAccountUseCase.Outcome outcome = initializeAdminAccountUseCase.execute(credentials);
        log.info("Compte administrateur {} : {}", credentials.login(), outcome);
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}
