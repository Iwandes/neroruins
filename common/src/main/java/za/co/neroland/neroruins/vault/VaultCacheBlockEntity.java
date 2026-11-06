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
    /** Whether this cache is gated by a room puzzle (set by its template), and whether that puzzle is solved. */
    private boolean puzzle;
    private boolean solved;
    /** The structure this cache belongs to, for the scoped {@code vault_claimed} milestone channel. */
    private String structure = "";
    public VaultCacheBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.VAULT_CACHE.get(), pos, state); }
    public String lootTable() { return lootTable; }
    public boolean puzzle() { return puzzle; }
    public boolean sealed() { return puzzle && !solved; }
    public String structure() { return structure; }

    /** Detaches a showcase copy from the world's per-structure vault counts (gallery use only). */
    public void demonstrationOnly() {
        structure = "";
        setChanged();
    }

    /** Marks the room puzzle solved. World state, not personal data: it stays solved for everyone. */
    public void unseal() {
        if (!puzzle || solved) return;
        solved = true;
        setChanged();
    }
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
        output.putBoolean("puzzle", puzzle);
        output.putBoolean("solved", solved);
        output.putString("structure", structure);
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
        puzzle = input.getBooleanOr("puzzle", false);
        solved = input.getBooleanOr("solved", false);
        String owner = input.getStringOr("structure", "");
        structure = owner.length() <= 128 && Identifier.tryParse(owner) != null ? owner : "";
    }

    public Component getDisplayName() { return Component.translatable("container.neroruins.vault_cache"); }
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new VaultCacheMenu(id, inventory, this);
    }
}
