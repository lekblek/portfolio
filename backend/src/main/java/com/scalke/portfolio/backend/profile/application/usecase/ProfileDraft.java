package com.scalke.portfolio.backend.profile.application.usecase;

import java.time.LocalDate;
import java.util.List;

/**
 * Saisie complète du profil par l'administrateur (D-CY), avant vérification : chaque collection dans l'ordre
 * d'affichage voulu. Espaces de début et de fin retirés des textes courts ; champs facultatifs vides → aucun ;
 * collections absentes → vides.
 */
public record ProfileDraft(
    String displayName,
    String professionalTitle,
    String shortBio,
    String aboutMarkdown,
    String publicLocation,
    String publicEmail,
    Long avatarMediaId,
    Long cvMediaId,
    List<Link> links,
    List<Skill> skills,
    List<Experience> experiences,
    List<Education> educations,
    List<Certification> certifications
) {

    public ProfileDraft {
        displayName = strip(displayName);
        professionalTitle = strip(professionalTitle);
        shortBio = strip(shortBio);
        aboutMarkdown = blankToNull(aboutMarkdown);
        publicLocation = blankToNull(publicLocation);
        publicEmail = blankToNull(publicEmail);
        links = links == null ? List.of() : List.copyOf(links);
        skills = skills == null ? List.of() : List.copyOf(skills);
        experiences = experiences == null ? List.of() : List.copyOf(experiences);
        educations = educations == null ? List.of() : List.copyOf(educations);
        certifications = certifications == null ? List.of() : List.copyOf(certifications);
    }

    public record Link(String label, String url) {

        public Link {
            label = strip(label);
            url = strip(url);
        }
    }

    public record Skill(String name, String category) {

        public Skill {
            name = strip(name);
            category = strip(category);
        }
    }

    public record Experience(String organization, String title, String location, LocalDate startDate,
                             LocalDate endDate, String description) {

        public Experience {
            organization = strip(organization);
            title = strip(title);
            location = strip(location);
        }
    }

    public record Education(String institution, String degree, String field, String location, LocalDate startDate,
                            LocalDate endDate, String description) {

        public Education {
            institution = strip(institution);
            degree = strip(degree);
            field = strip(field);
            location = strip(location);
        }
    }

    public record Certification(String name, String issuer, LocalDate issuedAt, LocalDate expiresAt,
                                String credentialUrl) {

        public Certification {
            name = strip(name);
            issuer = strip(issuer);
            credentialUrl = blankToNull(credentialUrl);
        }
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
