package za.co.neroland.neroruins.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import za.co.neroland.neroruins.lore.LogCatalog;
import za.co.neroland.neroruins.registry.ModBlockEntities;

public final class LogTerminalBlockEntity extends BlockEntity implements MenuProvider {
    private String logId = "";

    public LogTerminalBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.LOG_TERMINAL.get(), position, state);
    }

    public String logId() { return logId; }

    public boolean stillValid(Player player) {
        return level != null && player.level() == level && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5) <= 64.0;
    }

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    //?} else {
    /*@Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = ValueOutput.of(tag, registries);
    *///?}
        output.putString("log_id", logId);
    }

    //? if >=1.21.6 {
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    //?} else {
    /*@Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
    *///?}
        String candidate = input.getStringOr("log_id", "");
        logId = LogCatalog.translationPrefix(candidate).isPresent() ? candidate : "";
    }

    @Override
    public Component getDisplayName() { return Component.translatable("container.neroruins.log_terminal"); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new LogTerminalMenu(id, inventory, this);
    }
}
