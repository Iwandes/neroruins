package za.co.neroland.neroruins.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import za.co.neroland.neroruins.vault.VaultCacheMenu;

public final class VaultCacheScreen extends AbstractContainerScreen<VaultCacheMenu> {
    private Button claimButton;

    public VaultCacheScreen(VaultCacheMenu menu, Inventory inventory, Component title) {
        //? if >=26.1 {
        super(menu, inventory, title, 220, 120);
        //?} else {
        /*super(menu, inventory, title);
        this.imageWidth = 220;
        this.imageHeight = 120;
        *///?}
    }

    @Override
    protected void init() {
        super.init();
        claimButton = addRenderableWidget(Button.builder(Component.translatable("gui.neroruins.vault.claim"), button -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 40, topPos + 84, 140, 20).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        claimButton.active = menu.status() != 1 && menu.status() != 2;
    }

    private void panel(GuiGraphicsExtractor g) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF293139);
        g.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFF0D151D);
        g.fill(leftPos + 10, topPos + 24, leftPos + 210, topPos + 25, 0xFFE1B96B);
    }

    //? if >=26.1 {
    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        panel(g);
        super.extractContents(g, mouseX, mouseY, partialTick);
        drawText(g);
    }
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) { }
    //?} else {
    /*@Override
    protected void renderBg(GuiGraphicsExtractor g, float partialTick, int mouseX, int mouseY) { panel(g); }
    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        drawText(g);
        renderTooltip(g, mouseX, mouseY);
    }
    @Override
    protected void renderLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) { }
    *///?}

    private void drawText(GuiGraphicsExtractor g) {
        g.text(font, title, leftPos + 12, topPos + 10, 0xFFF3E8D1, false);
        Component status = Component.translatable("gui.neroruins.vault.status." + menu.status());
        var lines = font.split(status, 194);
        for (int i = 0; i < Math.min(lines.size(), 4); i++) {
            g.text(font, lines.get(i), leftPos + 13, topPos + 33 + i * 10, 0xFFD4D7D5, false);
        }
    }
}
