package com.scalke.portfolio.backend.shared.api;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D-DB : le filtre précède la chaîne de sécurité ; toute réponse porte son identifiant, y compris un refus.
 */
class RequestIdIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void every_response_carries_a_request_id() throws Exception {
        mockMvc.perform(get("/api/public/projects").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(header().string(RequestIdFilter.HEADER, matchesPattern("[0-9a-f-]{36}")));
        mockMvc.perform(get("/api/admin/projects").contextPath("/api"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(RequestIdFilter.HEADER, matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void keeps_the_id_set_by_the_reverse_proxy() throws Exception {
        mockMvc.perform(get("/api/public/projects").contextPath("/api").header(RequestIdFilter.HEADER, "caddy-1"))
            .andExpect(header().string(RequestIdFilter.HEADER, "caddy-1"));
    }
}
