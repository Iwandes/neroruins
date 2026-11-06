package za.co.neroland.neroruins.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import za.co.neroland.neroruins.lore.LogCatalog;
import za.co.neroland.neroruins.terminal.LogTerminalMenu;

public final class LogTerminalScreen extends AbstractContainerScreen<LogTerminalMenu> {
    private static final int LINES_PER_PAGE = 10;
    private int page;
    private Button archiveButton;
    private Button previousButton;
    private Button nextButton;

    public LogTerminalScreen(LogTerminalMenu menu, Inventory inventory, Component title) {
        //? if >=26.1 {
        super(menu, inventory, title, 240, 190);
        //?} else {
        /*super(menu, inventory, title);
        this.imageWidth = 240;
        this.imageHeight = 190;
        *///?}
    }

    @Override
    protected void init() {
        super.init();
        previousButton = addRenderableWidget(Button.builder(Component.translatable("gui.neroruins.terminal.previous"), b -> page = Math.max(0, page - 1))
                .bounds(leftPos + 12, topPos + 158, 28, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.translatable("gui.neroruins.terminal.next"), b -> {
            int pages = Math.max(1, (lines().size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
            page = Math.min(pages - 1, page + 1);
        }).bounds(leftPos + 44, topPos + 158, 28, 20).build());
        archiveButton = addRenderableWidget(Button.builder(Component.translatable("gui.neroruins.terminal.archive"), b -> {
            if (minecraft != null && minecraft.gameMode != null && !menu.logId().isEmpty()) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(leftPos + 105, topPos + 158, 123, 20).build());
        previousButton.active = false;
        nextButton.active = false;
        archiveButton.active = false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int pages = Math.max(1, (lines().size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        page = Math.min(page, pages - 1);
        previousButton.active = page > 0;
        nextButton.active = page + 1 < pages;
        archiveButton.active = !menu.logId().isEmpty() && !menu.archived();
        archiveButton.setMessage(Component.translatable(menu.archived()
                ? "gui.neroruins.terminal.archived" : "gui.neroruins.terminal.archive"));
    }

    private List<FormattedCharSequence> lines() {
        var prefix = LogCatalog.translationPrefix(menu.logId());
        Component content = prefix.<Component>map(key -> Component.translatable(key + ".title").append("\n\n")
                .append(Component.translatable(key + ".speaker")).append("\n\n")
                .append(Component.translatable(key + ".body")))
                .orElseGet(() -> Component.translatable("gui.neroruins.terminal.empty"));
        return font.split(content, 210);
    }

    private void panel(GuiGraphicsExtractor g) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF182631);
        g.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFF0B131D);
        g.fill(leftPos + 10, topPos + 23, leftPos + 230, topPos + 24, 0xFF4BC6C5);
    }

    //? if >=26.1 {
    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        panel(g);
        super.extractContents(g, mouseX, mouseY, partialTick);
        drawText(g);
    }
    //?} else {
    /*@Override
    protected void renderBg(GuiGraphicsExtractor g, float partialTick, int mouseX, int mouseY) { panel(g); }

    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        drawText(g);
        renderTooltip(g, mouseX, mouseY);
    }
    *///?}

    private void drawText(GuiGraphicsExtractor g) {
        g.text(font, title, leftPos + 12, topPos + 10, 0xFFD9F6F3, false);
        List<FormattedCharSequence> text = lines();
        int pages = Math.max(1, (text.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        page = Math.min(page, pages - 1);
        int start = page * LINES_PER_PAGE;
        for (int i = start; i < Math.min(text.size(), start + LINES_PER_PAGE); i++) {
            g.text(font, text.get(i), leftPos + 14, topPos + 32 + (i - start) * 11, 0xFFBBD2D6, false);
        }
        g.text(font, Component.literal((page + 1) + " / " + pages), leftPos + 80, topPos + 164, 0xFF8AA6AE, false);
    }

    //? if >=26.1 {
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) { }
    //?} else {
    /*@Override
    protected void renderLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) { }
    *///?}
}
