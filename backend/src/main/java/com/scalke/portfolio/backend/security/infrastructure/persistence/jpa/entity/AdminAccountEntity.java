package com.scalke.portfolio.backend.security.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Structure de persistance du compte administrateur ({@code V021}) : une seule ligne, d'identifiant
 * {@link #SINGLETON_ID} (invariant 30), comme le profil (D-A).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "admin_account")
public class AdminAccountEntity {

    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "login", nullable = false, length = 100)
    private String login;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    private AdminAccountEntity(String login, String passwordHash, boolean enabled, Instant lastLoginAt,
                               Instant createdAt, Instant updatedAt) {
        this.id = SINGLETON_ID;
        this.login = login;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void changeCredentials(String login, String passwordHash, Instant updatedAt) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.updatedAt = updatedAt;
    }
}
