package za.co.neroland.neroruins.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import za.co.neroland.neroruins.expedition.ExpeditionMapDisplay;
import za.co.neroland.neroruins.expedition.ExpeditionSnapshot;
import za.co.neroland.neroruins.lore.CodexPagination;

/** Shows the player's own Expedition Record as a book: one entry per discovered complex, newest first. */
public final class ExpeditionMapClient {
    private ExpeditionMapClient() { }

    public static void init() { ExpeditionMapDisplay.install(ExpeditionMapClient::open); }

    private static void open(ExpeditionSnapshot snapshot) {
        List<List<String>> groups = new ArrayList<>();
        groups.add(lines(Component.translatable("expedition.neroruins.title").append("\n\n")
                .append(Component.translatable(snapshot.entries().isEmpty()
                        ? "expedition.neroruins.empty" : "expedition.neroruins.hint"))
                .append("\n")));
        for (ExpeditionSnapshot.Entry entry : snapshot.entries()) {
            MutableComponent text = Component.translatable("expedition.neroruins.entry",
                    Component.translatable("family.neroruins." + entry.family()),
                    Component.translatable("size.neroruins." + entry.sizeClass())).append("\n")
                    .append(Component.translatable("expedition.neroruins.world", entry.dimension())).append("\n")
                    .append(entry.positioned()
                            ? Component.translatable("expedition.neroruins.position", entry.x(), entry.z())
                            : Component.translatable("expedition.neroruins.position_not_kept"))
                    .append("\n");
            groups.add(lines(text));
        }
        List<Component> pages = new ArrayList<>();
        for (String page : CodexPagination.groupedPages(groups)) pages.add(Component.literal(page));
        //? if >=26.2 {
        Minecraft.getInstance().setScreenAndShow(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
        //?} else {
        /*Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
        *///?}
    }

    private static List<String> lines(Component text) {
        return Minecraft.getInstance().font.getSplitter().splitLines(text, 114, Style.EMPTY).stream()
                .map(line -> line.getString()).toList();
    }
}
