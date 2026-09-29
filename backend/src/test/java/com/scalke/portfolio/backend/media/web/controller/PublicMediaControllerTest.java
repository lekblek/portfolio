package com.scalke.portfolio.backend.media.web.controller;

import com.scalke.portfolio.backend.media.application.usecase.MediaFile;
import com.scalke.portfolio.backend.media.application.usecase.OpenMediaFileUseCase;
import com.scalke.portfolio.backend.media.domain.model.MediaContent;
import com.scalke.portfolio.backend.media.domain.model.MediaFormat;
import com.scalke.portfolio.backend.security.infrastructure.SecurityConfiguration;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicMediaController.class)
@Import(SecurityConfiguration.class)
class PublicMediaControllerTest {

    private static final String KEY = "3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.pdf";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OpenMediaFileUseCase openMediaFileUseCase;

    /**
     * D-BP : type MIME du format, taille, cache immuable, pas de détection de type par le navigateur. Le point
     * de l'extension appartient à la clé (pas de correspondance de suffixe).
     */
    @Test
    void serves_the_raw_file_with_its_type_and_an_immutable_cache() throws Exception {
        byte[] bytes = "%PDF-1.7".getBytes();
        given(openMediaFileUseCase.execute(KEY))
            .willReturn(new MediaFile(MediaFormat.PDF, new MediaContent(bytes.length, new ByteArrayInputStream(bytes))));

        mockMvc.perform(get("/api/public/media/" + KEY).contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().longValue("Content-Length", bytes.length))
            .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(content().bytes(bytes));
    }

    @Test
    void returns_problem_details_for_an_unknown_media() throws Exception {
        given(openMediaFileUseCase.execute(KEY))
            .willThrow(new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Média introuvable."));

        mockMvc.perform(get("/api/public/media/" + KEY).contextPath("/api"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.detail").value("Média introuvable."));
    }
}
