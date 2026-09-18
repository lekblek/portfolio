package com.scalke.portfolio.backend.profile.domain.port;

import com.scalke.portfolio.backend.profile.domain.model.Profile;

import java.util.Optional;

public interface ProfileRepository {
    Optional<Profile> findById(Long id);

    Optional<Profile> find();

    Profile save(Profile profile);

    void delete(Profile profile);

    boolean exists();
}
