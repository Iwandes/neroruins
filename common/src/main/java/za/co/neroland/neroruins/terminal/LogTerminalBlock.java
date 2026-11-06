package za.co.neroland.neroruins.terminal;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.neroruins.menu.MenuOpener;

public final class LogTerminalBlock extends BaseEntityBlock {
    public static final MapCodec<LogTerminalBlock> CODEC = BlockCodecs.simple(LogTerminalBlock::new);
    public LogTerminalBlock(Properties properties) { super(properties); }
    protected MapCodec<LogTerminalBlock> codec() { return CODEC; }
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LogTerminalBlockEntity(pos, state); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof LogTerminalBlockEntity terminal) {
            return MenuOpener.openOrConsume(serverPlayer, terminal);
        }
        return InteractionResult.SUCCESS;
    }
}
