package za.co.neroland.neroruins.worldgen.placement;

import java.util.Random;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public final class SatelliteLayout {
    public record Offset(int x, int z) { }
    public record Chunk(int x, int z) { }

    private SatelliteLayout() { }

    public static boolean validFamily(String family) {
        if (family == null || family.isEmpty() || family.length() > 64) return false;
        for (int i = 0; i < family.length(); i++) {
            char c = family.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9') && c != '_') return false;
        }
        return true;
    }

    public static Optional<Chunk> findHub(long seed, int spacing, int chunkX, int chunkZ, String family,
            int index, int minRadius, int maxRadius, BiFunction<Integer, Integer, Chunk> potentialHub,
            BiPredicate<Integer, Integer> eligibleHub) {
        if (spacing < 16) throw new IllegalArgumentException("Hub spacing must be at least 16 chunks");
        validate(family, index, minRadius, maxRadius);
        int minX = Math.floorDiv(chunkX - maxRadius, spacing);
        int maxX = Math.floorDiv(chunkX + maxRadius, spacing);
        int minZ = Math.floorDiv(chunkZ - maxRadius, spacing);
        int maxZ = Math.floorDiv(chunkZ + maxRadius, spacing);
        for (int regionX = minX; regionX <= maxX; regionX++) {
            for (int regionZ = minZ; regionZ <= maxZ; regionZ++) {
                Chunk hub = potentialHub.apply(regionX * spacing, regionZ * spacing);
                Offset offset = offset(seed, hub.x(), hub.z(), family, index, minRadius, maxRadius);
                if (hub.x() + offset.x() == chunkX && hub.z() + offset.z() == chunkZ
                        && eligibleHub.test(hub.x(), hub.z())) return Optional.of(hub);
            }
        }
        return Optional.empty();
    }

    public static Offset offset(long worldSeed, int hubX, int hubZ, String family, int index,
            int minRadius, int maxRadius) {
        validate(family, index, minRadius, maxRadius);
        int side = 2 * maxRadius + 1;
        int inner = 2 * minRadius - 1;
        int count = side * side - inner * inner;
        long seed = worldSeed ^ (hubX * 0x9E3779B97F4A7C15L)
                ^ (hubZ * 0xC2B2AE3D27D4EB4FL) ^ familySeed(family);
        Random random = new Random(mixSeed(seed));
        int[] selected = new int[index + 1];
        for (int current = 0; current <= index; current++) {
            int choice = random.nextInt(count);
            while (contains(selected, current, choice)) {
                choice = (choice + 1) % count;
            }
            selected[current] = choice;
        }
        int remaining = selected[index];
        int outerRows = (maxRadius - minRadius + 1) * side;
        if (remaining < outerRows) {
            return new Offset(remaining % side - maxRadius, remaining / side - maxRadius);
        }
        remaining -= outerRows;
        int stripWidth = maxRadius - minRadius + 1;
        int middleRowWidth = stripWidth * 2;
        int middleRows = inner * middleRowWidth;
        if (remaining < middleRows) {
            int column = remaining % middleRowWidth;
            int x = column < stripWidth ? column - maxRadius : column - stripWidth + minRadius;
            return new Offset(x, remaining / middleRowWidth - minRadius + 1);
        }
        remaining -= middleRows;
        return new Offset(remaining % side - maxRadius, remaining / side + minRadius);
    }

    /**
     * Whether satellite {@code index} exists for this hub. Each satellite slot is present with probability
     * {@code chancePercent}/100, decided from the world seed, hub chunk, family and index, so the number of
     * satellites varies between hubs and seeds but never between visits to the same hub.
     */
    public static boolean present(long worldSeed, int hubX, int hubZ, String family, int index, int chancePercent) {
        if (!validFamily(family) || index < 0 || index >= 32) throw new IllegalArgumentException("Invalid satellite");
        if (chancePercent < 1 || chancePercent > 100) throw new IllegalArgumentException("Invalid satellite chance");
        if (chancePercent == 100) return true;
        long roll = Long.remainderUnsigned(instanceSeed(worldSeed, hubX, hubZ, family, 0x5341_5400L + index), 100);
        return roll < chancePercent;
    }

    /** A well-mixed per-instance seed; {@code salt} separates independent decisions about the same hub. */
    public static long instanceSeed(long worldSeed, int chunkX, int chunkZ, String family, long salt) {
        if (!validFamily(family)) throw new IllegalArgumentException("Invalid family");
        long seed = worldSeed ^ (chunkX * 0x9E3779B97F4A7C15L) ^ (chunkZ * 0xC2B2AE3D27D4EB4FL)
                ^ familySeed(family) ^ mixSeed(salt);
        return mixSeed(seed);
    }

    private static void validate(String family, int index, int minRadius, int maxRadius) {
        if (!validFamily(family) || minRadius < 1 || maxRadius < minRadius
                || maxRadius > 32 || index < 0 || index >= 32) {
            throw new IllegalArgumentException("Invalid satellite layout parameters");
        }
        int side = 2 * maxRadius + 1;
        int inner = 2 * minRadius - 1;
        int count = side * side - inner * inner;
        if (index >= count) {
            throw new IllegalArgumentException("Not enough distinct satellite positions");
        }
    }

    private static boolean contains(int[] selected, int length, int value) {
        for (int i = 0; i < length; i++) {
            if (selected[i] == value) return true;
        }
        return false;
    }

    private static long familySeed(String family) {
        long value = 0xcbf29ce484222325L;
        for (int i = 0; i < family.length(); i++) {
            value = (value ^ family.charAt(i)) * 0x100000001b3L;
        }
        return value;
    }

    private static long mixSeed(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
