package br.com.sisaws.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PostgresSchemaCompatibilityMigrationTest {

    @Test
    void shouldUpdateLegacyRoleConstraint() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("CHECK (((role)::text = 'STUDENT'::text))"));

        PostgresSchemaCompatibilityMigration migration =
                new PostgresSchemaCompatibilityMigration(jdbcTemplate);

        migration.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate, times(2)).execute(anyString());
    }

    @Test
    void shouldNotTouchConstraintWhenInstructorIsAlreadyAllowed() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("CHECK (((role)::text = ANY ((ARRAY['STUDENT'::character varying, 'INSTRUCTOR'::character varying])::text[])))"));

        PostgresSchemaCompatibilityMigration migration =
                new PostgresSchemaCompatibilityMigration(jdbcTemplate);

        migration.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate, never()).execute(anyString());
    }
}
