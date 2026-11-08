package za.co.neroland.neroruins.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * A tuning crystal: glows the colour of one pylon's target pitch. {@link #INDEX} pairs it with the pylon beside it;
 * {@link #TONE} is written by the {@code neroruins:puzzle_seed} processor. Unbreakable.
 */
public final class ResonanceClueBlock extends Block {
    public static final MapCodec<ResonanceClueBlock> CODEC = BlockCodecs.simple(ResonanceClueBlock::new);
    public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, PuzzleSeeds.PYLONS - 1);
    public static final IntegerProperty TONE = IntegerProperty.create("tone", 0, PuzzleSeeds.PITCHES - 1);

    @SuppressWarnings("this-escape")
    public ResonanceClueBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(INDEX, 0).setValue(TONE, 0));
    }

    protected MapCodec<ResonanceClueBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INDEX, TONE);
    }
}
