package com.scalke.portfolio.backend.profile.application.usecase;

import com.scalke.portfolio.backend.media.application.query.PublicDocument;
import com.scalke.portfolio.backend.media.application.query.PublicImage;
import com.scalke.portfolio.backend.profile.domain.model.Profile;

/**
 * Profil et ses médias sous forme publique (D-BX) : avatar ({@code null} si aucun) et CV ({@code null} si aucun).
 */
public record PublicProfile(Profile profile, PublicImage avatar, PublicDocument cv) {
}
