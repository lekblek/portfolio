package com.scalke.portfolio.backend.profile;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contraintes de {@code V014__add_media_to_profile.sql}, vérifiées sans JPA : invariant 13 (D-BX).
 */
@Transactional
class ProfileMediaSchemaIT extends AbstractIntegrationTest {

    @Autowired
    JdbcClient jdbcClient;

    @ParameterizedTest
    @CsvSource({"avatar_media_id, profile_avatar_media_fk", "cv_media_id, profile_cv_media_fk"})
    void refuses_to_delete_a_media_used_by_the_profile(String column, String constraint) {
        long media = jdbcClient.sql("""
                    INSERT INTO media (storage_key, original_name, size_bytes, created_at)
                    VALUES ('3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.pdf', 'cv.pdf', 10, now())
                    RETURNING id
                    """)
            .query(Long.class)
            .single();
        jdbcClient.sql("INSERT INTO profile (id, display_name, professional_title, short_bio, " + column + ") "
                + "VALUES (1, 'Nom', 'Titre', 'Présentation.', :media)")
            .param("media", media)
            .update();

        assertThatThrownBy(() -> jdbcClient.sql("DELETE FROM media WHERE id = :media").param("media", media).update())
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining(constraint);
    }
}
