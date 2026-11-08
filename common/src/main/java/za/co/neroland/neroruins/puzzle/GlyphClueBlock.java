package za.co.neroland.neroruins.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * A wall stone carrying one glyph of a {@code glyph_sequence} answer. {@link #INDEX} is the glyph's place in the
 * sequence (authored in the template, read left to right as you face the wall); {@link #GLYPH} is written by the
 * {@code neroruins:puzzle_seed} processor. Unbreakable, so the clue cannot be removed from the room.
 */
public final class GlyphClueBlock extends Block {
    public static final MapCodec<GlyphClueBlock> CODEC = BlockCodecs.simple(GlyphClueBlock::new);
    public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, PuzzleSeeds.GLYPH_LENGTH - 1);
    public static final IntegerProperty GLYPH = IntegerProperty.create("glyph", 0, 15);

    @SuppressWarnings("this-escape")
    public GlyphClueBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(INDEX, 0).setValue(GLYPH, 0));
    }

    protected MapCodec<GlyphClueBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INDEX, GLYPH);
    }
}
