package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;

public record ProfessionalLinkResponse(String label, String url) {

    static ProfessionalLinkResponse from(ProfessionalLink link) {
        return new ProfessionalLinkResponse(link.label(), link.url());
    }
}
