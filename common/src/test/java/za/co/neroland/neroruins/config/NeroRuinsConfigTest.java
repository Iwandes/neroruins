package za.co.neroland.neroruins.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NeroRuinsConfigTest {
    @Test
    void privacyAndGameplayDefaultsAreServerAuthoritative() {
        assertEquals(365, NeroRuinsConfig.EXPEDITION_RETENTION_DAYS.defaultValue());
        assertEquals(0, NeroRuinsConfig.CLAIM_RETENTION_DAYS.defaultValue());
        assertEquals("until_erasure", NeroRuinsConfig.ARCHIVE_RETENTION.defaultValue());
        assertEquals("per_player", NeroRuinsConfig.VAULT_MODE.defaultValue());
        assertFalse(NeroRuinsConfig.PUSH_NOTIFICATIONS.defaultValue());
        NeroRuinsConfig.SCHEMA.values().stream()
                .filter(value -> !value.key().equals("telemetryEnabled"))
                .forEach(value -> assertTrue(value.serverAuthoritative(), value.key()));
        assertFalse(NeroRuinsConfig.SCHEMA.value("telemetryEnabled").serverAuthoritative());
    }
}
