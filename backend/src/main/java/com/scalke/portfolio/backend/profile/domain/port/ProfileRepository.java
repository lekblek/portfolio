package com.scalke.portfolio.backend.profile.domain.port;

import com.scalke.portfolio.backend.profile.domain.model.Profile;

import java.util.Optional;

/**
 * Port de persistance du profil.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) :
 * {@code find} par {@code GetProfileUseCase}, {@code save} par le seed de développement.
 */
public interface ProfileRepository {

    Optional<Profile> find();

    Profile save(Profile profile);
}
