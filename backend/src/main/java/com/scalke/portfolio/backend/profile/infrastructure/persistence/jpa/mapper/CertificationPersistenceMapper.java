package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.profile.domain.model.Certification;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.CertificationEntity;

import java.util.List;

public final class CertificationPersistenceMapper {

    private CertificationPersistenceMapper() {
    }

    public static Certification toDomain(CertificationEntity entity) {
        return new Certification(
            entity.getId(),
            entity.getName(),
            entity.getIssuer(),
            entity.getIssuedAt(),
            entity.getExpiresAt(),
            entity.getCredentialUrl(),
            entity.getDisplayOrder()
        );
    }

    public static List<Certification> map(List<CertificationEntity> entities) {
        return entities.stream().map(CertificationPersistenceMapper::toDomain).toList();
    }

    public static CertificationEntity toEntity(Certification certification) {
        return CertificationEntity.builder()
            .name(certification.name())
            .issuer(certification.issuer())
            .issuedAt(certification.issuedAt())
            .expiresAt(certification.expiresAt())
            .credentialUrl(certification.credentialUrl())
            .displayOrder(certification.displayOrder())
            .build();
    }
}
