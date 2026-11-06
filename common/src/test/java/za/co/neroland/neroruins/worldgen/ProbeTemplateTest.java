package za.co.neroland.neroruins.worldgen;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class ProbeTemplateTest {
    @Test
    void jigsawDepthKeepsTheStartingPiece() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(
                "/data/neroruins/worldgen/structure/probe.json")) {
            assertNotNull(input);
            var structure = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertTrue(structure.getAsJsonObject("layout").get("size").getAsInt() > 0);
        }
    }

    @Test
    void templateDecodesAsMinecraftNbt() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/data/neroruins/structure/probe.nbt")) {
            assertNotNull(input);
            CompoundTag template = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            assertTrue(template.contains("size"));
            assertTrue(template.contains("palette"));
            assertTrue(template.contains("blocks"));
            assertTrue(template.contains("DataVersion"));
            assertEquals(25, assertInstanceOf(ListTag.class, template.get("blocks")).size());
        }
    }

    @Test
    void everyWeightedVariantHasACompleteDistinctTemplate() throws IOException {
        try (InputStream poolStream = getClass().getResourceAsStream(
                "/data/neroruins/worldgen/template_pool/probe.json")) {
            assertNotNull(poolStream);
            var pool = JsonParser.parseReader(new InputStreamReader(poolStream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            var entries = pool.getAsJsonArray("elements");
            assertTrue(entries.size() >= 3);
            Set<String> palettes = new HashSet<>();
            for (var entry : entries) {
                var object = entry.getAsJsonObject();
                assertTrue(object.get("weight").getAsInt() > 0);
                String location = object.getAsJsonObject("element").get("location").getAsString();
                assertTrue(location.startsWith("neroruins:"));
                String path = "/data/neroruins/structure/" + location.substring("neroruins:".length()) + ".nbt";
                try (InputStream templateStream = getClass().getResourceAsStream(path)) {
                    assertNotNull(templateStream, path);
                    CompoundTag template = NbtIo.readCompressed(templateStream, NbtAccounter.unlimitedHeap());
                    assertEquals(25, assertInstanceOf(ListTag.class, template.get("blocks")).size());
                    assertTrue(palettes.add(template.get("palette").toString()), "duplicate palette: " + path);
                }
            }
        }
    }

    @Test
    void rejectsCorruptCompressedTemplate() {
        assertThrows(IOException.class, () -> NbtIo.readCompressed(
                new ByteArrayInputStream(new byte[] { 1, 2, 3 }), NbtAccounter.unlimitedHeap()));
    }

    @Test
    void weightedHubPiecesConnectToTerminalPool() throws IOException {
        Set<Integer> branchCounts = new HashSet<>();
        for (String variant : new String[] { "probe", "probe_cracked", "probe_mossy" }) {
            CompoundTag hub = readTemplate(variant);
            String blocks = hub.get("blocks").toString();
            assertTrue(blocks.contains("neroruins:probe_end"));
            assertTrue(blocks.contains("neroruins:probe_hub"));
            assertTrue(hub.get("palette").toString().contains("east_up"));
            branchCounts.add(blocks.split("minecraft:jigsaw", -1).length - 1);
        }
        assertEquals(Set.of(1, 2, 3), branchCounts);
        CompoundTag end = readTemplate("probe_end");
        assertTrue(end.get("blocks").toString().contains("neroruins:probe_end"));
        assertTrue(end.get("blocks").toString().contains("minecraft:empty"));
        assertTrue(end.get("palette").toString().contains("west_up"));
        try (InputStream stream = getClass().getResourceAsStream(
                "/data/neroruins/worldgen/template_pool/probe_end.json")) {
            assertNotNull(stream);
            var pool = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("neroruins:probe_end", pool.getAsJsonArray("elements").get(0).getAsJsonObject()
                    .getAsJsonObject("element").get("location").getAsString());
        }
    }

    private CompoundTag readTemplate(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/data/neroruins/structure/" + name + ".nbt")) {
            assertNotNull(input);
            return NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        }
    }
}
