package za.co.neroland.neroruins.expedition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import za.co.neroland.neroruins.data.ExpeditionRecord;

class RuinLocatorTest {
    private static final String WORLD = "minecraft:overworld";
    private static final RuinLocator.Found HUB = new RuinLocator.Found("neroruins:bunker_earth", "bunker", 1000, -400);

    private static ExpeditionRecord.Discovery seen(String structure, String dimension, Optional<ExpeditionRecord.Hub> hub) {
        return new ExpeditionRecord.Discovery(structure, "bunker", "outpost", dimension, hub, 1);
    }

    @Test
    void ringsCoverEachCellOnceAndGrowByEight() {
        Set<List<Integer>> seen = new HashSet<>();
        for (int ring = 0; ring <= RuinLocator.MAX_RINGS; ring++) {
            var cells = RuinLocator.ringCells(ring);
            assertEquals(ring == 0 ? 1 : 8 * ring, cells.size());
            for (int[] cell : cells) {
                assertEquals(ring, Math.max(Math.abs(cell[0]), Math.abs(cell[1])));
                assertTrue(seen.add(List.of(cell[0], cell[1])), "no cell is visited twice");
            }
        }
        assertEquals((2 * RuinLocator.MAX_RINGS + 1) * (2 * RuinLocator.MAX_RINGS + 1), seen.size());
        assertThrows(IllegalArgumentException.class, () -> RuinLocator.ringCells(-1));
    }

    @Test
    void aHubIsExploredOnlyFromTheHoldersOwnMatchingDiscovery() {
        var near = Optional.of(new ExpeditionRecord.Hub(1050, 40, -380));
        assertTrue(RuinLocator.explored(List.of(seen(HUB.structure(), WORLD, near)), WORLD, HUB));
        assertTrue(RuinLocator.explored(List.of(seen(HUB.structure(), WORLD, Optional.empty())), WORLD, HUB),
                "without a kept position, the type in that world counts as explored");
    }

    @Test
    void otherSitesTypesAndWorldsDoNotCountAsExplored() {
        var far = Optional.of(new ExpeditionRecord.Hub(1000 + RuinLocator.SAME_SITE_BLOCKS + 1, 40, -400));
        assertFalse(RuinLocator.explored(List.of(), WORLD, HUB));
        assertFalse(RuinLocator.explored(List.of(seen(HUB.structure(), WORLD, far)), WORLD, HUB));
        assertFalse(RuinLocator.explored(List.of(seen("neroruins:bunker", WORLD, Optional.empty())), WORLD, HUB));
        assertFalse(RuinLocator.explored(List.of(seen(HUB.structure(), "nerospace:luna", Optional.empty())), WORLD, HUB));
    }
}
