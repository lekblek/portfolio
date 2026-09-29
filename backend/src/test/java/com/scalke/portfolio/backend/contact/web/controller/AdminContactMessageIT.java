package com.scalke.portfolio.backend.contact.web.controller;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import com.scalke.portfolio.backend.contact.domain.port.ContactMessageRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static com.scalke.portfolio.backend.testsupport.CsrfTestSupport.xsrf;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boîte de réception de bout en bout (D-CZ) : session, liste filtrable, lecture sans effet, cycle en avant seulement
 * (invariant 29).
 */
@Transactional
class AdminContactMessageIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ContactMessageRepository contactMessageRepository;

    @Test
    void requires_the_administrator_session_and_the_csrf_token() throws Exception {
        mockMvc.perform(get("/api/admin/contact-messages").contextPath("/api"))
            .andExpect(status().isUnauthorized());
        long id = message("Alice", ContactStatus.NEW, NOW).id();
        mockMvc.perform(post("/api/admin/contact-messages/" + id + "/status").contextPath("/api").with(user("admin"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READ\"}"))
            .andExpect(status().isForbidden());
    }

    /**
     * Les plus récents d'abord (l'identifiant départage les égalités), sans le texte ; filtre par statut.
     */
    @Test
    void lists_the_newest_messages_first_and_filters_by_status() throws Exception {
        ContactMessage recent = message("Bruno", ContactStatus.NEW, NOW);
        ContactMessage sameTime = message("Chloé", ContactStatus.NEW, NOW);
        ContactMessage old = message("Alice", ContactStatus.READ, NOW.minus(Duration.ofDays(2)));

        mockMvc.perform(get("/api/admin/contact-messages").contextPath("/api").with(user("admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].id").value(sameTime.id()))
            .andExpect(jsonPath("$.content[1].id").value(recent.id()))
            .andExpect(jsonPath("$.content[2].id").value(old.id()))
            .andExpect(jsonPath("$.content[0].subject").value("Sujet de Chloé"))
            .andExpect(jsonPath("$.content[0].message").doesNotExist());
        mockMvc.perform(get("/api/admin/contact-messages?status=READ").contextPath("/api").with(user("admin")))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Alice"));
        mockMvc.perform(get("/api/admin/contact-messages?status=SPAM").contextPath("/api").with(user("admin")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    /**
     * Lire un message ne change pas son statut.
     */
    @Test
    void reads_a_message_without_changing_its_status() throws Exception {
        long id = message("Alice", ContactStatus.NEW, NOW).id();

        mockMvc.perform(get("/api/admin/contact-messages/" + id).contextPath("/api").with(user("admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("alice@example.test"))
            .andExpect(jsonPath("$.message").value("Bonjour de Alice."))
            .andExpect(jsonPath("$.status").value("NEW"));
        assertThat(contactMessageRepository.findById(id).orElseThrow().status()).isEqualTo(ContactStatus.NEW);
        mockMvc.perform(get("/api/admin/contact-messages/999999").contextPath("/api").with(user("admin")))
            .andExpect(status().isNotFound());
    }

    /**
     * Invariant 29 : un message avance, éventuellement en sautant des étapes, et ne revient jamais en arrière.
     */
    @Test
    void moves_a_message_forward_only() throws Exception {
        long id = message("Alice", ContactStatus.NEW, NOW.minus(Duration.ofHours(1))).id();

        changeStatus(id, "{\"status\":\"PROCESSED\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PROCESSED"))
            .andExpect(jsonPath("$.updatedAt").value(NOW.toString()));
        changeStatus(id, "{\"status\":\"READ\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_CONTACT_MESSAGE_TRANSITION"));
        changeStatus(id, "{}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("status"));
        changeStatus(999_999L, "{\"status\":\"READ\"}").andExpect(status().isNotFound());
    }

    private ContactMessage message(String name, ContactStatus status, Instant receivedAt) {
        return contactMessageRepository.create(new ContactMessage(null, name,
            name.toLowerCase().replace("é", "e") + "@example.test", "Sujet de " + name, "Bonjour de " + name + ".",
            status, receivedAt, receivedAt));
    }

    private ResultActions changeStatus(long id, String json) throws Exception {
        return mockMvc.perform(post("/api/admin/contact-messages/" + id + "/status").contextPath("/api")
            .with(user("admin")).with(xsrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
