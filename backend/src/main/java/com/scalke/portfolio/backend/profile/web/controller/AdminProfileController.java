package com.scalke.portfolio.backend.profile.web.controller;

import com.scalke.portfolio.backend.profile.application.usecase.GetAdminProfileUseCase;
import com.scalke.portfolio.backend.profile.application.usecase.UpdateProfileUseCase;
import com.scalke.portfolio.backend.profile.web.dto.AdminProfileResponse;
import com.scalke.portfolio.backend.profile.web.dto.SaveProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administration du profil (D-CY), ressource unique derrière la session de l'administrateur : lu tel qu'il est saisi,
 * remplacé d'un bloc (créé s'il n'existe pas).
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/profile")
public class AdminProfileController {

    private final GetAdminProfileUseCase getAdminProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;

    @GetMapping
    AdminProfileResponse get() {
        return AdminProfileResponse.from(getAdminProfileUseCase.execute());
    }

    @PutMapping
    AdminProfileResponse update(@Valid @RequestBody SaveProfileRequest body) {
        return AdminProfileResponse.from(updateProfileUseCase.execute(body.toDraft()));
    }
}
