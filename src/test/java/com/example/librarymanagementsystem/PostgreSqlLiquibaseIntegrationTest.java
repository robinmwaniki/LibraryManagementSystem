package com.example.librarymanagementsystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the real PostgreSQL 18 schema migration and Hibernate mapping.
 * The test is skipped automatically on machines without a Docker daemon.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PostgreSqlLiquibaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("library_integration")
                    .withUsername("library_test")
                    .withPassword("library_test_password");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("library.security.admin.username", () -> "integration-admin");
        registry.add("library.security.admin.password", () -> "integration-test-password");
        registry.add("library.notifications.enabled", () -> false);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void liquibaseCreatesAllApplicationTablesAndHibernateValidatesMappings() {
        assertThat(tableExists("books")).isTrue();
        assertThat(tableExists("members")).isTrue();
        assertThat(tableExists("library_settings")).isTrue();
        assertThat(tableExists("borrowing_records")).isTrue();
        assertThat(tableExists("databasechangelog")).isTrue();

        Integer changesets = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog", Integer.class);
        assertThat(changesets).isEqualTo(4);

        Integer defaultSettings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM library_settings WHERE id = 1 AND loan_period_days = 14",
                Integer.class);
        assertThat(defaultSettings).isEqualTo(1);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count == 1;
    }
}
