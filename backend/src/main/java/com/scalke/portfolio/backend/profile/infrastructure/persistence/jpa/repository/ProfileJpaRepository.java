package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfileJpaRepository extends Repository<ProfileEntity,Long> {

    @Query("""
            select p
            from ProfileEntity p
            left join fetch p.links
            """)
    Optional<ProfileEntity> findWithLinks();

    @Query("""
        select p
        from ProfileEntity p
        left join fetch p.skills
        where p.id = :id
        """)
    Optional<ProfileEntity> loadSkills(@Param("id") Long id);


    ProfileEntity save(ProfileEntity entity);
}
