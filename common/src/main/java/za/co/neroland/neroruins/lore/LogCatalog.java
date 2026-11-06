package za.co.neroland.neroruins.lore;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class LogCatalog {
    private static final List<String> CHAPTERS = List.of(
            "earth", "luna", "ark", "mars", "cindara", "glacira", "greenxertz", "nyxara", "lattice");
    private static final List<String> IDS = CHAPTERS.stream()
            .flatMap(chapter -> java.util.stream.IntStream.rangeClosed(1, 6)
                    .mapToObj(number -> "neroruins:" + chapter + "." + number)).toList();

    private LogCatalog() { }

    public static Optional<String> translationPrefix(String logId) {
        if (logId == null || !logId.startsWith("neroruins:")) return Optional.empty();
        String path = logId.substring("neroruins:".length());
        int separator = path.indexOf('.');
        if (separator < 1 || separator != path.lastIndexOf('.')) return Optional.empty();
        String chapter = path.substring(0, separator);
        String number = path.substring(separator + 1);
        if (!CHAPTERS.contains(chapter) || !List.of("1", "2", "3", "4", "5", "6").contains(number)) {
            return Optional.empty();
        }
        return Optional.of("log.neroruins." + chapter + "." + number);
    }

    public static List<String> ids() {
        return IDS;
    }

    public static List<String> chapters() {
        return CHAPTERS;
    }

    public static Optional<String> chapterOf(String logId) {
        if (translationPrefix(logId).isEmpty()) return Optional.empty();
        return Optional.of(logId.substring("neroruins:".length(), logId.indexOf('.')));
    }

    public static boolean chapterComplete(String chapter, Set<String> readLogs) {
        return CHAPTERS.contains(chapter) && chapterReadCount(chapter, readLogs) == 6;
    }

    public static int chapterReadCount(String chapter, Set<String> readLogs) {
        if (!CHAPTERS.contains(chapter)) return 0;
        return (int) java.util.stream.IntStream.rangeClosed(1, 6)
                .filter(number -> readLogs.contains("neroruins:" + chapter + "." + number)).count();
    }
}
