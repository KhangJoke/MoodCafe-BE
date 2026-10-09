package com.moodcafe;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Utility test for manual local DB reset; do not run during automated builds")
class FlywayResetTest {

    @Test
    void testCleanAndMigrateDatabase() {
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:postgresql://localhost:5432/mood_cafe", "mood_cafe", "12345")
                .locations("classpath:db/migration", "classpath:db/seed")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        MigrateResult result = flyway.migrate();

        System.out.println("Flyway migration successful: " + result.migrationsExecuted + " migrations executed.");
        assertTrue(result.success);
    }
}
