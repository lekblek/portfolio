package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface SpringDataProfileRepository extends Repository<ProfileEntity,Long> {

    @Query("""
            select p
            from ProfileEntity p
            left join fetch p.links
            """)
    Optional<ProfileEntity> findWithLinks();
}
