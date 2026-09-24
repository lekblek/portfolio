package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Formation du parcours. Créée par son builder, puis rattachée au profil par
 * {@link ProfileEntity#addEducation(EducationEntity)}.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "education")
public class EducationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "institution", nullable = false, length = 160)
    private String institution;

    @Column(name = "degree", nullable = false, length = 160)
    private String degree;

    @Column(name = "field", nullable = false, length = 160)
    private String field;

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
    private EducationEntity(String institution, String degree, String field, String location,
                            LocalDate startDate, LocalDate endDate, String description, int displayOrder) {
        this.institution = institution;
        this.degree = degree;
        this.field = field;
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
