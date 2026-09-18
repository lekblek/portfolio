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
        insertProfile(1);

        assertThatThrownBy(() -> insertProfile(2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletes_links_when_the_profile_is_deleted() {
        insertProfile(1);

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

    private void insertProfile(long id) {
        jdbcClient.sql("""
                        INSERT INTO profile (id, display_name, professional_title, short_bio)
                        VALUES (:id, 'Blek', 'Développeur full-stack', 'Bio courte')
                        """)
            .param("id", id)
            .update();
    }

    @Test
    void allows_recreating_the_profile_after_deletion() {
        insertProfile(1);
        jdbcClient.sql("DELETE FROM profile WHERE id = 1").update();

        insertProfile(1);

        long count = jdbcClient.sql("SELECT count(*) FROM profile")
            .query(Long.class)
            .single();

        assertThat(count).isEqualTo(1);
    }

    @Test
    void rejects_a_duplicated_skill_name() {
        insertProfile(1);
        insertSkill("Java", "Backend");

        assertThatThrownBy(() -> insertSkill("Java", "Enseignement"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletes_skills_when_the_profile_is_deleted() {
        insertProfile(1);
        insertSkill("Java", "Backend");

        jdbcClient.sql("DELETE FROM profile WHERE id = 1").update();

        long remaining = jdbcClient.sql("SELECT count(*) FROM skill")
            .query(Long.class)
            .single();

        assertThat(remaining).isZero();
    }

    private void insertSkill(String name, String category) {
        jdbcClient.sql("""
                    INSERT INTO skill (profile_id, name, category, display_order)
                    VALUES (1, :name, :category, 0)
                    """)
            .param("name", name)
            .param("category", category)
            .update();
    }
}
