package za.co.neroland.neroruins.lore;

import java.util.ArrayList;
import java.util.List;

public final class CodexPagination {
    private static final int LINES_PER_PAGE = 14;

    private CodexPagination() { }

    public static List<String> pages(List<String> lines) {
        if (lines.isEmpty()) return List.of("");
        List<String> pages = new ArrayList<>();
        for (int start = 0; start < lines.size(); start += LINES_PER_PAGE) {
            pages.add(String.join("\n", lines.subList(start, Math.min(start + LINES_PER_PAGE, lines.size()))));
        }
        return List.copyOf(pages);
    }

    public static List<String> groupedPages(List<List<String>> groups) {
        List<String> result = new ArrayList<>();
        List<String> page = new ArrayList<>();
        for (List<String> group : groups) {
            if (group.size() <= LINES_PER_PAGE && page.size() + group.size() > LINES_PER_PAGE) {
                result.add(String.join("\n", page));
                page.clear();
            }
            for (String line : group) {
                if (page.size() == LINES_PER_PAGE) {
                    result.add(String.join("\n", page));
                    page.clear();
                }
                page.add(line);
            }
        }
        if (!page.isEmpty() || result.isEmpty()) result.add(String.join("\n", page));
        return List.copyOf(result);
    }
}
