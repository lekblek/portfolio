package com.scalke.portfolio.backend.profile.application.usecase;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.media.application.query.PublicDocument;
import com.scalke.portfolio.backend.media.application.query.PublicImage;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.port.ProfileRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetProfileUseCase {

    private final ProfileRepository profileRepository;
    private final MediaQueryService media;

    /**
     * Profil et médias par la façade du module {@code media} (D-BX) : une requête par média présent, aucune sinon.
     * Un avatar qui n'est pas une image, ou un CV qui n'est pas un PDF, n'est pas exposé.
     */
    @Transactional(readOnly = true)
    public PublicProfile execute() {
        Profile profile = profileRepository.find()
            .orElseThrow(() -> new ResourceNotFoundException(
                ErrorCode.RESOURCE_NOT_FOUND, "Aucun profil n'est disponible."));
        return new PublicProfile(profile, avatar(profile), cv(profile));
    }

    private PublicImage avatar(Profile profile) {
        Long id = profile.avatarMediaId();
        return id == null ? null : media.imagesById(Set.of(id)).get(id);
    }

    private PublicDocument cv(Profile profile) {
        Long id = profile.cvMediaId();
        return id == null ? null : media.documentsById(Set.of(id)).get(id);
    }
}
