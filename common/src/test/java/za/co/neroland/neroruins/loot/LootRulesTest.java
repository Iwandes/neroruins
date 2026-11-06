package za.co.neroland.neroruins.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class LootRulesTest {
    private static final float TIER_FOUR_BASE = 0.25F;
    private static final int ROLLS = 10_000;

    @Test
    void acceptsLoaderModIds() {
        for (String id : new String[] { "nerotech", "neroland_core", "some-mod", "ab" }) {
            assertTrue(LootRules.isValidModId(id), id);
        }
    }

    @Test
    void rejectsMalformedModIds() {
        for (String id : new String[] { null, "", "a", "Nerotech", "1mod", "nero tech", "nerotech:x",
                "a".repeat(65) }) {
            assertFalse(LootRules.isValidModId(id), String.valueOf(id));
        }
    }

    @Test
    void leverScalesAndClampsTheRelicChance() {
        assertEquals(0.25F, LootRules.relicChance(0.25F, 1.0D), 1.0E-6F);
        assertEquals(0.0625F, LootRules.relicChance(0.25F, 0.25D), 1.0E-6F);
        assertEquals(1.0F, LootRules.relicChance(0.25F, 4.0D), 1.0E-6F);
        assertEquals(1.0F, LootRules.relicChance(0.9F, 4.0D), 1.0E-6F);
    }

    @Test
    void invalidLeverNeverGuaranteesARelic() {
        assertEquals(0.0F, LootRules.relicChance(0.25F, Double.NaN));
        assertEquals(0.0F, LootRules.relicChance(0.25F, Double.POSITIVE_INFINITY));
        assertEquals(0.0F, LootRules.relicChance(0.25F, -1.0D));
        assertEquals(0.0F, LootRules.relicChance(Float.NaN, 1.0D));
    }

    @Test
    void tenThousandRollsMatchTheLever() {
        for (double lever : new double[] { 0.25D, 1.0D, 2.0D }) {
            float chance = LootRules.relicChance(TIER_FOUR_BASE, lever);
            Random random = new Random(0x5EED_0000L + (long) (lever * 100));
            int hits = 0;
            for (int i = 0; i < ROLLS; i++) {
                if (random.nextFloat() < chance) {
                    hits++;
                }
            }
            double rate = hits / (double) ROLLS;
            assertEquals(TIER_FOUR_BASE * lever, rate, 0.02D, "lever " + lever);
        }
    }
}
