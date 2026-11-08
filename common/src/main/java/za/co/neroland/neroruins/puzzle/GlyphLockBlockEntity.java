package za.co.neroland.neroruins.puzzle;

import com.mojang.serialization.Codec;
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
import za.co.neroland.neroruins.registry.ModBlockEntities;

/**
 * World state of a glyph lock: the answer written at generation, whether it is open, and until which game tick it
 * refuses input after a wrong answer. Nothing here identifies a player.
 */
public final class GlyphLockBlockEntity extends BlockEntity implements MenuProvider {
    private int[] sequence = new int[0];
    private boolean solved;
    private long cooldownUntil;

    public GlyphLockBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.GLYPH_LOCK.get(), pos, state); }

    public boolean configured() { return GlyphSequence.valid(sequence); }
    public boolean solved() { return solved; }
    public int[] sequence() { return sequence.clone(); }

    public boolean coolingDown() { return level != null && level.getGameTime() < cooldownUntil; }

    /** Checks an answer; a wrong one starts the cooldown. Returns whether the lock is now open. */
    public boolean attempt(int[] entered) {
        if (solved) return true;
        if (!configured() || coolingDown() || level == null) return false;
        if (GlyphSequence.matches(sequence, entered)) {
            solved = true;
        } else {
            cooldownUntil = level.getGameTime() + GlyphSequence.COOLDOWN_TICKS;
        }
        setChanged();
        return solved;
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
        output.store("sequence", Codec.INT.listOf(), java.util.Arrays.stream(sequence).boxed().toList());
        output.putBoolean("solved", solved);
        output.putLong("cooldown_until", cooldownUntil);
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
        int[] stored = input.read("sequence", Codec.INT.listOf()).orElse(java.util.List.of()).stream()
                .mapToInt(Integer::intValue).toArray();
        sequence = GlyphSequence.valid(stored) ? stored : new int[0];
        solved = input.getBooleanOr("solved", false);
        cooldownUntil = Math.max(0L, input.getLongOr("cooldown_until", 0L));
    }

    public Component getDisplayName() { return Component.translatable("container.neroruins.glyph_lock"); }
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GlyphLockMenu(id, inventory, this);
    }
}
