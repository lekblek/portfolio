package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "skill"
)
public class SkillEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "category", nullable = false, length = 60)
    private String category;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    SkillEntity(ProfileEntity profile, String name, String category, int displayOrder) {
        this.profile = profile;
        this.name = name;
        this.category = category;
        this.displayOrder = displayOrder;
    }
}
