package za.co.neroland.neroruins.puzzle;

/**
 * Per-instance puzzle solutions. Every value comes from the world seed and a position (a placed piece's origin
 * for room-wide answers, a block's own position for per-block scrambles), so the same seed and placement always
 * give the same puzzle while other worlds and other instances differ.
 */
public final class PuzzleSeeds {
    /** Glyphs in a {@code glyph_sequence} answer; a room has one clue stone per position. */
    public static final int GLYPH_LENGTH = 4;
    /** Resonance pylons per room (pylon and clue indices run 0..PYLONS-1). */
    public static final int PYLONS = 4;
    /** Pitches a resonance pylon can be tuned to. */
    public static final int PITCHES = 8;

    private PuzzleSeeds() { }

    static long mix(long seed, long position, long salt) {
        long value = seed ^ Long.rotateLeft(position, 23) ^ salt;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static int pick(long value, int bound) { return (int) Long.remainderUnsigned(value, bound); }

    /** Four distinct glyphs (0..15) for the room placed at {@code anchor}. */
    public static int[] glyphSequence(long worldSeed, long anchor) {
        int[] result = new int[GLYPH_LENGTH];
        boolean[] used = new boolean[16];
        long value = mix(worldSeed, anchor, 0x474C595048533031L);
        for (int index = 0; index < GLYPH_LENGTH; index++) {
            value = mix(value, index, 0x53455155454E4345L);
            int glyph = pick(value, 16);
            while (used[glyph]) glyph = (glyph + 1) & 15;
            used[glyph] = true;
            result[index] = glyph;
        }
        return result;
    }

    /**
     * How many quarter turns to scramble a conduit away from its authored (solved) facing. A straight piece is
     * symmetric under a half turn, so it always gets an odd turn; a corner gets one to three turns.
     */
    public static int conduitTurns(long worldSeed, long position, boolean corner) {
        long value = mix(worldSeed, position, 0x434F4E4455495421L);
        return corner ? 1 + pick(value, 3) : (pick(value, 2) == 0 ? 1 : 3);
    }

    /** The target pitch (0..7) of each resonance pylon index for the room placed at {@code anchor}. */
    public static int[] chord(long worldSeed, long anchor) {
        int[] result = new int[PYLONS];
        long value = mix(worldSeed, anchor, 0x5245534F4E414E43L);
        for (int index = 0; index < PYLONS; index++) {
            value = mix(value, index, 0x43484F5244212121L);
            result[index] = pick(value, PITCHES);
        }
        return result;
    }

    /** A starting pitch that is never the target, so a freshly generated room is never already solved. */
    public static int startPitch(long worldSeed, long position, int target) {
        if (target < 0 || target >= PITCHES) throw new IllegalArgumentException("Invalid pitch");
        return (target + 1 + pick(mix(worldSeed, position, 0x5354415254505448L), PITCHES - 1)) % PITCHES;
    }
}
