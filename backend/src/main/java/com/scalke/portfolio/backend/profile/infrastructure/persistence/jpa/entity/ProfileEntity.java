package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Length;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "profile"
)
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

    @Column(name = "public_location", length = 120)
    private String publicLocation;

    @Column(name = "public_email", length = 255)
    private String publicEmail;

    @Column(name = "about_markdown", length = Length.LONG32, columnDefinition = "TEXT")
    private String aboutMarkdown;

    @OneToMany(
        mappedBy = "profile",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<ProfessionalLinkEntity> links = new ArrayList<>();

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
}
