package com.scalke.portfolio.backend.testsupport;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

/**
 * Serveur SMTP en mémoire (GreenMail) des tests d'intégration (D-CJ), importé par {@link AbstractIntegrationTest} :
 * un seul contexte Spring, aucun courriel réel. Port libre choisi une fois pour tout le contexte et conservé si un
 * test arrête puis relance le serveur (panne SMTP simulée).
 */
@TestConfiguration(proxyBeanMethods = false)
public class MailTestConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    GreenMail greenMail() {
        return new GreenMail(new ServerSetup(freePort(), "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
    }

    @Bean
    DynamicPropertyRegistrar mailProperties(GreenMail greenMail) {
        return registry -> {
            registry.add("spring.mail.host", () -> "127.0.0.1");
            registry.add("spring.mail.port", () -> greenMail.getSmtp().getPort());
        };
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
