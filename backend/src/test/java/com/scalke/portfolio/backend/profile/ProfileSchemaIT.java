package com.scalke.portfolio.backend.profile;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProfileSchemaIT extends AbstractIntegrationTest {
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

    @Test
    void accepts_an_ongoing_experience() {
        insertProfile(1);

        insertExperience(LocalDate.of(2024, 1, 1), null);

        assertThat(count("experience")).isEqualTo(1);
    }

    @Test
    void rejects_an_experience_ending_before_it_starts() {
        insertProfile(1);

        assertThatThrownBy(() -> insertExperience(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1)))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("experience_dates_check");
    }

    @Test
    void rejects_an_education_ending_before_it_starts() {
        insertProfile(1);

        assertThatThrownBy(() -> insertEducation(LocalDate.of(2020, 9, 1), LocalDate.of(2019, 6, 30)))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("education_dates_check");
    }

    @Test
    void rejects_a_certification_expiring_before_it_is_issued() {
        insertProfile(1);

        assertThatThrownBy(() -> insertCertification(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 1, 1)))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("certification_dates_check");
    }

    @Test
    void deletes_career_entries_when_the_profile_is_deleted() {
        insertProfile(1);
        insertExperience(LocalDate.of(2024, 1, 1), null);
        insertEducation(LocalDate.of(2018, 9, 1), LocalDate.of(2023, 6, 30));
        insertCertification(LocalDate.of(2025, 1, 1), null);

        jdbcClient.sql("DELETE FROM profile WHERE id = 1").update();

        assertThat(count("experience")).isZero();
        assertThat(count("education")).isZero();
        assertThat(count("certification")).isZero();
    }

    private void insertExperience(LocalDate start, LocalDate end) {
        jdbcClient.sql("""
                    INSERT INTO experience (profile_id, organization, title, location,
                                            start_date, end_date, description, display_order)
                    VALUES (1, 'Scalke', 'Fondateur', 'Tanger', :start, :end, 'Description', 0)
                    """)
            .param("start", start)
            .param("end", end)
            .update();
    }

    private void insertEducation(LocalDate start, LocalDate end) {
        jdbcClient.sql("""
                    INSERT INTO education (profile_id, institution, degree, field, location,
                                           start_date, end_date, description, display_order)
                    VALUES (1, 'ENSA Tanger', 'Ingénieur', 'Informatique', 'Tanger',
                            :start, :end, 'Description', 0)
                    """)
            .param("start", start)
            .param("end", end)
            .update();
    }

    private void insertCertification(LocalDate issued, LocalDate expires) {
        jdbcClient.sql("""
                    INSERT INTO certification (profile_id, name, issuer, issued_at,
                                               expires_at, display_order)
                    VALUES (1, 'Certification', 'Émetteur', :issued, :expires, 0)
                    """)
            .param("issued", issued)
            .param("expires", expires)
            .update();
    }

    private long count(String table) {
        return jdbcClient.sql("SELECT count(*) FROM " + table)
            .query(Long.class)
            .single();
    }
}
