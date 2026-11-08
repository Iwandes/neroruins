package za.co.neroland.neroruins.expedition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BearingTest {
    @Test
    void namesTheEightCompassPointsWithNorthTowardsNegativeZ() {
        assertEquals("n", Bearing.point(0, -100));
        assertEquals("ne", Bearing.point(100, -100));
        assertEquals("e", Bearing.point(100, 0));
        assertEquals("se", Bearing.point(100, 100));
        assertEquals("s", Bearing.point(0, 100));
        assertEquals("sw", Bearing.point(-100, 100));
        assertEquals("w", Bearing.point(-100, 0));
        assertEquals("nw", Bearing.point(-100, -100));
        assertEquals("n", Bearing.point(10, -100), "a small sideways offset stays on the nearest point");
    }

    @Test
    void reportsHereAtZeroAndCoarseBandsOnly() {
        assertEquals("here", Bearing.point(0, 0));
        assertEquals("near", Bearing.band(0, 0));
        assertEquals("near", Bearing.band(255, 0));
        assertEquals("medium", Bearing.band(256, 0));
        assertEquals("medium", Bearing.band(0, -1023));
        assertEquals("far", Bearing.band(1024, 0));
        assertEquals("far", Bearing.band(Integer.MAX_VALUE, Integer.MIN_VALUE), "large offsets do not overflow");
    }
}
