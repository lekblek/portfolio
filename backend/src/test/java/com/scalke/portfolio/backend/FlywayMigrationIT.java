package com.scalke.portfolio.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywayMigrationIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void applies_every_migration_successfully_on_a_clean_database() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList("""
                select version, description, success
                from flyway_schema_history
                order by installed_rank
                """);

        assertThat(history).isNotEmpty();
        assertThat(history)
            .allSatisfy(row -> assertThat(row.get("success")).isEqualTo(true));
    }

    @Test
    void runs_against_postgresql_18_or_later() {
        Integer versionNum = jdbcTemplate.queryForObject(
            "select current_setting('server_version_num')::int", Integer.class);

        assertThat(versionNum).isNotNull().isGreaterThanOrEqualTo(180_000);
    }
}
