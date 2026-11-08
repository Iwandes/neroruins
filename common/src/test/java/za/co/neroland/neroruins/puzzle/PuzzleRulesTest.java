package za.co.neroland.neroruins.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class PuzzleRulesTest {
    @Test
    void glyphLockAcceptsOnlyTheExactSequence() {
        int[] answer = { 3, 9, 0, 14 };
        assertTrue(GlyphSequence.matches(answer, new int[] { 3, 9, 0, 14 }));
        assertFalse(GlyphSequence.matches(answer, new int[] { 9, 3, 0, 14 }), "order matters");
        assertFalse(GlyphSequence.matches(answer, new int[] { 3, 9, 0 }), "a short entry is refused");
        assertFalse(GlyphSequence.matches(answer, new int[] { 3, 9, 0, 16 }), "an out-of-range glyph is refused");
        assertFalse(GlyphSequence.matches(new int[0], new int[0]), "an unconfigured lock never opens");
    }

    private static ConduitRouting.Grid grid(Map<BlockPos, ConduitRouting.Cell> cells) {
        return pos -> cells.getOrDefault(pos, new ConduitRouting.Empty());
    }

    @Test
    void powerFollowsConnectedPiecesToTheLockAndStopsAtAGap() {
        BlockPos source = BlockPos.ZERO;
        Map<BlockPos, ConduitRouting.Cell> cells = new HashMap<>();
        cells.put(new BlockPos(1, 0, 0), new ConduitRouting.Pipe(ConduitRouting.openings(Direction.EAST, false)));
        // corner from west to north: facing west, clockwise of west is north
        cells.put(new BlockPos(2, 0, 0), new ConduitRouting.Pipe(ConduitRouting.openings(Direction.WEST, true)));
        cells.put(new BlockPos(2, 0, -1), new ConduitRouting.Lock());
        var solved = ConduitRouting.trace(grid(cells), source, Direction.EAST);
        assertTrue(solved.solved());
        assertEquals(List.of(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)), solved.powered());

        cells.put(new BlockPos(2, 0, 0), new ConduitRouting.Pipe(ConduitRouting.openings(Direction.NORTH, true)));
        var broken = ConduitRouting.trace(grid(cells), source, Direction.EAST);
        assertFalse(broken.solved(), "a corner turned the wrong way breaks the path");
        assertEquals(List.of(new BlockPos(1, 0, 0)), broken.powered());
    }

    @Test
    void aPathThatEndsInTheOpenNeverReachesALock() {
        Map<BlockPos, ConduitRouting.Cell> cells = new HashMap<>();
        cells.put(new BlockPos(1, 0, 0), new ConduitRouting.Pipe(ConduitRouting.openings(Direction.WEST, true)));
        cells.put(new BlockPos(1, 0, -1), new ConduitRouting.Pipe(ConduitRouting.openings(Direction.SOUTH, true)));
        var trace = ConduitRouting.trace(grid(cells), BlockPos.ZERO, Direction.EAST);
        assertFalse(trace.solved());
        assertTrue(trace.powered().size() <= ConduitRouting.MAX_STEPS);
    }

    @Test
    void resonanceNeedsEveryPylonOnTarget() {
        assertTrue(ResonanceTuning.solved(List.of(new ResonanceTuning.Pylon(2, 2), new ResonanceTuning.Pylon(7, 7))));
        assertFalse(ResonanceTuning.solved(List.of(new ResonanceTuning.Pylon(2, 2), new ResonanceTuning.Pylon(6, 7))));
        assertFalse(ResonanceTuning.solved(List.of()), "a room without pylons is never solved");
        assertEquals(0, ResonanceTuning.next(PuzzleSeeds.PITCHES - 1));
        for (int pitch = 0; pitch < PuzzleSeeds.PITCHES; pitch++) {
            float sound = ResonanceTuning.soundPitch(pitch);
            assertTrue(sound >= 0.5F && sound <= 2.0F);
        }
    }

    @Test
    void keycardsOpenReadersOfTheirTierOrLower() {
        assertTrue(KeycardCompat.opens(OptionalInt.of(2), 1));
        assertTrue(KeycardCompat.opens(OptionalInt.of(2), 2));
        assertFalse(KeycardCompat.opens(OptionalInt.of(1), 3));
        assertFalse(KeycardCompat.opens(OptionalInt.empty(), 1), "an ordinary item opens nothing");
    }
}
