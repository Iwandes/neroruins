package za.co.neroland.neroruins.puzzle;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
//? if >=1.21.2 {
import net.minecraft.world.level.block.state.properties.EnumProperty;
//?} else {
/*import net.minecraft.world.level.block.state.properties.DirectionProperty;
*///?}
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * A floor conduit of a {@code conduit_routing} room: a straight run or a corner. Using it turns it a quarter turn
 * clockwise; every turn re-traces power from the sources nearby and lights the pieces that carry it, so the path
 * is visible as it forms. Unbreakable. The {@code neroruins:puzzle_seed} processor turns each piece away from
 * its authored, solved facing at generation.
 */
public final class ConduitBlock extends Block {
    public static final MapCodec<ConduitBlock> CODEC = BlockCodecs.simple(ConduitBlock::new);
    //? if >=1.21.2 {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    //?} else {
    /*public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    *///?}
    public static final BooleanProperty CORNER = BooleanProperty.create("corner");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** How far from a turned conduit its room's sources and pieces may lie. */
    public static final int ROOM_REACH = 8;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    @SuppressWarnings("this-escape")
    public ConduitBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CORNER, false)
                .setValue(POWERED, false));
    }

    protected MapCodec<ConduitBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CORNER, POWERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public static Set<Direction> openings(BlockState state) {
        return ConduitRouting.openings(state.getValue(FACING), state.getValue(CORNER));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            if (isLocked(server, pos)) return InteractionResult.CONSUME;
            server.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, 1.2F);
            retrace(server, pos, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    /** A room stops turning once its lock is powered. */
    private static boolean isLocked(ServerLevel level, BlockPos pos) {
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-ROOM_REACH, -1, -ROOM_REACH), pos.offset(ROOM_REACH, 1, ROOM_REACH))) {
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof ConduitLockBlock && state.getValue(ConduitLockBlock.POWERED)) return true;
        }
        return false;
    }

    /** Re-traces every source in reach of {@code pos} and updates the lit pieces and any lock reached. */
    static void retrace(ServerLevel level, BlockPos pos, ServerPlayer player) {
        List<BlockPos> sources = new ArrayList<>();
        List<BlockPos> pieces = new ArrayList<>();
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-ROOM_REACH, -1, -ROOM_REACH), pos.offset(ROOM_REACH, 1, ROOM_REACH))) {
            Block block = level.getBlockState(at).getBlock();
            if (block instanceof ConduitSourceBlock) sources.add(at.immutable());
            else if (block instanceof ConduitBlock) pieces.add(at.immutable());
        }
        ConduitRouting.Grid grid = at -> {
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof ConduitLockBlock) return new ConduitRouting.Lock();
            if (state.getBlock() instanceof ConduitBlock) return new ConduitRouting.Pipe(openings(state));
            return new ConduitRouting.Empty();
        };
        Set<BlockPos> lit = new HashSet<>();
        BlockPos reached = null;
        for (BlockPos source : sources) {
            var trace = ConduitRouting.trace(grid, source, level.getBlockState(source).getValue(ConduitSourceBlock.FACING));
            lit.addAll(trace.powered());
            if (trace.solved()) reached = trace.lock();
        }
        for (BlockPos piece : pieces) {
            BlockState state = level.getBlockState(piece);
            boolean on = lit.contains(piece);
            if (state.getValue(POWERED) != on) level.setBlock(piece, state.setValue(POWERED, on), Block.UPDATE_CLIENTS);
        }
        if (reached != null) {
            BlockState lock = level.getBlockState(reached);
            level.setBlock(reached, lock.setValue(ConduitLockBlock.POWERED, true), Block.UPDATE_ALL);
            PuzzleUnlock.open(level, reached, player);
        }
    }
}
