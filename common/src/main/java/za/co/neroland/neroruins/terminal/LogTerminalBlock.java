package za.co.neroland.neroruins.terminal;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//? if >=1.21.2 {
import net.minecraft.world.level.block.state.properties.EnumProperty;
//?} else {
/*import net.minecraft.world.level.block.state.properties.DirectionProperty;
*///?}
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
    /** The side the screen is on. Templates set it so the screen faces the room; players get it facing them. */
    //? if >=1.21.2 {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    //?} else {
    /*public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    *///?}

    @SuppressWarnings("this-escape")
    public LogTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
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
