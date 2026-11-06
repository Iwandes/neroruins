package za.co.neroland.neroruins.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
//? if <26.2 {
/*import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import za.co.neroland.neroruins.registry.ModProcessors;
*///?}

/**
 * {@code neroruins:puzzle_seed}: gives each generated puzzle its own solution. A ballast plate's target is
 * derived from the world seed and the plate's final position, so the same seed and position always produce
 * the same puzzle and different worlds or instances differ.
 */
//? if >=26.2 {
public final class PuzzleSeedProcessor implements StructureProcessor {
//?} else {
/*public final class PuzzleSeedProcessor extends StructureProcessor {
*///?}
    public static final PuzzleSeedProcessor INSTANCE = new PuzzleSeedProcessor();
    public static final MapCodec<PuzzleSeedProcessor> CODEC = MapCodec.unit(INSTANCE);

    private PuzzleSeedProcessor() { }

    static StructureTemplate.StructureBlockInfo seed(LevelReader level, StructureTemplate.StructureBlockInfo current) {
        if (current == null || !(current.state().getBlock() instanceof BallastPlateBlock)) return current;
        long seed = level instanceof WorldGenLevel generating ? generating.getSeed() : 0L;
        int target = BallastPuzzle.target(seed, current.pos().asLong());
        return new StructureTemplate.StructureBlockInfo(current.pos(),
                current.state().setValue(BallastPlateBlock.TARGET, target), current.nbt());
    }

    //? if >=26.2 {
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos target, BlockPos reference,
            BlockPos templateRelative, StructureTemplate.StructureBlockInfo current, StructurePlaceSettings settings) {
        return seed(level, current);
    }

    @Override
    public MapCodec<PuzzleSeedProcessor> codec() { return CODEC; }
    //?} else {
    /*@Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos target, BlockPos reference,
            StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current,
            StructurePlaceSettings settings) {
        return seed(level, current);
    }

    @Override
    protected StructureProcessorType<?> getType() { return ModProcessors.PUZZLE_SEED.get(); }
    *///?}
}
