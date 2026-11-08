package za.co.neroland.neroruins.expedition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RelicPassivesTest {
    private static final List<String> RELICS = List.of("surveyors_compass", "pale_survey_lens", "directors_seal",
            "field_recorder", "meridian_keystone", "reach_thermal_core", "rootglass_vessel", "twilight_prism",
            "resonance_heart");

    @Test
    void everyChapterRelicHasItsOwnPassive() {
        var passives = EnumSet.noneOf(RelicPassives.Passive.class);
        for (String relic : RELICS) passives.add(RelicPassives.forRelic(relic).orElseThrow());
        assertEquals(EnumSet.allOf(RelicPassives.Passive.class), passives, "nine relics, nine distinct passives");
        assertEquals(Optional.of(RelicPassives.Passive.BEARING), RelicPassives.forRelic("surveyors_compass"));
    }

    @Test
    void nonRelicsGiveNothing() {
        assertTrue(RelicPassives.forRelic("data_shard").isEmpty());
        assertTrue(RelicPassives.forRelic("").isEmpty());
    }

    @Test
    void regrowthOnlyBelowHalfHealth() {
        assertTrue(RelicPassives.needsRegrowth(9, 20));
        assertFalse(RelicPassives.needsRegrowth(10, 20));
        assertFalse(RelicPassives.needsRegrowth(0, 20), "no healing once dead");
        assertFalse(RelicPassives.needsRegrowth(5, 0));
    }

    @Test
    void compassRecomputesOnlyWhenNeeded() {
        assertFalse(RelicPassives.compassStale(true, true, 1000, 1500));
        assertTrue(RelicPassives.compassStale(false, true, 1000, 1500));
        assertTrue(RelicPassives.compassStale(true, false, 1000, 1500), "a new world needs a new target");
        assertTrue(RelicPassives.compassStale(true, true, 1000, 2200));
        assertTrue(RelicPassives.compassStale(true, true, 5000, 10), "a reset tick counter forces a refresh");
    }
}
