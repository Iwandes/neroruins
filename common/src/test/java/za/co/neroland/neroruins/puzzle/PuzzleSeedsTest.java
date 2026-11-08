package za.co.neroland.neroruins.puzzle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class PuzzleSeedsTest {
    @Test
    void glyphSequencesAreDistinctBoundedRepeatableAndVary() {
        var answers = new HashSet<String>();
        for (long anchor = 0; anchor < 200; anchor++) {
            int[] sequence = PuzzleSeeds.glyphSequence(42L, anchor * 977);
            assertArrayEquals(sequence, PuzzleSeeds.glyphSequence(42L, anchor * 977));
            assertTrue(GlyphSequence.valid(sequence));
            assertEquals(PuzzleSeeds.GLYPH_LENGTH, Arrays.stream(sequence).distinct().count(), "glyphs repeat");
            answers.add(Arrays.toString(sequence));
        }
        assertTrue(answers.size() > 150, "answers should differ between instances");
        assertNotEquals(Arrays.toString(PuzzleSeeds.glyphSequence(1L, 5L)), Arrays.toString(PuzzleSeeds.glyphSequence(2L, 5L)));
    }

    @Test
    void conduitScramblesNeverLeaveAPieceSolved() {
        for (long position = 0; position < 500; position++) {
            int straight = PuzzleSeeds.conduitTurns(7L, position, false);
            int corner = PuzzleSeeds.conduitTurns(7L, position, true);
            assertTrue(straight == 1 || straight == 3, "a half turn leaves a straight piece connected");
            assertTrue(corner >= 1 && corner <= 3);
        }
    }

    @Test
    void resonanceStartsOffTargetAndChordsAreBounded() {
        for (long anchor = 0; anchor < 300; anchor++) {
            int[] chord = PuzzleSeeds.chord(99L, anchor);
            assertEquals(PuzzleSeeds.PYLONS, chord.length);
            for (int target : chord) {
                assertTrue(target >= 0 && target < PuzzleSeeds.PITCHES);
                int start = PuzzleSeeds.startPitch(99L, anchor * 31, target);
                assertTrue(start >= 0 && start < PuzzleSeeds.PITCHES);
                assertNotEquals(target, start, "a fresh room must not already be solved");
            }
        }
        assertThrows(IllegalArgumentException.class, () -> PuzzleSeeds.startPitch(1L, 1L, PuzzleSeeds.PITCHES));
    }
}
