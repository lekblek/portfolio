package com.scalke.portfolio.backend.contact.web.controller;

import com.icegreen.greenmail.util.GreenMail;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Formulaire de contact de bout en bout (D-EJ) : route ouverte sans session ni jeton CSRF, message enregistré et
 * notifié, piège à robots, limitation par adresse. <strong>Sans</strong> transaction de test : la notification suit
 * une validation réelle (D-CJ). Chaque test a sa propre adresse : la limite vit avec le contexte Spring partagé.
 */
class PublicContactMessageIT extends AbstractIntegrationTest {

    private static final String VALID = """
        {"name":"Camille Martin","email":"camille@example.com","subject":"Proposition","message":"Bonjour."}""";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    GreenMail greenMail;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void givenAnEmptyMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void removeTheMessages() {
        jdbcClient.sql("DELETE FROM contact_message").update();
    }

    private static MockHttpServletRequestBuilder send(String body, String address) {
        return post("/api/public/contact-messages").contextPath("/api")
            .contentType(MediaType.APPLICATION_JSON).content(body)
            .with(request -> {
                request.setRemoteAddr(address);
                return request;
            });
    }

    @Test
    void records_and_notifies_a_message_sent_anonymously() throws Exception {
        mockMvc.perform(send(VALID, "192.0.2.21"))
            .andExpect(status().isAccepted())
            .andExpect(header().doesNotExist("Set-Cookie"));

        assertThat(count()).isEqualTo(1);
        assertThat(greenMail.waitForIncomingEmail(5_000, 1)).isTrue();
        assertThat(greenMail.getReceivedMessages()[0].getSubject()).isEqualTo("[Portfolio] Proposition");
    }

    @Test
    void records_nothing_when_the_trap_is_filled() throws Exception {
        mockMvc.perform(send(VALID.replace("}", ",\"website\":\"https://spam.example\"}"), "192.0.2.22"))
            .andExpect(status().isAccepted());

        assertThat(count()).isZero();
        assertThat(greenMail.waitForIncomingEmail(1_000, 1)).isFalse();
    }

    @Test
    void refuses_the_sixth_message_of_the_hour_from_the_same_address() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(send(VALID, "192.0.2.23")).andExpect(status().isAccepted());
        }

        mockMvc.perform(send(VALID, "192.0.2.23"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "3600"))
            .andExpect(jsonPath("$.code").value("TOO_MANY_CONTACT_MESSAGES"));
        mockMvc.perform(send(VALID, "192.0.2.24")).andExpect(status().isAccepted());
        assertThat(count()).isEqualTo(6);
    }

    private long count() {
        return jdbcClient.sql("SELECT count(*) FROM contact_message").query(Long.class).single();
    }
}
