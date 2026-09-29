package com.scalke.portfolio.backend.media.web.dto;

import com.scalke.portfolio.backend.media.domain.model.Media;
import jakarta.validation.constraints.Size;

/**
 * Modification d'un média (D-CT) : son texte alternatif seulement ; absent ou vide, il est retiré.
 */
public record UpdateMediaRequest(@Size(max = Media.MAX_ALT_TEXT_LENGTH) String altText) {
}
