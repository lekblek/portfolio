package com.scalke.portfolio.backend.profile.infrastructure.seed;

import com.scalke.portfolio.backend.profile.application.usecase.ProfileDraft;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D-EU : un profil existant n'est que complété par le jeu de démonstration, jamais écrasé.
 */
class ProfileSeederTest {

    private static final ProfileDraft DEMO = new ProfileDraft("Démo", "Titre démo", "Bio démo", "## Démo",
        "Tanger", null, 10L, 11L,
        List.of(new ProfileDraft.Link("GitHub", "https://example.test/github/demo")),
        List.of(new ProfileDraft.Skill("Java", "Backend"), new ProfileDraft.Skill("Python", "IA et données")),
        List.of(new ProfileDraft.Experience("Atlas", "Ingénieur", "Casablanca", LocalDate.of(2024, 3, 1), null, "")),
        List.of(new ProfileDraft.Education("Institut", "Ingénieur", "Logiciel", "Tanger",
            LocalDate.of(2018, 9, 1), LocalDate.of(2021, 7, 1), "")),
        List.of(new ProfileDraft.Certification("CKAD", "CNCF", LocalDate.of(2024, 11, 4), null, null)));

    @Test
    void fills_the_empty_parts_and_the_old_minimal_demo_lists() {
        ProfileDraft existing = new ProfileDraft("Mon nom", "Mon titre", "Ma bio", null, null, null, null, null,
            List.of(new ProfileDraft.Link("GitHub", "https://example.test/gh"),
                new ProfileDraft.Link("LinkedIn", "https://example.test/in")),
            List.of(new ProfileDraft.Skill("Angular", "Frontend"), new ProfileDraft.Skill("Spring Boot", "Backend")),
            List.of(), List.of(), List.of());

        ProfileDraft completed = ProfileSeeder.complete(existing, DEMO);

        assertThat(completed.displayName()).isEqualTo("Mon nom");
        assertThat(completed.shortBio()).isEqualTo("Ma bio");
        assertThat(completed.aboutMarkdown()).isEqualTo("## Démo");
        assertThat(completed.avatarMediaId()).isEqualTo(10L);
        assertThat(completed.links()).isEqualTo(DEMO.links());
        assertThat(completed.skills()).isEqualTo(DEMO.skills());
        assertThat(completed.experiences()).isEqualTo(DEMO.experiences());
        assertThat(completed.certifications()).isEqualTo(DEMO.certifications());
    }

    @Test
    void keeps_everything_entered_by_hand() {
        ProfileDraft existing = new ProfileDraft("Mon nom", "Mon titre", "Ma bio", "Ma présentation", "Paris",
            "moi@example.test", 3L, 4L,
            List.of(new ProfileDraft.Link("Site", "https://example.test/moi")),
            List.of(new ProfileDraft.Skill("Go", "Backend")),
            List.of(new ProfileDraft.Experience("Ma société", "Dév", "Paris", LocalDate.of(2020, 1, 1), null, "")),
            List.of(new ProfileDraft.Education("Mon école", "Master", "Info", "Paris",
                LocalDate.of(2015, 9, 1), LocalDate.of(2020, 6, 30), "")),
            List.of(new ProfileDraft.Certification("Ma certification", "Moi", LocalDate.of(2022, 1, 1), null, null)));

        assertThat(ProfileSeeder.complete(existing, DEMO)).isEqualTo(existing);
    }
}
