package za.co.neroland.neroruins.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroruins.lore.LogCatalog;

/**
 * The "Expedition Notes" NeroQuests chapter is inert data, so nothing at runtime checks it against this
 * mod. These tests do: every quest is listed once, prerequisites resolve without cycles, and every gate,
 * channel, structure tag and entity tag a quest waits on is one NeroRuins actually emits or ships.
 */
class QuestPackTest {

    private static final String ROOT = "/data/neroruins/";

    /** The record gates NeroRuins code opens today (temple_cleared has no caller, so it is excluded). */
    private static Set<String> emittedGates() {
        Set<String> gates = new HashSet<>(Set.of(RuinMilestones.FIRST_LOG.toString(),
                RuinMilestones.FIRST_VAULT.toString(), RuinMilestones.PUZZLE_SOLVER.toString()));
        for (String chapter : LogCatalog.chapters()) gates.add(RuinMilestones.codexGate(chapter).toString());
        return gates;
    }

    private static final Set<String> EMITTED_CHANNELS = Set.of(RuinMilestones.VAULT_CLAIMED.toString());

    private static JsonObject json(String path) throws IOException {
        try (var input = QuestPackTest.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static Map<String, JsonObject> quests() throws IOException, URISyntaxException {
        Path dir = Path.of(QuestPackTest.class.getResource(ROOT + "neroquests/quests/expedition_notes").toURI());
        Map<String, JsonObject> quests = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.sorted().toList()) {
                String name = file.getFileName().toString();
                assertTrue(name.endsWith(".json"), name);
                quests.put("neroruins:expedition_notes/" + name.substring(0, name.length() - 5),
                        json(ROOT + "neroquests/quests/expedition_notes/" + name));
            }
        }
        return quests;
    }

    private static List<String> prerequisites(JsonObject quest) {
        return quest.has("prerequisites")
                ? quest.getAsJsonArray("prerequisites").asList().stream().map(JsonElement::getAsString).toList()
                : List.of();
    }

    /** True when every node can be peeled in dependency order (Kahn); false on a cycle. */
    static boolean acyclic(Map<String, List<String>> edges) {
        Map<String, Integer> pending = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        edges.forEach((node, requires) -> {
            pending.put(node, requires.size());
            for (String required : requires) dependents.computeIfAbsent(required, key -> new java.util.ArrayList<>()).add(node);
        });
        ArrayDeque<String> ready = new ArrayDeque<>();
        pending.forEach((node, count) -> { if (count == 0) ready.add(node); });
        int peeled = 0;
        while (!ready.isEmpty()) {
            String node = ready.poll();
            peeled++;
            for (String dependent : dependents.getOrDefault(node, List.of())) {
                if (pending.merge(dependent, -1, Integer::sum) == 0) ready.add(dependent);
            }
        }
        return peeled == edges.size();
    }

    /** The gates in {@code used} that NeroRuins never opens. */
    static Set<String> unemitted(Set<String> used) {
        Set<String> missing = new HashSet<>(used);
        missing.removeAll(emittedGates());
        return missing;
    }

    @Test
    void theChapterListsEveryQuestExactlyOnce() throws Exception {
        var quests = quests();
        var listed = json(ROOT + "neroquests/chapters/expedition_notes.json").getAsJsonArray("quests").asList()
                .stream().map(entry -> entry.getAsJsonObject().get("quest").getAsString()).toList();
        assertEquals(listed.size(), Set.copyOf(listed).size(), "a quest is listed twice");
        assertEquals(quests.keySet(), Set.copyOf(listed));
        var positions = json(ROOT + "neroquests/chapters/expedition_notes.json").getAsJsonArray("quests").asList()
                .stream().map(entry -> entry.getAsJsonObject().get("x") + "," + entry.getAsJsonObject().get("y")).toList();
        assertEquals(positions.size(), Set.copyOf(positions).size(), "two quests share a book position");
    }

    @Test
    void prerequisitesExistAndFormNoCycle() throws Exception {
        var quests = quests();
        Map<String, List<String>> edges = new HashMap<>();
        quests.forEach((id, quest) -> {
            for (String required : prerequisites(quest)) assertTrue(quests.containsKey(required), id + " -> " + required);
            edges.put(id, prerequisites(quest));
        });
        assertTrue(acyclic(edges));
        assertFalse(acyclic(Map.of("a", List.of("b"), "b", List.of("a"))), "a cycle must be detected");
    }

    @Test
    void everyTriggerIsOneNeroRuinsEmitsOrShips() throws Exception {
        Set<String> gates = new HashSet<>();
        for (var entry : quests().entrySet()) {
            for (var element : entry.getValue().getAsJsonArray("objectives")) {
                JsonObject objective = element.getAsJsonObject();
                switch (objective.get("type").getAsString()) {
                    case "neroquests:gate_open" -> gates.add(objective.get("gate").getAsString());
                    case "neroquests:custom_event" -> {
                        assertTrue(EMITTED_CHANNELS.contains(objective.get("channel").getAsString()), entry.getKey());
                        assertEquals("server", entry.getValue().get("scope").getAsString(),
                                entry.getKey() + ": a world-audience event needs a server-scope quest");
                        if (objective.has("event_scope")) {
                            String structure = objective.get("event_scope").getAsString();
                            json(ROOT + "worldgen/structure/" + structure.substring("neroruins:".length()) + ".json");
                        }
                    }
                    case "neroquests:visit_structure" -> {
                        String tag = objective.get("tag").getAsString();
                        var values = json(ROOT + "tags/worldgen/structure/" + tag.substring("neroruins:".length()) + ".json")
                                .getAsJsonArray("values");
                        for (var value : values) {
                            json(ROOT + "worldgen/structure/" + value.getAsString().substring("neroruins:".length()) + ".json");
                        }
                    }
                    case "neroquests:kill_entity" -> json(ROOT + "tags/entity_type/"
                            + objective.get("tag").getAsString().substring("neroruins:".length()) + ".json");
                    case "neroquests:collect_item" -> json(ROOT + "tags/item/"
                            + objective.get("tag").getAsString().substring("neroruins:".length()) + ".json");
                    default -> throw new AssertionError(entry.getKey() + ": unchecked objective type " + objective.get("type"));
                }
            }
        }
        assertEquals(Set.of(), unemitted(gates), "a quest waits on a gate nothing opens");
        for (String gate : gates) json(ROOT + "neroland_gates/" + gate.substring("neroruins:".length()) + ".json");
        assertEquals(Set.of("neroruins:temple_cleared"), unemitted(Set.of("neroruins:temple_cleared", "neroruins:first_log")),
                "a gate with no caller must be reported");
    }

    @Test
    void everyQuestTextHasAnEnglishString() throws Exception {
        JsonObject lang = json("/assets/neroruins/lang/en_us.json");
        for (var quest : quests().values()) {
            assertTrue(lang.has(quest.get("title").getAsString()), quest.get("title").getAsString());
            assertTrue(lang.has(quest.get("description").getAsString()), quest.get("description").getAsString());
        }
    }

    @Test
    void codexGatesExistOnlyForRealChapters() {
        assertEquals("neroruins:codex/lattice", RuinMilestones.codexGate("lattice").toString());
        assertThrows(IllegalArgumentException.class, () -> RuinMilestones.codexGate("atlantis"));
    }
}
