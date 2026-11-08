package za.co.neroland.neroruins.puzzle;

import java.util.List;

/** Rules for a {@code resonance_tuning} room: it resonates when every pylon sounds its target pitch. */
public final class ResonanceTuning {
    public record Pylon(int pitch, int target) { }
    /** Semitone of each pitch step (a major scale over one octave), used for the note a pylon plays. */
    private static final int[] SEMITONES = { 0, 2, 4, 5, 7, 9, 11, 12 };

    private ResonanceTuning() { }

    public static boolean solved(List<Pylon> pylons) {
        if (pylons.isEmpty() || pylons.size() > PuzzleSeeds.PYLONS * 2) return false;
        for (Pylon pylon : pylons) {
            if (pylon.pitch() < 0 || pylon.pitch() >= PuzzleSeeds.PITCHES || pylon.pitch() != pylon.target()) return false;
        }
        return true;
    }

    public static int next(int pitch) { return (pitch + 1) % PuzzleSeeds.PITCHES; }

    /** Sound pitch multiplier for a pitch step, within the note block range of 0.5 to 2.0. */
    public static float soundPitch(int pitch) {
        return (float) Math.pow(2.0, (SEMITONES[pitch] - 6) / 12.0);
    }
}
