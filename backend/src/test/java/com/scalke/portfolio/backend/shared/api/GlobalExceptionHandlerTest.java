package com.scalke.portfolio.backend.shared.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ErrorHandlingTestController.class)
@Import(GlobalExceptionHandler.class)
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void renders_a_business_not_found_as_problem_detail() throws Exception {
        mockMvc.perform(get("/test-errors/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.detail").value("Publication introuvable."))
            .andExpect(jsonPath("$.instance").value("/test-errors/not-found"));
    }

    @Test
    void renders_a_business_conflict_as_problem_detail() throws Exception {
        mockMvc.perform(get("/test-errors/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SLUG_ALREADY_USED"));
    }

    @Test
    void renders_validation_failures_with_field_details() throws Exception {
        mockMvc.perform(post("/test-errors/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"  \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void never_leaks_internal_details_on_unexpected_errors() throws Exception {
        mockMvc.perform(get("/test-errors/boom"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.detail")
                .value("Une erreur inattendue est survenue."))
            .andExpect(content().string(
                org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("s3cr3t"))));
    }
}
