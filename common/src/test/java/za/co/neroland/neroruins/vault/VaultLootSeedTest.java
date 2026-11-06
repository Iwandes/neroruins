package za.co.neroland.neroruins.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VaultLootSeedTest {
    @Test
    void retriesAreStableAndDifferentClaimsHaveDistinctInputs() {
        UUID player = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        long initial = VaultLootSeed.forClaim(42, 11, "minecraft:overworld", "neroruins:chests/a", player, null);
        assertEquals(initial, VaultLootSeed.forClaim(42, 11, "minecraft:overworld", "neroruins:chests/a", player, null));
        assertNotEquals(initial, VaultLootSeed.forClaim(42, 11, "minecraft:the_nether", "neroruins:chests/a", player, null));
        assertNotEquals(initial, VaultLootSeed.forClaim(42, 11, "minecraft:overworld", "neroruins:chests/a", player, 100L));
        assertNotEquals(initial, VaultLootSeed.forClaim(42 ^ (1L << 48), 11, "minecraft:overworld", "neroruins:chests/a", player, null));
        assertNotEquals(initial, VaultLootSeed.forClaim(42, 11, "minecraft:overworld", "neroruins:chests/a",
                UUID.fromString("12345678-1234-1234-1234-123456789abd"), null));
        assertNotEquals(0, initial);
    }
}
