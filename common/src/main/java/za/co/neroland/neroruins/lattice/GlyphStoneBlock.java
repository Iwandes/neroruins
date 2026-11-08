package za.co.neroland.neroruins.lattice;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * Architect glyph stone: latticework carrying one of sixteen luminous glyphs. The glyph is block state so
 * templates (and later puzzle processors) can write wall clues; it carries no player data.
 */
public final class GlyphStoneBlock extends Block {
    public static final MapCodec<GlyphStoneBlock> CODEC = BlockCodecs.simple(GlyphStoneBlock::new);
    public static final int GLYPHS = 16;
    public static final IntegerProperty GLYPH = IntegerProperty.create("glyph", 0, GLYPHS - 1);

    @SuppressWarnings("this-escape")
    public GlyphStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GLYPH, 0));
    }

    protected MapCodec<GlyphStoneBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLYPH);
    }
}
