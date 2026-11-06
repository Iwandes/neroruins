package za.co.neroland.neroruins.lore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class LogCatalogTest {
    @Test
    void everyLogHasCompleteBoundedText() throws Exception {
        var ids = LogCatalog.ids();
        assertEquals(54, ids.size());
        assertEquals(ids.size(), new HashSet<>(ids).size());
        try (var input = getClass().getResourceAsStream("/assets/neroruins/lang/en_us.json")) {
            assertNotNull(input);
            var lang = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String id : ids) {
                String prefix = LogCatalog.translationPrefix(id).orElseThrow();
                assertTrue(lang.has(prefix + ".title"));
                assertTrue(lang.has(prefix + ".speaker"));
                assertTrue(lang.has(prefix + ".body"));
                assertTrue(lang.get(prefix + ".body").getAsString().length() <= 600);
            }
            for (String chapter : LogCatalog.chapters()) {
                assertTrue(lang.has("codex.neroruins." + chapter + ".title"));
                assertTrue(lang.has("codex.neroruins." + chapter + ".body"));
            }
        }
    }

    @Test
    void rejectsUnknownAndMalformedIds() {
        assertFalse(LogCatalog.translationPrefix(null).isPresent());
        assertFalse(LogCatalog.translationPrefix("other:earth.1").isPresent());
        assertFalse(LogCatalog.translationPrefix("neroruins:earth.7").isPresent());
        assertFalse(LogCatalog.translationPrefix("neroruins:unknown.1").isPresent());
        assertEquals("earth", LogCatalog.chapterOf("neroruins:earth.1").orElseThrow());
        assertTrue(LogCatalog.chapterOf("neroruins:earth.7").isEmpty());
    }

    @Test
    void chapterUnlockRequiresEveryLogFromThatChapter() {
        var logs = new HashSet<>(LogCatalog.ids().stream().filter(id -> id.startsWith("neroruins:earth.")).toList());
        assertTrue(LogCatalog.chapterComplete("earth", logs));
        assertEquals(6, LogCatalog.chapterReadCount("earth", logs));
        logs.remove("neroruins:earth.6");
        assertEquals(5, LogCatalog.chapterReadCount("earth", logs));
        assertFalse(LogCatalog.chapterComplete("earth", logs));
        assertFalse(LogCatalog.chapterComplete("unknown", logs));
    }
}
