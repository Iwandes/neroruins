package za.co.neroland.neroruins.loot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

/**
 * Every required {@code minecraft:} item id in NeroRuins' item tags and loot tables must exist in the
 * Minecraft version this cell is built for. Vanilla renames items between versions, and one unknown required
 * id makes the whole tag fail, which takes every loot table using it down with it and aborts world creation.
 * Reads the resources as packaged for this cell, so the 26.3 overlay is checked on 26.3. An item exists when
 * this cell's Minecraft ships its item definition ({@code assets/minecraft/items}, 1.21.4+) or item model
 * ({@code assets/minecraft/models/item}, every line); both are on the test classpath, and no bootstrap is needed.
 */
class VanillaIdsTest {
    @Test
    void requiredVanillaItemsExistInThisVersion() throws Exception {
        Set<String> missing = new TreeSet<>();
        for (Path file : packagedJson("tags/item", "loot_table")) {
            for (String id : requiredVanillaIds(JsonParser.parseString(Files.readString(file)))) {
                if (!exists(id)) missing.add(file.getFileName() + ": " + id);
            }
        }
        assertTrue(missing.isEmpty(), "Unknown required vanilla items: " + missing);
    }

    @Test
    void theCheckRejectsARenamedItem() {
        var fixture = JsonParser.parseString("{\"values\":[\"minecraft:definitely_not_an_item\","
                + "{\"id\":\"minecraft:also_missing\",\"required\":false}]}");
        var ids = requiredVanillaIds(fixture);
        assertTrue(ids.contains("minecraft:definitely_not_an_item"));
        assertFalse(ids.contains("minecraft:also_missing"), "optional entries are allowed to be absent");
        assertFalse(exists("minecraft:definitely_not_an_item"));
        assertTrue(exists("minecraft:iron_bars"));
        // The regression this test exists for: 1.21.9 renamed minecraft:chain, which broke world creation.
        //? if >=1.21.9 {
        assertFalse(exists("minecraft:chain"));
        //?} else {
        /*assertTrue(exists("minecraft:chain"));
        *///?}
    }

    private static boolean exists(String id) {
        String path = id.substring("minecraft:".length());
        return VanillaIdsTest.class.getResource("/assets/minecraft/items/" + path + ".json") != null
                || VanillaIdsTest.class.getResource("/assets/minecraft/models/item/" + path + ".json") != null;
    }

    static Set<String> requiredVanillaIds(JsonElement element) {
        Set<String> ids = new TreeSet<>();
        collect(element, true, ids);
        return ids;
    }

    private static void collect(JsonElement element, boolean required, Set<String> ids) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) add(child.getAsString(), required, ids);
                else collect(child, required, ids);
            }
        } else if (element.isJsonObject()) {
            var object = element.getAsJsonObject();
            boolean optional = object.has("required") && !object.get("required").getAsBoolean();
            boolean itemEntry = !object.has("type") || "minecraft:item".equals(object.get("type").getAsString());
            for (var entry : object.entrySet()) {
                var value = entry.getValue();
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                        && (entry.getKey().equals("id") || (entry.getKey().equals("name") && itemEntry))) {
                    add(value.getAsString(), required && !optional, ids);
                } else {
                    collect(value, required && !optional, ids);
                }
            }
        }
    }

    private static void add(String value, boolean required, Set<String> ids) {
        if (required && value.startsWith("minecraft:")) ids.add(value);
    }

    private static List<Path> packagedJson(String... folders) throws Exception {
        var url = VanillaIdsTest.class.getResource("/data/neroruins");
        Path root = Path.of(url.toURI()).getParent();
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(path -> path.toString().endsWith(".json")).filter(path -> {
                String relative = root.relativize(path).toString().replace('\\', '/');
                for (String folder : folders) {
                    if (relative.matches("[a-z0-9_.-]+/" + folder + "/.*")) return true;
                }
                return false;
            }).toList();
        }
    }
}
