package za.co.neroland.neroruins.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class PlayerArchiveTest {
    @Test
    void recordsOncePersistsAndErasesOnlyTheRequestedPlayer() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PlayerArchive archive = new PlayerArchive();
        assertTrue(archive.record(first, "neroruins:earth.1", 100));
        assertFalse(archive.record(first, "neroruins:earth.1", 200));
        assertTrue(archive.record(second, "neroruins:luna.1", 300));
        assertTrue(archive.contains(first, "neroruins:earth.1"));
        assertFalse(archive.contains(second, "neroruins:earth.1"));

        var encoded = PlayerArchive.codec().encodeStart(JsonOps.INSTANCE, archive).getOrThrow();
        PlayerArchive loaded = PlayerArchive.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(100L, loaded.export(first).get("neroruins:earth.1"));
        assertThrows(UnsupportedOperationException.class, () -> loaded.export(first).clear());
        loaded.erase(first);
        assertTrue(loaded.export(first).isEmpty());
        assertEquals(300L, loaded.export(second).get("neroruins:luna.1"));
    }

    @Test
    void rejectsInvalidInputAndCorruptSavedRows() {
        PlayerArchive archive = new PlayerArchive();
        UUID player = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> archive.record(player, "not a log id", 1));
        assertThrows(IllegalArgumentException.class, () -> archive.record(player, "neroruins:earth.1", -1));
        assertTrue(archive.export(player).isEmpty());
        var corrupted = JsonParser.parseString(
                "{\"entries\":[{\"player\":\"bad\",\"log\":\"neroruins:earth.1\",\"read_at\":1}]}");
        assertTrue(PlayerArchive.codec().parse(JsonOps.INSTANCE, corrupted).error().isPresent());
    }
}
