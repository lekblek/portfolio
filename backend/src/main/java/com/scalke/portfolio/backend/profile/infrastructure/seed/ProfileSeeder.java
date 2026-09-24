package com.scalke.portfolio.backend.profile.infrastructure.seed;

import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import com.scalke.portfolio.backend.profile.domain.port.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@org.springframework.context.annotation.Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class ProfileSeeder implements ApplicationRunner {

    private final ProfileRepository profileRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        seedProfile();
    }

    private void seedProfile() {

        if (profileRepository.find().isEmpty()) {
            List<ProfessionalLink> links = new ArrayList<>();
            links.add(new ProfessionalLink(null, "LinkedIn", "https://example.test/in", 1));
            links.add(new ProfessionalLink(null, "GitHub", "https://example.test/gh", 0));


            List<Skill> skills = new ArrayList<>();
            skills.add(new Skill(null, "Spring Boot", "Backend", 1));
            skills.add(new Skill(null, "Angular", "Frontend", 0));


            Profile profile = Profile.create(
                "Blek Gedeon Ngossanga",
                "Développeur full-stack",
                "Je conçois des solutions modernes et évolutives.",
                null,
                links,
                skills
            );
            profileRepository.save(profile);
        }
    }
}
