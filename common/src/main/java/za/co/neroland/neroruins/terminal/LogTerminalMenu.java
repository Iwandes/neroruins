package za.co.neroland.neroruins.terminal;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import za.co.neroland.neroruins.data.PlayerArchive;
import za.co.neroland.neroruins.lore.ArchiveReading;
import za.co.neroland.neroruins.lore.LogCatalog;
import za.co.neroland.neroruins.progression.RuinMilestones;
import za.co.neroland.neroruins.registry.ModMenuTypes;

public final class LogTerminalMenu extends AbstractContainerMenu {
    private final LogTerminalBlockEntity terminal;
    private final ContainerData data;

    public LogTerminalMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    @SuppressWarnings("this-escape")
    public LogTerminalMenu(int id, Inventory inventory, LogTerminalBlockEntity terminal) {
        super(ModMenuTypes.LOG_TERMINAL.get(), id);
        this.terminal = terminal;
        this.data = terminal == null ? new SimpleContainerData(2) : new ContainerData() {
            public int get(int index) {
                if (index == 0) return LogCatalog.ids().indexOf(terminal.logId()) + 1;
                if (index == 1 && inventory.player instanceof ServerPlayer serverPlayer
                        && LogCatalog.translationPrefix(terminal.logId()).isPresent()) {
                    return PlayerArchive.get(serverPlayer.level().getServer())
                            .contains(serverPlayer.getUUID(), terminal.logId()) ? 1 : 0;
                }
                return 0;
            }
            public void set(int index, int value) { }
            public int getCount() { return 2; }
        };
        addDataSlots(data);
    }

    public String logId() {
        int index = data.get(0) - 1;
        return index >= 0 && index < LogCatalog.ids().size() ? LogCatalog.ids().get(index) : "";
    }

    public boolean archived() { return data.get(1) != 0; }

    @Override
    public boolean stillValid(Player player) { return terminal == null || terminal.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer serverPlayer) || terminal == null
                || !TerminalAccess.mayArchive(button, terminal.stillValid(player), terminal.logId())) return false;
        String id = terminal.logId();
        var result = ArchiveReading.record(PlayerArchive.get(serverPlayer.level().getServer()),
                serverPlayer.getUUID(), id, System.currentTimeMillis());
        if (result == ArchiveReading.Result.INVALID) return false;
        if (result == ArchiveReading.Result.ARCHIVED || result == ArchiveReading.Result.CHAPTER_COMPLETED) {
            RuinMilestones.firstLog(serverPlayer);
        }
        Component feedback = Component.translatable(result == ArchiveReading.Result.ALREADY_ARCHIVED
                ? "message.neroruins.shard.duplicate" : "message.neroruins.shard.saved");
        //? if >=26.1 {
        serverPlayer.sendSystemMessage(feedback);
        //?} else {
        /*serverPlayer.displayClientMessage(feedback, false);
        *///?}
        if (result == ArchiveReading.Result.CHAPTER_COMPLETED) {
            Component unlocked = Component.translatable("message.neroruins.codex.unlocked",
                    Component.translatable("codex.neroruins." + LogCatalog.chapterOf(id).orElseThrow() + ".title"));
            //? if >=26.1 {
            serverPlayer.sendSystemMessage(unlocked);
            //?} else {
            /*serverPlayer.displayClientMessage(unlocked, false);
            *///?}
        }
        return true;
    }
}
