package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Expérience du parcours. Créée par son builder, puis rattachée au profil par
 * {@link ProfileEntity#addExperience(ExperienceEntity)}.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "experience")
public class ExperienceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "organization", nullable = false, length = 120)
    private String organization;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "location", nullable = false, length = 120)
    private String location;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Builder
    private ExperienceEntity(String organization, String title, String location,
                             LocalDate startDate, LocalDate endDate, String description, int displayOrder) {
        this.organization = organization;
        this.title = title;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    void attachTo(ProfileEntity profile) {
        this.profile = profile;
    }
}
