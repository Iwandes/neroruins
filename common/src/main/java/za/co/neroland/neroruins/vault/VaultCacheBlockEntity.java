package za.co.neroland.neroruins.vault;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import za.co.neroland.neroruins.registry.ModBlockEntities;

public final class VaultCacheBlockEntity extends BlockEntity implements MenuProvider {
    private String lootTable = "";
    public VaultCacheBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.VAULT_CACHE.get(), pos, state); }
    public String lootTable() { return lootTable; }
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
        output.putString("loot_table", lootTable);
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
        String value = input.getStringOr("loot_table", "");
        lootTable = value.length() <= 128 && Identifier.tryParse(value) != null ? value : "";
    }

    public Component getDisplayName() { return Component.translatable("container.neroruins.vault_cache"); }
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new VaultCacheMenu(id, inventory, this);
    }
}
