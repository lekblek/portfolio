package com.scalke.portfolio.backend.profile.application.usecase;

import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.port.ProfileRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Le profil tel qu'il est saisi, médias par identifiant, pour l'administration (D-CY). Absent : 404, et le premier
 * {@code PUT} le crée.
 */
@Service
@RequiredArgsConstructor
public class GetAdminProfileUseCase {

    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public Profile execute() {
        return profileRepository.find()
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Aucun profil n'est saisi."));
    }
}
