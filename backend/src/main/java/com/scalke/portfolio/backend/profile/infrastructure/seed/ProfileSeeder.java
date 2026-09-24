package com.scalke.portfolio.backend.profile.infrastructure.seed;

import com.scalke.portfolio.backend.profile.domain.model.Certification;
import com.scalke.portfolio.backend.profile.domain.model.Education;
import com.scalke.portfolio.backend.profile.domain.model.Experience;
import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import com.scalke.portfolio.backend.profile.domain.port.ProfileRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Données de démonstration du profil `dev`, créées uniquement si la base ne contient aucun profil.
 * Ce ne sont pas des données réelles (voir docs/01-perimetre-v1.md §20).
 */
@Component
@org.springframework.context.annotation.Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class ProfileSeeder implements ApplicationRunner {

    private final ProfileRepository profileRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (profileRepository.find().isPresent()) {
            return;
        }
        profileRepository.save(demoProfile());
        log.info("Profil de démonstration créé (profil dev)");
    }

    private static Profile demoProfile() {
        return new Profile(
            "Blek Gedeon Ngossanga",
            "Développeur full-stack",
            "Je conçois des solutions modernes et évolutives.",
            null,
            null,
            null,
            List.of(
                new ProfessionalLink(null, "GitHub", "https://example.test/gh", 0),
                new ProfessionalLink(null, "LinkedIn", "https://example.test/in", 1)),
            List.of(
                new Skill(null, "Angular", "Frontend", 0),
                new Skill(null, "Spring Boot", "Backend", 1)),
            List.of(
                new Experience(null, "Organisation de démonstration", "Développeur full-stack", "Tanger",
                    DateRange.ongoingSince(LocalDate.of(2024, 1, 1)), "Expérience de démonstration.", 0)),
            List.of(
                new Education(null, "École de démonstration", "Diplôme d'ingénieur", "Informatique", "Tanger",
                    DateRange.between(LocalDate.of(2018, 9, 1), LocalDate.of(2023, 6, 30)),
                    "Formation de démonstration.", 0)),
            List.of(
                new Certification(null, "Certification de démonstration", "Émetteur",
                    LocalDate.of(2025, 1, 1), null, null, 0)));
    }
}
