package za.co.neroland.neroruins.lore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroruins.data.PlayerArchive;

class ArchiveReadingTest {
    @Test
    void recordsOnceAndReportsChapterCompletionOnlyOnTheFinalNewLog() {
        PlayerArchive archive = new PlayerArchive();
        UUID reader = UUID.randomUUID();
        for (int i = 1; i < 6; i++) {
            assertEquals(ArchiveReading.Result.ARCHIVED,
                    ArchiveReading.record(archive, reader, "neroruins:earth." + i, i));
        }
        assertEquals(ArchiveReading.Result.CHAPTER_COMPLETED,
                ArchiveReading.record(archive, reader, "neroruins:earth.6", 6));
        assertEquals(ArchiveReading.Result.ALREADY_ARCHIVED,
                ArchiveReading.record(archive, reader, "neroruins:earth.6", 7));
        assertEquals(6L, archive.export(reader).get("neroruins:earth.6"));
        assertTrue(archive.export(UUID.randomUUID()).isEmpty());
    }

    @Test
    void refusesUnknownIdsWithoutCreatingArchiveEntries() {
        PlayerArchive archive = new PlayerArchive();
        UUID reader = UUID.randomUUID();
        assertEquals(ArchiveReading.Result.INVALID, ArchiveReading.record(archive, reader, null, 1));
        assertEquals(ArchiveReading.Result.INVALID, ArchiveReading.record(archive, reader, "neroruins:earth.7", 1));
        assertTrue(archive.export(reader).isEmpty());
    }
}
