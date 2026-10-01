package com.scalke.portfolio.backend.contact.web.controller;

import com.scalke.portfolio.backend.contact.application.usecase.ContactMessageSubmission;
import com.scalke.portfolio.backend.contact.application.usecase.SubmitContactMessageUseCase;
import com.scalke.portfolio.backend.security.infrastructure.SecurityConfiguration;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrat de la route publique du formulaire de contact (D-EJ) : validation, piège à robots, 202 sans corps, 429.
 */
@WebMvcTest(PublicContactMessageController.class)
@Import(SecurityConfiguration.class)
class PublicContactMessageControllerTest {

    private static final String VALID = """
        {"name":"Camille Martin","email":"camille@example.com","subject":"Proposition","message":"Bonjour."}""";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SubmitContactMessageUseCase submitContactMessageUseCase;

    private static MockHttpServletRequestBuilder send(String body) {
        return post("/api/public/contact-messages").contextPath("/api")
            .contentType(MediaType.APPLICATION_JSON).content(body)
            .with(request -> {
                request.setRemoteAddr("198.51.100.7");
                return request;
            });
    }

    @Test
    void accepts_a_message_without_csrf_token_and_without_body() throws Exception {
        mockMvc.perform(send(VALID))
            .andExpect(status().isAccepted())
            .andExpect(content().string(""));

        then(submitContactMessageUseCase).should().execute(
            new ContactMessageSubmission("Camille Martin", "camille@example.com", "Proposition", "Bonjour."),
            "198.51.100.7");
    }

    @Test
    void answers_a_filled_trap_like_a_success_and_records_nothing() throws Exception {
        mockMvc.perform(send(VALID.replace("}", ",\"website\":\"https://spam.example\"}")))
            .andExpect(status().isAccepted())
            .andExpect(content().string(""));

        then(submitContactMessageUseCase).shouldHaveNoInteractions();
    }

    @Test
    void reports_each_invalid_field() throws Exception {
        String invalid = """
            {"name":" ","email":"camille","subject":"%s","message":""}""".formatted("s".repeat(201));

        mockMvc.perform(send(invalid))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("name", "email", "subject", "message")));

        then(submitContactMessageUseCase).shouldHaveNoInteractions();
    }

    @Test
    void answers_too_many_messages_with_the_delay_to_wait() throws Exception {
        given(submitContactMessageUseCase.execute(any(), anyString())).willThrow(new TooManyRequestsException(
            ErrorCode.TOO_MANY_CONTACT_MESSAGES, "Trop de messages envoyés : réessayer plus tard.",
            Duration.ofMinutes(42)));

        mockMvc.perform(send(VALID))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "2520"))
            .andExpect(jsonPath("$.code").value("TOO_MANY_CONTACT_MESSAGES"));
    }
}
