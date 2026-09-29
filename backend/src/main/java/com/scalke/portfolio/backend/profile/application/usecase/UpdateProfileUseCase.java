package com.scalke.portfolio.backend.profile.application.usecase;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.profile.domain.model.Certification;
import com.scalke.portfolio.backend.profile.domain.model.Education;
import com.scalke.portfolio.backend.profile.domain.model.Experience;
import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import com.scalke.portfolio.backend.profile.domain.port.ProfileRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.error.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Crée ou remplace tout le profil, collections comprises (D-CY) : chaque collection est rangée dans l'ordre saisi
 * (ordre d'affichage 0, 1, …). Refus sur le champ en cause (400 {@code VALIDATION_FAILED}) : période qui se termine
 * avant de commencer (invariant 17), compétence nommée deux fois ({@code skill_unique_name}), avatar qui n'est pas
 * une image, CV qui n'est pas un PDF (D06, D-BX).
 */
@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase {

    private final ProfileRepository profileRepository;
    private final MediaQueryService media;

    @Transactional
    public Profile execute(ProfileDraft draft) {
        verifyMedia(draft);
        Set<String> skillNames = new HashSet<>();
        for (ProfileDraft.Skill skill : draft.skills()) {
            if (!skillNames.add(skill.name().toLowerCase(Locale.ROOT))) {
                throw new InvalidInputException("skills", "Une compétence figure deux fois : " + skill.name() + ".");
            }
        }
        return profileRepository.save(new Profile(
            draft.displayName(), draft.professionalTitle(), draft.shortBio(), draft.aboutMarkdown(),
            draft.publicLocation(), draft.publicEmail(),
            indexed(draft.links(), (link, order) -> new ProfessionalLink(null, link.label(), link.url(), order)),
            indexed(draft.skills(), (skill, order) -> new Skill(null, skill.name(), skill.category(), order)),
            indexed(draft.experiences(), (entry, order) -> new Experience(null, entry.organization(), entry.title(),
                entry.location(), period("experiences", order, entry.startDate(), entry.endDate()),
                entry.description(), order)),
            indexed(draft.educations(), (entry, order) -> new Education(null, entry.institution(), entry.degree(),
                entry.field(), entry.location(), period("educations", order, entry.startDate(), entry.endDate()),
                entry.description(), order)),
            indexed(draft.certifications(), (entry, order) -> {
                if (entry.expiresAt() != null && entry.expiresAt().isBefore(entry.issuedAt())) {
                    throw new InvalidInputException("certifications[" + order + "].expiresAt",
                        "L'expiration précède la délivrance.");
                }
                return new Certification(null, entry.name(), entry.issuer(), entry.issuedAt(), entry.expiresAt(),
                    entry.credentialUrl(), order);
            }),
            draft.avatarMediaId(),
            draft.cvMediaId()));
    }

    private void verifyMedia(ProfileDraft draft) {
        Long avatar = draft.avatarMediaId();
        if (avatar != null && !media.imagesById(Set.of(avatar)).containsKey(avatar)) {
            throw new InvalidInputException("avatarMediaId", "Image inconnue.");
        }
        Long cv = draft.cvMediaId();
        if (cv != null && !media.documentsById(Set.of(cv)).containsKey(cv)) {
            throw new InvalidInputException("cvMediaId", "Document PDF inconnu.");
        }
    }

    private static DateRange period(String collection, int index, LocalDate start, LocalDate end) {
        if (end != null && end.isBefore(start)) {
            throw new InvalidInputException(collection + "[" + index + "].endDate",
                "La date de fin précède la date de début.");
        }
        return new DateRange(start, end);
    }

    private static <T, R> List<R> indexed(List<T> entries, Indexed<T, R> mapper) {
        return IntStream.range(0, entries.size()).mapToObj(index -> mapper.apply(entries.get(index), index)).toList();
    }

    @FunctionalInterface
    private interface Indexed<T, R> {

        R apply(T entry, int index);
    }
}
