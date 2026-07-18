package com.example.pmp.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pmp_flyway_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("local")
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationsCreateSchemaAndSeedCategories() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true",
                Integer.class
        );
        Integer categoryCount = jdbcTemplate.queryForObject(
                "select count(*) from categories where taxonomy = 'PMP_TOPIC' and active = true",
                Integer.class
        );
        Integer historyTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'QUESTION_ANSWER_ATTEMPTS'",
                Integer.class
        );

        assertThat(migrationCount).isGreaterThanOrEqualTo(6);
        assertThat(categoryCount).isEqualTo(14);
        assertThat(historyTableCount).isEqualTo(1);
    }
}
