package com.scalke.portfolio.backend.profile.web.controller;

import com.scalke.portfolio.backend.profile.application.usecase.GetProfileUseCase;
import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicProfileController.class)
class PublicProfileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetProfileUseCase getProfileUseCase;

    @Test
    void returns_the_public_profile() throws Exception {
        given(getProfileUseCase.execute()).willReturn(new Profile(
            "Blek Gedeon Ngossanga",
            "Développeur full-stack",
            "Je conçois des solutions modernes et évolutives.",
            null,
            null,
            null,
            List.of(new ProfessionalLink(1L, "GitHub", "https://example.test/gh", 0)),
            List.of(
                new Skill(1L, "Spring Boot", "Backend", 0),
                new Skill(2L, "Angular", "Frontend", 1)),
            List.of(),
            List.of(),
            List.of()));

        mockMvc.perform(get("/api/public/profile").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Blek Gedeon Ngossanga"))
            .andExpect(jsonPath("$.publicLocation").value(nullValue()))
            .andExpect(jsonPath("$.links[0].label").value("GitHub"))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.skillGroups[0].category").value("Backend"))
            .andExpect(jsonPath("$.skillGroups[0].skills[0].name").value("Spring Boot"))
            .andExpect(jsonPath("$.skillGroups[0].skills[0].category").doesNotExist());
    }

    @Test
    void returns_problem_details_when_no_profile_exists() throws Exception {
        given(getProfileUseCase.execute())
            .willThrow(new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Aucun profil n'est disponible."));

        mockMvc.perform(get("/api/public/profile").contextPath("/api"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
