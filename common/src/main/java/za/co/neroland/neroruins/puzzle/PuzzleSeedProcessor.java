package za.co.neroland.neroruins.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.state.BlockState;
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
 * {@code neroruins:puzzle_seed}: gives each generated puzzle its own solution from the world seed and the
 * placement, so the same seed and position always produce the same puzzle and different worlds or instances
 * differ. It sets ballast plate targets, glyph clues and the matching glyph-lock answer, scrambles conduits away
 * from their authored solved facing, and sets resonance targets, starting pitches and their clue crystals.
 * Keycard readers and relic sockets need no seed: their requirement is authored per room.
 */
//? if >=26.2 {
public final class PuzzleSeedProcessor implements StructureProcessor {
//?} else {
/*public final class PuzzleSeedProcessor extends StructureProcessor {
*///?}
    public static final PuzzleSeedProcessor INSTANCE = new PuzzleSeedProcessor();
    public static final MapCodec<PuzzleSeedProcessor> CODEC = MapCodec.unit(INSTANCE);

    private PuzzleSeedProcessor() { }

    /**
     * {@code anchor} is the placed piece's origin: the same for every block of one room, so a clue and its lock
     * agree on the answer. Per-block scrambles use the block's own final position instead.
     */
    static StructureTemplate.StructureBlockInfo seed(LevelReader level, BlockPos anchor,
            StructureTemplate.StructureBlockInfo current) {
        if (current == null) return null;
        long seed = level instanceof WorldGenLevel generating ? generating.getSeed() : 0L;
        BlockState state = current.state();
        BlockState result = state;
        CompoundTag nbt = current.nbt();
        if (state.getBlock() instanceof BallastPlateBlock) {
            result = state.setValue(BallastPlateBlock.TARGET, BallastPuzzle.target(seed, current.pos().asLong()));
        } else if (state.getBlock() instanceof GlyphClueBlock) {
            int[] sequence = PuzzleSeeds.glyphSequence(seed, anchor.asLong());
            result = state.setValue(GlyphClueBlock.GLYPH, sequence[state.getValue(GlyphClueBlock.INDEX)]);
        } else if (state.getBlock() instanceof GlyphLockBlock) {
            nbt = nbt == null ? new CompoundTag() : nbt.copy();
            nbt.putString("id", "neroruins:glyph_lock");
            ListTag sequence = new ListTag();
            for (int glyph : PuzzleSeeds.glyphSequence(seed, anchor.asLong())) sequence.add(IntTag.valueOf(glyph));
            nbt.put("sequence", sequence);
        } else if (state.getBlock() instanceof ConduitBlock) {
            Direction facing = state.getValue(ConduitBlock.FACING);
            int turns = PuzzleSeeds.conduitTurns(seed, current.pos().asLong(), state.getValue(ConduitBlock.CORNER));
            for (int turn = 0; turn < turns; turn++) facing = facing.getClockWise();
            result = state.setValue(ConduitBlock.FACING, facing).setValue(ConduitBlock.POWERED, false);
        } else if (state.getBlock() instanceof ResonancePylonBlock) {
            int target = PuzzleSeeds.chord(seed, anchor.asLong())[state.getValue(ResonancePylonBlock.INDEX)];
            result = state.setValue(ResonancePylonBlock.TARGET, target)
                    .setValue(ResonancePylonBlock.PITCH, PuzzleSeeds.startPitch(seed, current.pos().asLong(), target));
        } else if (state.getBlock() instanceof ResonanceClueBlock) {
            result = state.setValue(ResonanceClueBlock.TONE,
                    PuzzleSeeds.chord(seed, anchor.asLong())[state.getValue(ResonanceClueBlock.INDEX)]);
        } else {
            return current;
        }
        return new StructureTemplate.StructureBlockInfo(current.pos(), result, nbt);
    }

    //? if >=26.2 {
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos target, BlockPos reference,
            BlockPos templateRelative, StructureTemplate.StructureBlockInfo current, StructurePlaceSettings settings) {
        return seed(level, target, current);
    }

    @Override
    public MapCodec<PuzzleSeedProcessor> codec() { return CODEC; }
    //?} else {
    /*@Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos target, BlockPos reference,
            StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current,
            StructurePlaceSettings settings) {
        return seed(level, target, current);
    }

    @Override
    protected StructureProcessorType<?> getType() { return ModProcessors.PUZZLE_SEED.get(); }
    *///?}
}
