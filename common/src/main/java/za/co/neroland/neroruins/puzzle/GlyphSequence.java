package za.co.neroland.neroruins.puzzle;

import java.util.Arrays;

/** Rules for a {@code glyph_sequence} lock: an answer is accepted only when it matches exactly. */
public final class GlyphSequence {
    /** Ticks a lock refuses input after a wrong answer. */
    public static final int COOLDOWN_TICKS = 60;

    private GlyphSequence() { }

    public static boolean valid(int[] sequence) {
        if (sequence == null || sequence.length != PuzzleSeeds.GLYPH_LENGTH) return false;
        for (int glyph : sequence) if (glyph < 0 || glyph > 15) return false;
        return true;
    }

    public static boolean matches(int[] expected, int[] entered) {
        return valid(expected) && valid(entered) && Arrays.equals(expected, entered);
    }
}
