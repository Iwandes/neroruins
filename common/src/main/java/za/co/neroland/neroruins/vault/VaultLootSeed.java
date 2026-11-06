package za.co.neroland.neroruins.vault;

import java.util.Objects;
import java.util.UUID;

public final class VaultLootSeed {
    private VaultLootSeed() { }

    public static long forClaim(long worldSeed, long position, String dimension, String lootTable, UUID player, Long previous) {
        long value = worldSeed ^ position ^ Objects.requireNonNull(player, "player").getMostSignificantBits()
                ^ Long.rotateLeft(player.getLeastSignificantBits(), 23)
                ^ (Objects.requireNonNull(dimension, "dimension").hashCode() * 0x9E3779B97F4A7C15L)
                ^ (Objects.requireNonNull(lootTable, "lootTable").hashCode() * 0xC2B2AE3D27D4EB4FL)
                ^ (previous == null ? 0 : previous);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value == 0 ? 1 : value;
    }
}
