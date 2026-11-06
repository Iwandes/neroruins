package za.co.neroland.neroruins.lore;

import java.util.UUID;

import za.co.neroland.neroruins.data.PlayerArchive;

public final class ArchiveReading {
    public enum Result { INVALID, ALREADY_ARCHIVED, ARCHIVED, CHAPTER_COMPLETED }

    private ArchiveReading() { }

    public static Result record(PlayerArchive archive, UUID player, String id, long readAt) {
        var chapter = LogCatalog.chapterOf(id);
        if (chapter.isEmpty()) return Result.INVALID;
        boolean completeBefore = LogCatalog.chapterComplete(chapter.get(), archive.export(player).keySet());
        if (!archive.record(player, id, readAt)) return Result.ALREADY_ARCHIVED;
        return !completeBefore && LogCatalog.chapterComplete(chapter.get(), archive.export(player).keySet())
                ? Result.CHAPTER_COMPLETED : Result.ARCHIVED;
    }
}
