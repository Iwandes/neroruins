package za.co.neroland.neroruins.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import za.co.neroland.neroruins.puzzle.GlyphLockMenu;
import za.co.neroland.neroruins.puzzle.PuzzleSeeds;

/** The glyph keypad: sixteen glyph keys, the entry so far, Clear and Enter. The server checks the answer. */
public final class GlyphLockScreen extends AbstractContainerScreen<GlyphLockMenu> {
    private static final int KEY = 22;
    private static final int GRID_X = 14;
    private static final int GRID_Y = 34;
    private static final int GAP = 6;
    private static final int SIDE_X = 134;
    private static final int GLYPH = 0xFF7FE9FF;
    private final List<Button> keys = new ArrayList<>();
    private Button clear;
    private Button enter;

    public GlyphLockScreen(GlyphLockMenu menu, Inventory inventory, Component title) {
        //? if >=26.1 {
        super(menu, inventory, title, 240, 160);
        //?} else {
        /*super(menu, inventory, title);
        this.imageWidth = 240;
        this.imageHeight = 160;
        *///?}
    }

    private void press(int button) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    @Override
    protected void init() {
        super.init();
        keys.clear();
        for (int glyph = 0; glyph < 16; glyph++) {
            int id = glyph;
            int x = leftPos + GRID_X + (glyph % 4) * (KEY + GAP);
            int y = topPos + GRID_Y + (glyph / 4) * (KEY + GAP);
            keys.add(addRenderableWidget(Button.builder(Component.empty(), button -> press(id))
                    .bounds(x, y, KEY, KEY).build()));
        }
        clear = addRenderableWidget(Button.builder(Component.translatable("gui.neroruins.glyph_lock.clear"),
                button -> press(GlyphLockMenu.CLEAR)).bounds(leftPos + SIDE_X, topPos + 66, 46, 20).build());
        enter = addRenderableWidget(Button.builder(Component.translatable("gui.neroruins.glyph_lock.enter"),
                button -> press(GlyphLockMenu.SUBMIT)).bounds(leftPos + SIDE_X + 50, topPos + 66, 46, 20).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean usable = menu.status() != GlyphLockMenu.OPEN && menu.status() != GlyphLockMenu.INERT;
        boolean waiting = menu.status() == GlyphLockMenu.WRONG || menu.status() == GlyphLockMenu.COOLING;
        boolean full = menu.count() >= PuzzleSeeds.GLYPH_LENGTH;
        for (Button key : keys) key.active = usable && !full;
        clear.active = usable && menu.count() > 0;
        enter.active = usable && full && !waiting;
    }

    private void panel(GuiGraphicsExtractor g) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2A2440);
        g.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFF100D1C);
        g.fill(leftPos + 10, topPos + 24, leftPos + imageWidth - 10, topPos + 25, 0xFF7FE9FF);
        for (int slot = 0; slot < PuzzleSeeds.GLYPH_LENGTH; slot++) {
            int x = leftPos + SIDE_X + slot * 25;
            int y = topPos + 34;
            g.fill(x, y, x + KEY, y + KEY, 0xFF3B3358);
            g.fill(x + 1, y + 1, x + KEY - 1, y + KEY - 1, 0xFF070510);
        }
    }

    private static void glyph(GuiGraphicsExtractor g, int glyph, int x, int y, int colour) {
        if (glyph < 0 || glyph > 15) return;
        for (int row = 0; row < 16; row++) {
            for (int column = 0; column < 16; column++) {
                if (GlyphBitmaps.lit(glyph, column, row)) g.fill(x + column, y + row, x + column + 1, y + row + 1, colour);
            }
        }
    }

    //? if >=26.1 {
    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        panel(g);
        super.extractContents(g, mouseX, mouseY, partialTick);
        overlay(g);
    }
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) { }
    //?} else {
    /*@Override
    protected void renderBg(GuiGraphicsExtractor g, float partialTick, int mouseX, int mouseY) { panel(g); }
    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        overlay(g);
        renderTooltip(g, mouseX, mouseY);
    }
    @Override
    protected void renderLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) { }
    *///?}

    private void overlay(GuiGraphicsExtractor g) {
        g.text(font, title, leftPos + 12, topPos + 10, 0xFFF3E8D1, false);
        for (int index = 0; index < keys.size(); index++) {
            Button key = keys.get(index);
            glyph(g, index, key.getX() + 3, key.getY() + 3, key.active ? GLYPH : 0xFF3F6E7A);
        }
        for (int slot = 0; slot < menu.count(); slot++) {
            glyph(g, menu.entered(slot), leftPos + SIDE_X + slot * 25 + 3, topPos + 37, GLYPH);
        }
        Component status = Component.translatable("gui.neroruins.glyph_lock.status." + menu.status());
        var lines = font.split(status, imageWidth - SIDE_X - 10);
        for (int i = 0; i < Math.min(lines.size(), 6); i++) {
            g.text(font, lines.get(i), leftPos + SIDE_X, topPos + 94 + i * 10, 0xFFD4D7D5, false);
        }
    }
}
