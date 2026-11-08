package za.co.neroland.neroruins.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//? if >=1.21.2 {
import net.minecraft.world.level.block.state.properties.EnumProperty;
//?} else {
/*import net.minecraft.world.level.block.state.properties.DirectionProperty;
*///?}
import net.minecraft.world.phys.BlockHitResult;
import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.neroruins.menu.MenuOpener;

/** The keypad of a {@code glyph_sequence} room. Unbreakable; the answer lives in its block entity. */
public final class GlyphLockBlock extends BaseEntityBlock {
    public static final MapCodec<GlyphLockBlock> CODEC = BlockCodecs.simple(GlyphLockBlock::new);
    //? if >=1.21.2 {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    //?} else {
    /*public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    *///?}

    @SuppressWarnings("this-escape")
    public GlyphLockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    protected MapCodec<GlyphLockBlock> codec() { return CODEC; }

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

    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GlyphLockBlockEntity(pos, state); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof GlyphLockBlockEntity lock) {
            return MenuOpener.openOrConsume(serverPlayer, lock);
        }
        return InteractionResult.SUCCESS;
    }
}
