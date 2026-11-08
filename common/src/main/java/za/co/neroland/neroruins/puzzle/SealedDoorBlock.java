package za.co.neroland.neroruins.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
//? if <1.21.2 {
/*import net.minecraft.world.level.block.state.properties.DirectionProperty;
*///?}
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * A two-block-high sealed door. It has no lock of its own: a solved puzzle within reach opens every sealed door
 * around it (see {@link PuzzleUnlock}). Open is world state shared by everyone and never closes again. The door
 * is unbreakable in survival, so a puzzle cannot be skipped by mining through it.
 */
public final class SealedDoorBlock extends Block {
    public static final MapCodec<SealedDoorBlock> CODEC = BlockCodecs.simple(SealedDoorBlock::new);
    //? if >=1.21.2 {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    //?} else {
    /*public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    *///?}
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape THRESHOLD = Block.box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape LINTEL = Block.box(0, 14, 0, 16, 16, 16);

    @SuppressWarnings("this-escape")
    public SealedDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER).setValue(OPEN, false));
    }

    protected MapCodec<SealedDoorBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, OPEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN)) return net.minecraft.world.phys.shapes.Shapes.block();
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? THRESHOLD : LINTEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        if (!context.getLevel().getBlockState(above).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos other = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        if (level.getBlockState(other).getBlock() == this) {
            level.setBlock(other, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
