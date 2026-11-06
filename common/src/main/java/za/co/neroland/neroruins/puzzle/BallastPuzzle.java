package za.co.neroland.neroruins.puzzle;

import java.util.List;

/**
 * The weight-plate ("ballast") puzzle. Each plate shows a target between {@link #MIN} and {@link #MAX} as
 * lights on its face; the vault opens when every plate in the room carries exactly its target number of items
 * at the moment a player tries the vault. Targets come from the world seed and the plate's position, so they
 * differ between worlds and between instances but never change for a placed plate.
 */
public final class BallastPuzzle {
    public static final int MIN = 1;
    public static final int MAX = 8;
    public static final int MAX_PLATES = 8;

    public record Plate(int target, int load) { }

    private BallastPuzzle() { }

    public static int target(long worldSeed, long position) {
        long value = worldSeed ^ Long.rotateLeft(position, 17) ^ 0x42414C4C41535421L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return MIN + (int) Long.remainderUnsigned(value, MAX - MIN + 1);
    }

    /** True only for a non-empty room whose every plate carries exactly its target. */
    public static boolean solved(List<Plate> plates) {
        if (plates.isEmpty() || plates.size() > MAX_PLATES) return false;
        for (Plate plate : plates) {
            if (plate.target() < MIN || plate.target() > MAX || plate.load() != plate.target()) return false;
        }
        return true;
    }
}
