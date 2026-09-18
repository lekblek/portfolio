package com.scalke.portfolio.backend.profile.web.controller;

import com.scalke.portfolio.backend.profile.application.usecase.GetProfileUseCase;
import com.scalke.portfolio.backend.profile.web.dto.ProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/public/profile")
public class PublicProfileController {

    private final GetProfileUseCase getProfileUseCase;

    @GetMapping
    ProfileResponse getProfile() {
        return ProfileResponse.from(getProfileUseCase.execute());
    }
}
