package za.co.neroland.neroruins.guardian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuardianRulesTest {
    @Test
    void spawnerBudgetsScaleWithTierAndClamp() {
        assertEquals(4, GuardianRules.initialBudget(1));
        assertEquals(6, GuardianRules.initialBudget(2));
        assertEquals(8, GuardianRules.initialBudget(3));
        assertEquals(4, GuardianRules.initialBudget(0));
        assertEquals(8, GuardianRules.initialBudget(9));
    }

    @Test
    void onlySurvivalPlayersInRangeWakeASpawner() {
        double inside = 7.9 * 7.9;
        assertTrue(GuardianRules.wakes(false, false, inside));
        assertFalse(GuardianRules.wakes(true, false, inside), "spectators do not wake it");
        assertFalse(GuardianRules.wakes(false, true, inside), "creative players do not wake it");
        assertFalse(GuardianRules.wakes(false, false, 8.1 * 8.1), "out of range");
    }

    @Test
    void spawningStopsWhenAnyConditionFails() {
        assertTrue(GuardianRules.shouldSpawn(3, false, true, 0));
        assertFalse(GuardianRules.shouldSpawn(0, false, true, 0), "budget spent");
        assertFalse(GuardianRules.shouldSpawn(3, true, true, 0), "peaceful");
        assertFalse(GuardianRules.shouldSpawn(3, false, false, 0), "nobody near");
        assertFalse(GuardianRules.shouldSpawn(3, false, true, GuardianRules.MAX_NEARBY), "zone already crowded");
    }

    @Test
    void sentinelShootsOnlyBetweenMeleeAndMaximumRange() {
        assertTrue(GuardianRules.inShotRange(8.0 * 8.0));
        assertTrue(GuardianRules.inShotRange(16.0 * 16.0));
        assertFalse(GuardianRules.inShotRange(3.0 * 3.0), "melee reach uses the claw");
        assertFalse(GuardianRules.inShotRange(16.5 * 16.5));
    }

    @Test
    void arcGrateHitsChargedSurvivalPlayersOnly() {
        assertTrue(GuardianRules.discharges(true, true, false, false));
        assertFalse(GuardianRules.discharges(false, true, false, false), "recharging");
        assertFalse(GuardianRules.discharges(true, false, false, false), "mobs pass over it");
        assertFalse(GuardianRules.discharges(true, true, true, false));
        assertFalse(GuardianRules.discharges(true, true, false, true));
    }

    @Test
    void sentinelMarksGrowStrongerAndCarryTheirTier() {
        Sentinel.Mark[] marks = Sentinel.Mark.values();
        assertEquals(3, marks.length);
        for (int i = 1; i < marks.length; i++) {
            assertTrue(marks[i].health() > marks[i - 1].health());
            assertTrue(marks[i].damage() > marks[i - 1].damage());
            assertTrue(marks[i].armor() > marks[i - 1].armor());
            assertTrue(marks[i].shotDamage() > marks[i - 1].shotDamage());
            assertEquals(i + 1, marks[i].tier());
        }
    }
}
