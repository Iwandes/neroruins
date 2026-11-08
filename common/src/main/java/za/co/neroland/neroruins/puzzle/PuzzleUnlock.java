package za.co.neroland.neroruins.puzzle;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import za.co.neroland.neroruins.progression.RuinMilestones;
import za.co.neroland.neroruins.vault.VaultCacheBlockEntity;
import za.co.neroland.neroruins.vault.VaultRewardService;

/**
 * What a solved puzzle does: it opens every {@link SealedDoorBlock} and unseals every sealed vault cache within
 * {@link #HORIZONTAL_REACH} blocks sideways and {@link #VERTICAL_REACH} up or down of the lock. Template authors
 * keep each puzzle room's lock, doors and vault inside that box and away from other rooms' fixtures.
 *
 * <p>Under {@code vaultMode=first_come} the solver's roll is reserved at the moment the vault unseals, so someone
 * who waits beside the vault cannot take it from the player who did the work (decision 028).
 */
public final class PuzzleUnlock {
    public static final int HORIZONTAL_REACH = 8;
    public static final int VERTICAL_REACH = 4;

    private PuzzleUnlock() { }

    /** Opens what the lock at {@code lock} controls. Returns whether anything was still closed. */
    public static boolean open(ServerLevel level, BlockPos lock, ServerPlayer solver) {
        boolean changed = false;
        List<VaultCacheBlockEntity> unsealed = new ArrayList<>();
        for (BlockPos at : BlockPos.betweenClosed(lock.offset(-HORIZONTAL_REACH, -VERTICAL_REACH, -HORIZONTAL_REACH),
                lock.offset(HORIZONTAL_REACH, VERTICAL_REACH, HORIZONTAL_REACH))) {
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof SealedDoorBlock && !state.getValue(SealedDoorBlock.OPEN)) {
                level.setBlock(at, state.setValue(SealedDoorBlock.OPEN, true), Block.UPDATE_ALL);
                changed = true;
            } else if (level.getBlockEntity(at) instanceof VaultCacheBlockEntity cache && cache.sealed()) {
                cache.unseal();
                unsealed.add(cache);
                changed = true;
            }
        }
        if (!changed) return false;
        level.playSound(null, lock, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
        solver.sendSystemMessage(Component.translatable("gui.neroruins.puzzle.opened"));
        RuinMilestones.puzzleSolver(solver);
        for (VaultCacheBlockEntity cache : unsealed) VaultRewardService.holdForSolver(solver, level, cache);
        return true;
    }
}
