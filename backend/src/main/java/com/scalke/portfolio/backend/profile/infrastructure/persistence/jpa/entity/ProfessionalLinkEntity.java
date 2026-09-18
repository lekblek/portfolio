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
    name = "professional_link"
)
public class ProfessionalLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "label", nullable = false, length = 80)
    private String label;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public ProfessionalLinkEntity(ProfileEntity profile, String label, String url, int displayOrder) {
        this.profile = profile;
        this.label = label;
        this.url = url;
        this.displayOrder = displayOrder;
    }
}
