package za.co.neroland.neroruins.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import za.co.neroland.neroruins.lore.CodexPagination;
import za.co.neroland.neroruins.lore.LogCatalog;
import za.co.neroland.neroruins.network.CodexDisplay;
import za.co.neroland.neroruins.network.CodexSnapshot;

public final class CodexClient {
    private CodexClient() { }

    public static void init() {
        CodexDisplay.install(CodexClient::open);
        ExpeditionMapClient.init();
    }

    private static void open(CodexSnapshot snapshot) {
        List<Component> pages = new ArrayList<>();
        Set<String> logs = Set.copyOf(snapshot.logs());
        var overview = Component.translatable("codex.neroruins.overview").append("\n\n")
                .append(Component.translatable(snapshot.logs().isEmpty()
                        ? "message.neroruins.codex.empty" : "codex.neroruins.pages_hint"))
                .append("\n\n");
        List<List<String>> overviewGroups = new ArrayList<>();
        overviewGroups.add(lines(overview));
        for (String chapter : LogCatalog.chapters()) {
            int count = LogCatalog.chapterReadCount(chapter, logs);
            overviewGroups.add(lines(Component.translatable("codex.neroruins.progress",
                    Component.translatable("codex.neroruins." + chapter + ".title"), count)));
        }
        for (String page : CodexPagination.groupedPages(overviewGroups)) pages.add(Component.literal(page));
        for (String id : snapshot.logs()) {
            String prefix = LogCatalog.translationPrefix(id).orElseThrow();
            addPages(pages, Component.translatable(prefix + ".title").append("\n\n")
                    .append(Component.translatable(prefix + ".speaker")).append("\n\n")
                    .append(Component.translatable(prefix + ".body")));
        }
        for (String chapter : LogCatalog.chapters()) {
            if (LogCatalog.chapterComplete(chapter, logs)) {
                String prefix = "codex.neroruins." + chapter;
                addPages(pages, Component.translatable(prefix + ".title").append("\n\n")
                        .append(Component.translatable(prefix + ".body")));
            }
        }
        //? if >=26.2 {
        Minecraft.getInstance().setScreenAndShow(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
        //?} else {
        /*Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
        *///?}
    }

    private static void addPages(List<Component> pages, Component text) {
        for (String page : CodexPagination.pages(lines(text))) pages.add(Component.literal(page));
    }

    private static List<String> lines(Component text) {
        return Minecraft.getInstance().font.getSplitter().splitLines(text, 114, Style.EMPTY).stream()
                .map(line -> line.getString()).toList();
    }
}
