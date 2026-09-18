package com.scalke.portfolio.backend.profile;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class ProfileSchemaIT extends AbstractIntegrationTest {
    @Autowired
    JdbcClient jdbcClient;

    @Test
    void rejects_a_second_profile_row() {
        insertProfile();

        assertThatThrownBy(this::insertProfile)
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletes_links_when_the_profile_is_deleted() {
        insertProfile();

        jdbcClient.sql("""
                        INSERT INTO professional_link (profile_id, label, url, display_order)
                        VALUES (1, 'GitHub', 'https://github.com/example', 0)
                        """)
            .update();

        jdbcClient.sql("DELETE FROM profile WHERE id = 1").update();

        long remaining = jdbcClient.sql("SELECT count(*) FROM professional_link")
            .query(Long.class)
            .single();

        assertThat(remaining).isZero();
    }

    private void insertProfile() {
        jdbcClient.sql("""
                        INSERT INTO profile (display_name, professional_title, short_bio)
                        VALUES ('Blek', 'Développeur full-stack', 'Bio courte')
                        """)
            .update();
    }
}
