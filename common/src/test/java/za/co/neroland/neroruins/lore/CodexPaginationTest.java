package za.co.neroland.neroruins.lore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CodexPaginationTest {
    @Test
    void splitsLongEntriesWithoutDroppingLines() {
        List<String> lines = IntStream.range(0, 31).mapToObj(Integer::toString).toList();
        var pages = CodexPagination.pages(lines);
        assertEquals(3, pages.size());
        assertEquals(lines, pages.stream().flatMap(page -> page.lines()).toList());
    }

    @Test
    void providesAnEmptyPageWhenThereAreNoLines() {
        assertEquals(List.of(""), CodexPagination.pages(List.of()));
    }

    @Test
    void keepsChapterEntriesTogetherAndSplitsOversizedTranslations() {
        var header = IntStream.range(0, 13).mapToObj(Integer::toString).toList();
        var chapter = List.of("chapter", "0/6");
        var longTranslation = IntStream.range(0, 20).mapToObj(i -> "line" + i).toList();
        var pages = CodexPagination.groupedPages(List.of(header, chapter, longTranslation));
        assertEquals(String.join("\n", header), pages.get(0));
        assertEquals(chapter, pages.get(1).lines().limit(2).toList());
        assertEquals(35, pages.stream().flatMap(String::lines).count());
        assertEquals(List.of(""), CodexPagination.groupedPages(List.of()));
    }
}
