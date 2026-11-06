package za.co.neroland.neroruins.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class BallastPuzzleTest {
    @Test
    void targetsAreBoundedRepeatableAndVaryWithSeedAndPosition() {
        var bySeed = new HashSet<Integer>();
        var byPosition = new HashSet<Integer>();
        for (long i = 0; i < 500; i++) {
            int target = BallastPuzzle.target(i, 12345L);
            assertTrue(target >= BallastPuzzle.MIN && target <= BallastPuzzle.MAX);
            assertEquals(target, BallastPuzzle.target(i, 12345L));
            bySeed.add(target);
            byPosition.add(BallastPuzzle.target(99L, i * 4097));
        }
        assertEquals(BallastPuzzle.MAX - BallastPuzzle.MIN + 1, bySeed.size(), "every target occurs across seeds");
        assertEquals(BallastPuzzle.MAX - BallastPuzzle.MIN + 1, byPosition.size(), "and across plates in one world");
        var highBits = new HashSet<Integer>();
        for (long high = 0; high < 64; high++) highBits.add(BallastPuzzle.target((high << 48) | 5L, 777L));
        assertTrue(highBits.size() > 4, "upper seed bits matter");
    }

    @Test
    void solvedOnlyWhenEveryPlateCarriesExactlyItsTarget() {
        var plates = List.of(new BallastPuzzle.Plate(3, 3), new BallastPuzzle.Plate(5, 5), new BallastPuzzle.Plate(1, 1));
        assertTrue(BallastPuzzle.solved(plates));
        assertFalse(BallastPuzzle.solved(List.of(new BallastPuzzle.Plate(3, 3), new BallastPuzzle.Plate(5, 6))),
                "overloading a plate does not count");
        assertFalse(BallastPuzzle.solved(List.of(new BallastPuzzle.Plate(3, 3), new BallastPuzzle.Plate(5, 4))));
        assertFalse(BallastPuzzle.solved(List.of()), "a cache without plates is never solved by this check");
        assertFalse(BallastPuzzle.solved(List.of(new BallastPuzzle.Plate(0, 0))), "corrupt targets never solve");
        var tooMany = new ArrayList<BallastPuzzle.Plate>();
        for (int i = 0; i <= BallastPuzzle.MAX_PLATES; i++) tooMany.add(new BallastPuzzle.Plate(1, 1));
        assertFalse(BallastPuzzle.solved(tooMany), "planting extra plates cannot change the rules");
    }

    @Test
    void bunkerPiecesRunThePuzzleSeedProcessor() throws Exception {
        try (var input = getClass().getResourceAsStream("/data/neroruins/worldgen/processor_list/bunker_weathering.json")) {
            var processors = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("processors");
            boolean found = false;
            for (var processor : processors) {
                found |= "neroruins:puzzle_seed".equals(processor.getAsJsonObject().get("processor_type").getAsString());
            }
            assertTrue(found);
        }
    }
}
