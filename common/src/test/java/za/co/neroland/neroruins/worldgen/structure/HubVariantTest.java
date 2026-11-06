package za.co.neroland.neroruins.worldgen.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroruins.worldgen.placement.SatelliteLayout;

class HubVariantTest {
    private static final int[] WEIGHTS = { 3, 2 };

    @Test
    void choiceIsRepeatableForASeedAndPositionAndVariesAcrossThem() {
        var bySeed = new HashSet<Integer>();
        var byPosition = new HashSet<Integer>();
        int[] counts = new int[2];
        for (long seed = 0; seed < 2000; seed++) {
            int choice = HubVariant.choose(seed, 17, -40, "bunker", WEIGHTS);
            assertEquals(choice, HubVariant.choose(seed, 17, -40, "bunker", WEIGHTS));
            bySeed.add(choice);
            counts[choice]++;
            byPosition.add(HubVariant.choose(42L, (int) seed, 3, "bunker", WEIGHTS));
        }
        assertEquals(2, bySeed.size(), "different seeds reach every layout");
        assertEquals(2, byPosition.size(), "different hubs in one world reach every layout");
        // 3:2 weights over 2000 samples: expect about 1200 / 800.
        assertTrue(counts[0] > 1050 && counts[0] < 1350, "weights are honoured: " + counts[0]);
    }

    @Test
    void upperSeedBitsAndFamilyChangeTheChoice() {
        var choices = new HashSet<Integer>();
        for (long high = 0; high < 64; high++) choices.add(HubVariant.choose((high << 48) | 7L, 1, 1, "bunker", WEIGHTS));
        assertEquals(2, choices.size());
        var families = new HashSet<Long>();
        families.add(SatelliteLayout.instanceSeed(1, 2, 3, "an", 0));
        families.add(SatelliteLayout.instanceSeed(1, 2, 3, "c0", 0));
        assertEquals(2, families.size(), "families with colliding Java hashes still differ");
    }

    @Test
    void invalidWeightsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> HubVariant.choose(1, 0, 0, "bunker", new int[0]));
        assertThrows(IllegalArgumentException.class, () -> HubVariant.choose(1, 0, 0, "bunker", new int[] { 1, 0 }));
        assertThrows(IllegalArgumentException.class, () -> HubVariant.choose(1, 0, 0, "bad family", WEIGHTS));
        assertEquals(0, HubVariant.choose(1, 0, 0, "bunker", new int[] { 5 }));
    }
}
