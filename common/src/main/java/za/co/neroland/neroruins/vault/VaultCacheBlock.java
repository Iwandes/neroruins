package za.co.neroland.neroruins.vault;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.neroruins.menu.MenuOpener;

public final class VaultCacheBlock extends BaseEntityBlock {
    public static final MapCodec<VaultCacheBlock> CODEC = BlockCodecs.simple(VaultCacheBlock::new);
    /** Visual only: whether the room puzzle still seals this cache. The block entity is authoritative. */
    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");

    @SuppressWarnings("this-escape")
    public VaultCacheBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SEALED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SEALED);
    }
    protected MapCodec<VaultCacheBlock> codec() { return CODEC; }
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new VaultCacheBlockEntity(pos, state); }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof VaultCacheBlockEntity cache) {
            return MenuOpener.openOrConsume(serverPlayer, cache);
        }
        return InteractionResult.SUCCESS;
    }
}
