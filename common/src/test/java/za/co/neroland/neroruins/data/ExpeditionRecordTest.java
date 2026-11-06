package za.co.neroland.neroruins.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExpeditionRecordTest {
    private static ExpeditionRecord.Discovery at(int x, long time) {
        return new ExpeditionRecord.Discovery("neroruins:bunker_earth", "bunker", "outpost", "minecraft:overworld",
                Optional.of(new ExpeditionRecord.Hub(x, 60, 7)), time);
    }

    private static ExpeditionRecord.Discovery unpositioned(String structure, long time) {
        return new ExpeditionRecord.Discovery(structure, "bunker", "outpost", "minecraft:overworld", Optional.empty(), time);
    }

    @Test
    void recordsEachHubOnceAndOnlyForTheVisitor() {
        var visitor = UUID.randomUUID();
        var other = UUID.randomUUID();
        var record = new ExpeditionRecord();
        assertTrue(record.discover(visitor, at(100, 1)));
        assertFalse(record.discover(visitor, at(100, 2)), "a second visit is not a discovery");
        assertTrue(record.discover(visitor, at(900, 3)), "a different hub of the same type is");
        assertEquals(2, record.export(visitor).size());
        assertTrue(record.export(other).isEmpty(), "nothing leaks to another player");
        assertThrows(UnsupportedOperationException.class, () -> record.export(visitor).clear());

        var saved = ExpeditionRecord.codec().encodeStart(NbtOps.INSTANCE, record).getOrThrow();
        var loaded = ExpeditionRecord.codec().parse(NbtOps.INSTANCE, saved).getOrThrow();
        assertEquals(record.export(visitor), loaded.export(visitor));
        loaded.erase(visitor);
        assertTrue(loaded.export(visitor).isEmpty());
    }

    @Test
    void minimisedDiscoveriesAreOncePerTypeAndWorld() {
        var player = UUID.randomUUID();
        var record = new ExpeditionRecord();
        assertTrue(record.discover(player, unpositioned("neroruins:bunker_earth", 1)));
        assertFalse(record.discover(player, unpositioned("neroruins:bunker_earth", 2)));
        assertTrue(record.discover(player, at(5, 3)), "with positions back on, a hub is recorded by its position");
        assertFalse(record.discover(player, unpositioned("neroruins:bunker_earth", 4)));
        assertTrue(record.discover(player, unpositioned("neroruins:bunker", 5)));
    }

    @Test
    void retentionDropsOnlyOldPositionsAndKeepsTheDiscovery() {
        var player = UUID.randomUUID();
        var record = new ExpeditionRecord();
        record.discover(player, at(1, 10));
        record.discover(player, at(2, 86_400_000L));
        assertEquals(0, record.dropExpiredPositions(86_400_020L, 0), "zero keeps positions until erasure");
        assertEquals(1, record.dropExpiredPositions(86_400_020L, 1));
        var list = record.export(player);
        assertEquals(2, list.size());
        assertTrue(list.get(0).hub().isEmpty());
        assertTrue(list.get(1).hub().isPresent());
        assertEquals(1, record.dropAllPositions());
        assertTrue(record.export(player).stream().allMatch(discovery -> discovery.hub().isEmpty()));
        assertThrows(IllegalArgumentException.class, () -> record.dropExpiredPositions(1, -1));
    }

    @Test
    void perPlayerCapAndCorruptDataAreRejected() {
        var player = UUID.randomUUID();
        var record = new ExpeditionRecord();
        for (int i = 0; i < ExpeditionRecord.MAX_PER_PLAYER; i++) assertTrue(record.discover(player, at(i, 1)));
        assertFalse(record.discover(player, at(-1, 1)));
        assertThrows(IllegalArgumentException.class, () -> new ExpeditionRecord.Discovery("bad", "bunker", "outpost",
                "minecraft:overworld", Optional.empty(), 1));
        assertThrows(IllegalArgumentException.class, () -> new ExpeditionRecord.Discovery("neroruins:bunker", "bunker",
                "huge", "minecraft:overworld", Optional.empty(), 1));
        var corrupt = JsonParser.parseString("{\"entries\":[{\"player\":\"" + player + "\",\"structure\":\"neroruins:bunker\","
                + "\"family\":\"bunker\",\"size_class\":\"outpost\",\"dimension\":\"minecraft:overworld\",\"hub\":[1,2],"
                + "\"first_seen\":1}]}");
        assertTrue(ExpeditionRecord.codec().parse(JsonOps.INSTANCE, corrupt).error().isPresent());
    }

    @Test
    void exportIncludesOnlyTheRequestersDiscoveries(@TempDir Path world) throws Exception {
        var player = UUID.randomUUID();
        var record = new ExpeditionRecord();
        record.discover(player, at(42, 5));
        record.discover(player, unpositioned("neroruins:bunker", 6));
        record.discover(UUID.randomUUID(), at(77, 7));
        var path = world.resolve(ArchiveExport.write(world, player, Map.of(), Map.of(), Map.of(), record.export(player)));
        var expeditions = JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonArray("expeditions");
        assertEquals(2, expeditions.size());
        assertEquals(42, expeditions.get(0).getAsJsonObject().get("x").getAsInt());
        assertFalse(expeditions.get(1).getAsJsonObject().has("x"), "minimised discoveries carry no position");
        assertEquals(List.of(), new ExpeditionRecord().export(player));
    }
}
