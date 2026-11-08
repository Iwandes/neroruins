package za.co.neroland.neroruins.expedition;

import net.minecraft.network.chat.Component;

/**
 * Turns an offset into a coarse compass point and distance band. Exact coordinates are never shown: a reading
 * is "north-east, medium range", which is enough to travel by and too coarse to pinpoint anything.
 */
public final class Bearing {
    public static final int NEAR = 256;
    public static final int MEDIUM = 1024;
    private static final String[] POINTS = { "n", "ne", "e", "se", "s", "sw", "w", "nw" };

    private Bearing() { }

    /** One of {@code n ne e se s sw w nw}, with north towards negative Z as in vanilla; {@code here} at zero. */
    public static String point(long dx, long dz) {
        if (dx == 0 && dz == 0) return "here";
        double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        int sector = (int) Math.floorMod(Math.round(degrees / 45.0), 8L);
        return POINTS[sector];
    }

    /** {@code near} under 256 blocks, {@code medium} under 1024, otherwise {@code far}. */
    public static String band(long dx, long dz) {
        double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
        if (distance < NEAR) return "near";
        return distance < MEDIUM ? "medium" : "far";
    }

    public static Component describe(long dx, long dz) {
        return Component.translatable("bearing.neroruins.reading",
                Component.translatable("bearing.neroruins." + point(dx, dz)),
                Component.translatable("bearing.neroruins.band." + band(dx, dz)));
    }
}
