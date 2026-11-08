package za.co.neroland.neroruins.puzzle;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * A pylon of a {@code resonance_tuning} room. Using it steps its pitch and sounds the note; its band glows the
 * colour of its current pitch. The target is the colour of the {@link ResonanceClueBlock} crystal with the same
 * {@link #INDEX} set beside it. When every pylon in the room matches, the room's doors open. Unbreakable.
 */
public final class ResonancePylonBlock extends Block {
    public static final MapCodec<ResonancePylonBlock> CODEC = BlockCodecs.simple(ResonancePylonBlock::new);
    public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, PuzzleSeeds.PYLONS - 1);
    public static final IntegerProperty PITCH = IntegerProperty.create("pitch", 0, PuzzleSeeds.PITCHES - 1);
    public static final IntegerProperty TARGET = IntegerProperty.create("target", 0, PuzzleSeeds.PITCHES - 1);
    /** How far apart a room's pylons may stand. */
    public static final int ROOM_REACH = 6;
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    @SuppressWarnings("this-escape")
    public ResonancePylonBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(INDEX, 0).setValue(PITCH, 0).setValue(TARGET, 0));
    }

    protected MapCodec<ResonancePylonBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INDEX, PITCH, TARGET);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            List<ResonanceTuning.Pylon> room = room(server, pos);
            if (ResonanceTuning.solved(room)) return InteractionResult.CONSUME;
            int pitch = ResonanceTuning.next(state.getValue(PITCH));
            server.setBlock(pos, state.setValue(PITCH, pitch), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F,
                    ResonanceTuning.soundPitch(pitch));
            if (ResonanceTuning.solved(room(server, pos))) PuzzleUnlock.open(server, pos, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    private static List<ResonanceTuning.Pylon> room(ServerLevel level, BlockPos pos) {
        List<ResonanceTuning.Pylon> pylons = new ArrayList<>();
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-ROOM_REACH, -2, -ROOM_REACH), pos.offset(ROOM_REACH, 2, ROOM_REACH))) {
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof ResonancePylonBlock) {
                pylons.add(new ResonanceTuning.Pylon(state.getValue(PITCH), state.getValue(TARGET)));
            }
        }
        return pylons;
    }
}
