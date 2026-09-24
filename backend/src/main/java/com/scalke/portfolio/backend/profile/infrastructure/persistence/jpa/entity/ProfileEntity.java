package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Length;

import java.util.ArrayList;
import java.util.List;

/**
 * Racine de persistance du profil (singleton : {@code id = 1}, garanti par {@code V001}).
 * <p>
 * Les collections sont initialisées à la déclaration et ne sont modifiées que par les
 * méthodes {@code addX}, qui positionnent toujours la référence arrière vers le profil.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "profile")
public class ProfileEntity {

    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "professional_title", nullable = false, length = 160)
    private String professionalTitle;

    @Column(name = "short_bio", nullable = false, length = 500)
    private String shortBio;

    @Setter
    @Column(name = "public_location", length = 120)
    private String publicLocation;

    @Setter
    @Column(name = "public_email", length = 255)
    private String publicEmail;

    @Setter
    @Column(name = "about_markdown", length = Length.LONG32, columnDefinition = "TEXT")
    private String aboutMarkdown;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, label ASC")
    private List<ProfessionalLinkEntity> links = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, name ASC")
    private List<SkillEntity> skills = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, period.startDate DESC, id ASC")
    private List<ExperienceEntity> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, period.startDate DESC, id ASC")
    private List<EducationEntity> educations = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, issuedAt DESC, id ASC")
    private List<CertificationEntity> certifications = new ArrayList<>();

    public ProfileEntity(String displayName, String professionalTitle, String shortBio) {
        this.id = SINGLETON_ID;
        this.displayName = displayName;
        this.professionalTitle = professionalTitle;
        this.shortBio = shortBio;
    }

    public void addLink(String label, String url, int displayOrder) {
        links.add(new ProfessionalLinkEntity(this, label, url, displayOrder));
    }

    public void removeLink(ProfessionalLinkEntity link) {
        links.remove(link);
    }

    public void addSkill(String name, String category, int displayOrder) {
        skills.add(new SkillEntity(this, name, category, displayOrder));
    }

    public void removeSkill(SkillEntity skill) {
        skills.remove(skill);
    }

    public void addExperience(ExperienceEntity experience) {
        experience.attachTo(this);
        experiences.add(experience);
    }

    public void addEducation(EducationEntity education) {
        education.attachTo(this);
        educations.add(education);
    }

    public void addCertification(CertificationEntity certification) {
        certification.attachTo(this);
        certifications.add(certification);
    }
}
