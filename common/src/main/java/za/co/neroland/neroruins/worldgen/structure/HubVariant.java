package za.co.neroland.neroruins.worldgen.structure;

import za.co.neroland.neroruins.worldgen.placement.SatelliteLayout;

/**
 * Picks which of a complex's weighted hub layouts an instance uses. The choice depends only on the world
 * seed, the hub chunk and the family, so every player, restart and satellite sees the same layout for a
 * given hub, while different seeds or positions vary it.
 */
public final class HubVariant {
    private HubVariant() { }

    public static int choose(long worldSeed, int chunkX, int chunkZ, String family, int[] weights) {
        if (weights == null || weights.length == 0) throw new IllegalArgumentException("No hub layouts");
        long total = 0;
        for (int weight : weights) {
            if (weight < 1) throw new IllegalArgumentException("Hub layout weights must be positive");
            total += weight;
        }
        long roll = Long.remainderUnsigned(SatelliteLayout.instanceSeed(worldSeed, chunkX, chunkZ, family, 0x4855_4256L), total);
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) return i;
        }
        throw new IllegalStateException("Unreachable");
    }
}
