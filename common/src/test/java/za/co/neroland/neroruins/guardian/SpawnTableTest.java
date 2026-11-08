package za.co.neroland.neroruins.guardian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

class SpawnTableTest {
    private static final Set<String> SENTINELS = Set.of(
            "neroruins:sentinel_mk1", "neroruins:sentinel_mk2", "neroruins:sentinel_mk3");

    private static SpawnTable bunker() throws Exception {
        var input = SpawnTableTest.class.getResourceAsStream("/data/neroruins/spawn_tables/bunker.json");
        assertNotNull(input);
        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return SpawnTable.parse(reader).getOrThrow();
        }
    }

    @Test
    void bunkerTableListsSentinelsSiblingsAndVanillaFallbacks() throws Exception {
        SpawnTable table = bunker();
        for (var entry : table.entries()) {
            if (entry.required()) {
                assertTrue(entry.entity().startsWith("minecraft:") || SENTINELS.contains(entry.entity()),
                        entry.entity() + " is required but is not a NeroRuins or vanilla mob");
            } else {
                assertTrue(entry.entity().startsWith("nerocreatures:"), "only sibling mobs are optional");
            }
        }
        for (int tier = 1; tier <= SpawnTable.MAX_TIER; tier++) {
            int t = tier;
            assertTrue(table.entries().stream().anyMatch(e -> e.covers(t) && SENTINELS.contains(e.entity())),
                    "every tier has a Sentinel");
        }
    }

    @Test
    void absentSiblingMobsAreSkippedAndPresentOnesJoin() throws Exception {
        SpawnTable table = bunker();
        Predicate<String> coreOnly = id -> id.startsWith("minecraft:") || id.startsWith("neroruins:");
        long optional = table.entries().stream().filter(e -> !e.required()).count();
        assertEquals(4, optional);
        for (int tier = 1; tier <= SpawnTable.MAX_TIER; tier++) {
            assertTrue(table.available(tier, coreOnly).stream().allMatch(e -> coreOnly.test(e.entity())));
            assertFalse(table.available(tier, coreOnly).isEmpty());
        }
        assertTrue(table.available(3, id -> true).stream().anyMatch(e -> e.entity().equals("nerocreatures:rogue_android")));
        assertFalse(table.available(1, id -> true).stream().anyMatch(e -> e.entity().equals("nerocreatures:rogue_android")),
                "tier ranges are honoured");
    }

    @Test
    void weightedPickWalksTheWeights() {
        SpawnTable table = new SpawnTable(List.of(
                new SpawnTable.Entry("neroruins:a", 3, 1, 3, true),
                new SpawnTable.Entry("neroruins:b", 1, 1, 3, true)));
        assertEquals(Optional.of("neroruins:a"), table.pick(1, id -> true, bound -> 2));
        assertEquals(Optional.of("neroruins:b"), table.pick(1, id -> true, bound -> bound - 1));
        assertEquals(Optional.empty(), table.pick(1, id -> false, bound -> 0), "nothing registered, nothing spawned");
    }

    @Test
    void invalidTablesAreRejected() {
        for (String json : new String[] {
                "{\"entries\":[]}",
                "{\"entries\":[{\"entity\":\"not an id\",\"weight\":1}]}",
                "{\"entries\":[{\"entity\":\"neroruins:x\",\"weight\":0}]}",
                "{\"entries\":[{\"entity\":\"neroruins:x\",\"weight\":1,\"min_tier\":3,\"max_tier\":1}]}",
                "{\"entries\":[{\"entity\":\"neroruins:x\",\"weight\":1,\"max_tier\":4}]}",
                "{not json",
        }) {
            assertTrue(SpawnTable.parse(new StringReader(json)).error().isPresent(), json);
        }
        assertTrue(SpawnTable.parse(new StringReader("{\"entries\":[{\"entity\":\"neroruins:x\",\"weight\":1}]}"))
                .result().isPresent());
    }
}
