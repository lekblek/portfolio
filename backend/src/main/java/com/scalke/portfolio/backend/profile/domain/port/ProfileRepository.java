package com.scalke.portfolio.backend.profile.domain.port;

import com.scalke.portfolio.backend.profile.domain.model.Profile;

import java.util.Optional;

/**
 * Port de persistance du profil.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) :
 * {@code find} par {@code GetProfileUseCase} et {@code GetAdminProfileUseCase}, {@code save} par le seed de
 * développement et {@code UpdateProfileUseCase} (D-CY).
 */
public interface ProfileRepository {

    Optional<Profile> find();

    /**
     * Crée le profil ou remplace le profil existant, collections comprises.
     *
     * @throws com.scalke.portfolio.backend.shared.error.InvalidInputException média supprimé entre-temps
     */
    Profile save(Profile profile);
}
