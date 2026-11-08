package za.co.neroland.neroruins.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BenchStatsTest {
    @Test
    void summarisesTimesOverAttemptsAndSizesOverStarts() {
        var stats = new BenchStats(4);
        stats.attempt(2_000_000);
        stats.start(10, 40, 12, 30);
        stats.attempt(4_000_000);
        stats.attempt(6_000_000);
        stats.start(20, 90, 8, 60);
        stats.attempt(8_000_000);
        var result = stats.result();
        assertEquals(4, result.attempts());
        assertEquals(2, result.starts());
        assertEquals(5.0, result.meanMillis(), 1e-9);
        assertEquals(8.0, result.p95Millis(), 1e-9);
        assertEquals(8.0, result.maxMillis(), 1e-9);
        assertEquals(15.0, result.meanPieces(), 1e-9);
        assertEquals(20, result.maxPieces());
        assertEquals(90, result.maxSpanX());
        assertEquals(12, result.maxSpanY(), "spans are maximised independently");
        assertEquals(60, result.maxSpanZ());
    }

    @Test
    void attemptsWithoutStartsReportZeroPieces() {
        var stats = new BenchStats(2);
        stats.attempt(1_000_000);
        stats.attempt(3_000_000);
        var result = stats.result();
        assertEquals(0, result.starts());
        assertEquals(0.0, result.meanPieces(), 1e-9);
        assertEquals(0, result.maxPieces());
        assertEquals(2.0, result.meanMillis(), 1e-9);
    }

    @Test
    void nearestRankPercentileOnSmallSets() {
        assertEquals(7, BenchStats.percentile(new long[] { 7 }, 95), "one value is every percentile");
        assertEquals(3, BenchStats.percentile(new long[] { 1, 2, 3 }, 95));
        assertEquals(2, BenchStats.percentile(new long[] { 1, 2, 3 }, 50));
        long[] twenty = new long[20];
        for (int i = 0; i < 20; i++) twenty[i] = i + 1;
        assertEquals(19, BenchStats.percentile(twenty, 95));
        assertEquals(20, BenchStats.percentile(twenty, 100));
        assertThrows(IllegalArgumentException.class, () -> BenchStats.percentile(new long[0], 95));
        assertThrows(IllegalArgumentException.class, () -> BenchStats.percentile(new long[] { 1 }, 0));
    }

    @Test
    void emptyOrOverfilledRunsAreRejected() {
        assertThrows(IllegalStateException.class, () -> new BenchStats(3).result(), "no attempts, no summary");
        var stats = new BenchStats(1);
        assertThrows(IllegalStateException.class, () -> stats.start(1, 1, 1, 1), "a start needs its attempt first");
        stats.attempt(5);
        assertThrows(IllegalStateException.class, () -> stats.attempt(5), "more attempts than samples");
        assertThrows(IllegalArgumentException.class, () -> new BenchStats(2).attempt(-1));
    }

    @Test
    void sampleCountsOutsideOneTo256AreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BenchStats(0));
        assertThrows(IllegalArgumentException.class, () -> new BenchStats(BenchStats.MAX_SAMPLES + 1));
        assertThrows(IllegalArgumentException.class, () -> BenchStats.offset(0, 0));
        assertThrows(IllegalArgumentException.class, () -> BenchStats.offset(4, 4));
        new BenchStats(1);
        new BenchStats(BenchStats.MAX_SAMPLES);
    }

    @Test
    void trialsSitOnDistinctRepeatableChunksAroundTheCaller() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < BenchStats.MAX_SAMPLES; i++) {
            int[] offset = BenchStats.offset(i, BenchStats.MAX_SAMPLES);
            assertTrue(seen.add(offset[0] + "," + offset[1]), "trial " + i + " repeats a chunk");
            assertEquals(0, offset[0] % BenchStats.STRIDE_CHUNKS);
            assertTrue(Math.abs(offset[0]) <= 8 * BenchStats.STRIDE_CHUNKS && Math.abs(offset[1]) <= 8 * BenchStats.STRIDE_CHUNKS);
        }
        assertArrayEquals(BenchStats.offset(5, 32), BenchStats.offset(5, 32));
        assertArrayEquals(new int[] { 0, 0 }, BenchStats.offset(0, 1), "a single trial runs at the caller's chunk");
    }
}
