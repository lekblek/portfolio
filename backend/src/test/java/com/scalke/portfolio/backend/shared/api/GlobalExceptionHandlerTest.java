package com.scalke.portfolio.backend.shared.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ErrorHandlingTestController.class)
// Rendu des erreurs seulement : les règles de sécurité sont testées par SecurityConfigurationIT.
@AutoConfigureMockMvc(addFilters = false)
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

    /**
     * D-BS (KI-23) : format refusé → 415, fichier trop volumineux → 413.
     */
    @Test
    void renders_a_rejected_upload_with_its_http_status() throws Exception {
        mockMvc.perform(get("/test-errors/unsupported-content"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_FORMAT"));
        mockMvc.perform(get("/test-errors/too-large"))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.code").value("MEDIA_TOO_LARGE"));
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

    /**
     * D-CU : une valeur refusée par un cas d'usage a la forme d'un échec de validation du corps.
     */
    @Test
    void renders_an_invalid_input_like_a_validation_failure() throws Exception {
        mockMvc.perform(get("/test-errors/invalid-input"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.detail").value("Catégorie inconnue."))
            .andExpect(jsonPath("$.errors[0].field").value("categoryId"))
            .andExpect(jsonPath("$.errors[0].message").value("Catégorie inconnue."));
    }

    /**
     * D-CF : une exception de Spring arrivée sans corps (paramètre obligatoire absent) reçoit elle aussi son code.
     */
    @Test
    void gives_a_code_to_a_missing_required_parameter() throws Exception {
        mockMvc.perform(get("/test-errors/required-param"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
            .andExpect(jsonPath("$.detail").value("Required parameter 'value' is not present."));
    }

    /**
     * D-CF : une contrainte sur un paramètre de requête a la même forme qu'un échec de validation d'un corps.
     */
    @Test
    void renders_request_parameter_constraint_failures_with_field_details() throws Exception {
        mockMvc.perform(get("/test-errors/validate-param").param("value", "trop long"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("value"))
            .andExpect(jsonPath("$.errors[0].message").value("au plus 3 caractères"));
    }

    /**
     * D-CL : une authentification absente ou invalide est une 401 codée.
     */
    @Test
    void renders_a_missing_authentication_as_problem_detail() throws Exception {
        mockMvc.perform(get("/test-errors/unauthenticated"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    /**
     * D-CQ : 429 avec l'attente en secondes entières, arrondie au-dessus.
     */
    @Test
    void renders_too_many_requests_with_a_retry_delay() throws Exception {
        mockMvc.perform(get("/test-errors/too-many"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "91"))
            .andExpect(jsonPath("$.code").value("TOO_MANY_LOGIN_ATTEMPTS"));
    }

    /**
     * KI-20 : un accès refusé est une 403 codée, pas une 500 du gestionnaire générique.
     */
    @Test
    void renders_a_denied_access_as_forbidden() throws Exception {
        mockMvc.perform(get("/test-errors/access-denied"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
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
