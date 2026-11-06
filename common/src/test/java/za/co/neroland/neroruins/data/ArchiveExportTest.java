package za.co.neroland.neroruins.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchiveExportTest {
    @TempDir Path world;

    @Test
    void writesOnlyTheRequestedArchiveAndErasesItsExport() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        var location = new ClaimLedger.Location("minecraft:overworld", 1, 70, 3);
        Path firstPath = world.resolve(ArchiveExport.write(world, first, Map.of("neroruins:earth.1", 123L), Map.of(location, 124L)));
        Path secondPath = world.resolve(ArchiveExport.write(world, second, Map.of("neroruins:luna.2", 456L), Map.of()));
        var json = JsonParser.parseString(Files.readString(firstPath)).getAsJsonObject();
        assertEquals(first.toString(), json.get("player").getAsString());
        assertEquals(123L, json.getAsJsonObject("archive").get("neroruins:earth.1").getAsLong());
        assertEquals(124L, json.getAsJsonArray("vault_claims").get(0).getAsJsonObject().get("claimed_at").getAsLong());
        assertFalse(Files.readString(firstPath).contains("neroruins:luna.2"));
        ArchiveExport.write(world, first, Map.of(), Map.of());
        assertEquals(0, JsonParser.parseString(Files.readString(firstPath)).getAsJsonObject()
                .getAsJsonObject("archive").size());
        ArchiveExport.erase(world, first);
        assertFalse(Files.exists(firstPath));
        assertTrue(Files.exists(secondPath));
        ArchiveExport.erase(world, first);
    }

    @Test
    void reportsWhenExportDirectoryIsNotADirectory() throws Exception {
        Files.writeString(world.resolve("neroruins-exports"), "occupied");
        assertThrows(IOException.class, () -> ArchiveExport.write(world, UUID.randomUUID(), Map.of(), Map.of()));
        assertThrows(IOException.class, () -> ArchiveExport.erase(world, UUID.randomUUID()));
    }
}
