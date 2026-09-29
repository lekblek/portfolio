package com.scalke.portfolio.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D-DD : la configuration commune est celle de la production ; les valeurs de développement (répertoire relatif,
 * Mailpit, adresses locales) ne s'appliquent qu'en profil {@code dev}. Sans variable d'environnement, ces propriétés
 * restent vides, ce qui fait échouer le démarrage en nommant la propriété (validations de
 * {@code MediaStorageProperties}, {@code ContactNotificationProperties}, {@code EmailContactNotificationSender}).
 * Environnement sans variables système : le résultat ne dépend pas du poste.
 */
class ConfigurationDefaultsTest {

    private static final List<String> PRODUCTION_SENSITIVE = List.of(
        "portfolio.media.storage-root",
        "spring.mail.host",
        "portfolio.contact.notification.from",
        "portfolio.contact.notification.to");

    @Test
    void production_configuration_has_no_development_defaults() throws IOException {
        ConfigurableEnvironment environment = environment("application.yaml");

        for (String property : PRODUCTION_SENSITIVE) {
            assertThat(environment.getProperty(property)).as(property).isEmpty();
        }
    }

    @Test
    void development_profile_provides_local_defaults() throws IOException {
        ConfigurableEnvironment environment = environment("application-dev.yaml", "application.yaml");

        assertThat(environment.getProperty("portfolio.media.storage-root")).isEqualTo("uploads");
        assertThat(environment.getProperty("spring.mail.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("spring.mail.port")).isEqualTo("1025");
        assertThat(environment.getProperty("portfolio.contact.notification.to")).isEqualTo("admin@localhost");
    }

    /**
     * Fichiers par ordre de priorité décroissante, comme Spring Boot empile un profil au-dessus de la configuration
     * commune.
     */
    private static ConfigurableEnvironment environment(String... files) throws IOException {
        ConfigurableEnvironment environment = new AbstractEnvironment() { };
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (String file : files) {
            for (PropertySource<?> source : loader.load(file, new ClassPathResource(file))) {
                environment.getPropertySources().addLast(source);
            }
        }
        return environment;
    }
}
