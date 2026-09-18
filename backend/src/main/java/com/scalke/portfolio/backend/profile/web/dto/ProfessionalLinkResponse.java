package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;

public record ProfessionalLinkResponse(
    String label,
    String url,
    Integer displayOrder
) {

    static ProfessionalLinkResponse from(ProfessionalLink link) {
        return new ProfessionalLinkResponse(link.label(), link.url(), link.displayOrder());
    }
}
