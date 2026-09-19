package br.com.sisaws.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("postgres")
public class PostgresSchemaCompatibilityMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PostgresSchemaCompatibilityMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public PostgresSchemaCompatibilityMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> definitions = jdbcTemplate.queryForList("""
                SELECT pg_get_constraintdef(c.oid)
                FROM pg_constraint c
                JOIN pg_class t ON c.conrelid = t.oid
                JOIN pg_namespace n ON t.relnamespace = n.oid
                WHERE n.nspname = current_schema()
                  AND t.relname = 'app_users'
                  AND c.conname = 'app_users_role_check'
                """, String.class);

        boolean instructorAlreadyAllowed = definitions.stream()
                .anyMatch(definition -> definition != null && definition.contains("'INSTRUCTOR'"));

        if (instructorAlreadyAllowed) {
            return;
        }

        log.info("Updating app_users_role_check to support STUDENT and INSTRUCTOR roles");

        jdbcTemplate.execute("""
                ALTER TABLE app_users
                DROP CONSTRAINT IF EXISTS app_users_role_check
                """);

        jdbcTemplate.execute("""
                ALTER TABLE app_users
                ADD CONSTRAINT app_users_role_check
                CHECK (role IN ('STUDENT', 'INSTRUCTOR'))
                """);
    }
}
