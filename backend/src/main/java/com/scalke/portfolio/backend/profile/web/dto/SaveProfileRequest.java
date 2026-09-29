package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.application.usecase.ProfileDraft;
import com.scalke.portfolio.backend.shared.api.InputPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * Saisie complète du profil (D-CY) : elle remplace le profil existant, collections comprises, chacune dans l'ordre
 * d'affichage voulu. Bornes : celles des colonnes ({@code V001} à {@code V003}) ; textes longs de 10 000 caractères
 * ({@code aboutMarkdown}, descriptions) ; adresses {@code http(s)}.
 */
public record SaveProfileRequest(
    @NotBlank @Size(max = 120) String displayName,
    @NotBlank @Size(max = 160) String professionalTitle,
    @NotBlank @Size(max = 500) String shortBio,
    @Size(max = LONG_TEXT) String aboutMarkdown,
    @Size(max = 120) String publicLocation,
    @Email @Size(max = 255) String publicEmail,
    Long avatarMediaId,
    Long cvMediaId,
    List<@NotNull @Valid Link> links,
    List<@NotNull @Valid Skill> skills,
    List<@NotNull @Valid Experience> experiences,
    List<@NotNull @Valid Education> educations,
    List<@NotNull @Valid Certification> certifications
) {

    static final int LONG_TEXT = 10_000;

    public ProfileDraft toDraft() {
        return new ProfileDraft(displayName, professionalTitle, shortBio, aboutMarkdown, publicLocation, publicEmail,
            avatarMediaId, cvMediaId,
            map(links, link -> new ProfileDraft.Link(link.label(), link.url())),
            map(skills, skill -> new ProfileDraft.Skill(skill.name(), skill.category())),
            map(experiences, entry -> new ProfileDraft.Experience(entry.organization(), entry.title(),
                entry.location(), entry.startDate(), entry.endDate(), entry.description())),
            map(educations, entry -> new ProfileDraft.Education(entry.institution(), entry.degree(), entry.field(),
                entry.location(), entry.startDate(), entry.endDate(), entry.description())),
            map(certifications, entry -> new ProfileDraft.Certification(entry.name(), entry.issuer(),
                entry.issuedAt(), entry.expiresAt(), entry.credentialUrl())));
    }

    private static <T, R> List<R> map(List<T> entries, Function<T, R> mapper) {
        return entries == null ? null : entries.stream().map(mapper).toList();
    }

    public record Link(
        @NotBlank @Size(max = 80) String label,
        @NotBlank @Size(max = 2048) @Pattern(regexp = InputPatterns.HTTP_URL, message = InputPatterns.HTTP_URL_MESSAGE)
        String url
    ) {
    }

    public record Skill(@NotBlank @Size(max = 80) String name, @NotBlank @Size(max = 60) String category) {
    }

    public record Experience(
        @NotBlank @Size(max = 120) String organization,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 120) String location,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull @Size(max = LONG_TEXT) String description
    ) {
    }

    public record Education(
        @NotBlank @Size(max = 160) String institution,
        @NotBlank @Size(max = 160) String degree,
        @NotBlank @Size(max = 160) String field,
        @NotBlank @Size(max = 120) String location,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull @Size(max = LONG_TEXT) String description
    ) {
    }

    public record Certification(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 120) String issuer,
        @NotNull LocalDate issuedAt,
        LocalDate expiresAt,
        @Size(max = 2048) @Pattern(regexp = InputPatterns.HTTP_URL, message = InputPatterns.HTTP_URL_MESSAGE)
        String credentialUrl
    ) {
    }
}
