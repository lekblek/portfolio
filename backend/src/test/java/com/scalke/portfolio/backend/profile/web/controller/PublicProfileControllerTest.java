package com.scalke.portfolio.backend.profile.web.controller;

import com.scalke.portfolio.backend.profile.application.usecase.GetProfileUseCase;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper.ProfilePersistenceMapper;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
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
        ProfileEntity profile = new ProfileEntity(
            "Blek Gedeon Ngossanga",
            "Développeur full-stack",
            "Je conçois des solutions modernes et évolutives.");
        profile.addLink("GitHub", "https://example.test/gh", 0);
        profile.addSkill("Spring Boot", "Backend", 0);
        profile.addSkill("Angular", "Frontend", 1);
        given(getProfileUseCase.execute()).willReturn(
            ProfilePersistenceMapper.toDomain(profile)
        );

        mockMvc.perform(get("/api/public/profile").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Blek Gedeon Ngossanga"))
            .andExpect(jsonPath("$.publicLocation").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.links[0].label").value("GitHub"))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.skillGroups[0].category").value("Backend"))
            .andExpect(jsonPath("$.skillGroups[0].skills[0].name").value("Spring Boot"))
            .andExpect(jsonPath("$.skillGroups[0].skills[0].category").doesNotExist());
    }

    @Test
    void returns_problem_details_when_no_profile_exists() throws Exception {
        given(getProfileUseCase.execute())
            .willThrow(new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,"Aucun profil n'est disponible."));

        mockMvc.perform(get("/api/public/profile").contextPath("/api"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
