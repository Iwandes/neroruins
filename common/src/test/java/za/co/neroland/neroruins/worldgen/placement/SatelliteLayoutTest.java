package za.co.neroland.neroruins.worldgen.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SatelliteLayoutTest {
    @Test
    void offsetsAreStableDistinctAndBounded() {
        Set<SatelliteLayout.Offset> offsets = new HashSet<>();
        for (int index = 0; index < 20; index++) {
            var offset = SatelliteLayout.offset(1234L, -317, 99, "lattice_temple", index, 3, 12);
            assertEquals(offset, SatelliteLayout.offset(1234L, -317, 99, "lattice_temple", index, 3, 12));
            assertTrue(offsets.add(offset));
            int radius = Math.max(Math.abs(offset.x()), Math.abs(offset.z()));
            assertTrue(radius >= 3 && radius <= 12);
        }
        assertNotEquals(offsets, offsetsForSeed(9876L));
    }

    @Test
    void highWorldSeedBitsAffectSatelliteLayouts() {
        Set<SatelliteLayout.Offset> choices = new HashSet<>();
        for (long highBits = 0; highBits < 64; highBits++) {
            long seed = (highBits << 48) | 1234L;
            var first = SatelliteLayout.offset(seed, -317, 99, "lattice_temple", 0, 3, 12);
            assertEquals(first, SatelliteLayout.offset(seed, -317, 99, "lattice_temple", 0, 3, 12));
            choices.add(first);
        }
        assertTrue(choices.size() > 16, "Upper world-seed bits should influence the layout");
    }

    @Test
    void familiesWithCollidingJavaHashesDoNotShareTheSameLayout() {
        assertEquals("an".hashCode(), "c0".hashCode());
        var first = new java.util.ArrayList<SatelliteLayout.Offset>();
        var second = new java.util.ArrayList<SatelliteLayout.Offset>();
        for (int index = 0; index < 12; index++) {
            first.add(SatelliteLayout.offset(1234L, -317, 99, "an", index, 3, 12));
            second.add(SatelliteLayout.offset(1234L, -317, 99, "c0", index, 3, 12));
        }
        assertNotEquals(first, second);
        assertEquals(first.get(0), SatelliteLayout.offset(1234L, -317, 99, "an", 0, 3, 12));
    }

    @Test
    void invalidOrOverfullRingsFail() {
        assertThrows(IllegalArgumentException.class,
                () -> SatelliteLayout.offset(1, 0, 0, "", 0, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> SatelliteLayout.offset(1, 0, 0, "bad family", 0, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> SatelliteLayout.offset(1, 0, 0, "bunker", 8, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> SatelliteLayout.offset(1, 0, 0, "bunker", 0, 4, 3));
    }

    @Test
    void findsCandidateHubAndRejectsIneligibleHub() {
        long seed = 928374L;
        var hub = new SatelliteLayout.Chunk(-62, 99);
        var offset = SatelliteLayout.offset(seed, hub.x(), hub.z(), "probe", 0, 2, 4);
        int x = hub.x() + offset.x();
        int z = hub.z() + offset.z();
        java.util.function.BiFunction<Integer, Integer, SatelliteLayout.Chunk> source =
                (regionX, regionZ) -> new SatelliteLayout.Chunk(regionX + 2, regionZ + 3);
        assertEquals(hub, SatelliteLayout.findHub(seed, 32, x, z, "probe", 0, 2, 4,
                source, (hx, hz) -> hx == hub.x() && hz == hub.z()).orElseThrow());
        assertTrue(SatelliteLayout.findHub(seed, 32, x, z, "probe", 0, 2, 4,
                source, (hx, hz) -> false).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> SatelliteLayout.findHub(seed, 15, x, z, "probe", 0, 2, 4, source, (hx, hz) -> true));
        assertThrows(IllegalArgumentException.class,
                () -> SatelliteLayout.findHub(seed, 32, x, z, "probe", 0, 1, Integer.MAX_VALUE,
                        source, (hx, hz) -> true));
    }

    @Test
    void candidateSearchHasABoundedNumberOfRegions() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        SatelliteLayout.findHub(1L, 16, -101, 201, "probe", 0, 1, 32,
                (x, z) -> {
                    calls.incrementAndGet();
                    return new SatelliteLayout.Chunk(x, z);
                }, (x, z) -> false);
        assertTrue(calls.get() <= 25);
    }

    @Test
    void satellitesRequireTheMatchingEligibleAnchorAcrossSeeds() {
        java.util.function.BiFunction<Integer, Integer, SatelliteLayout.Chunk> source =
                (regionX, regionZ) -> new SatelliteLayout.Chunk(regionX + 3, regionZ + 5);
        for (long seed = -20; seed <= 20; seed++) {
            var anchor = new SatelliteLayout.Chunk(-61, 101);
            var offset = SatelliteLayout.offset(seed, anchor.x(), anchor.z(), "probe", 0, 2, 8);
            int satelliteX = anchor.x() + offset.x();
            int satelliteZ = anchor.z() + offset.z();
            assertTrue(SatelliteLayout.findHub(seed, 32, satelliteX, satelliteZ, "probe", 0, 2, 8,
                    source, (x, z) -> x == anchor.x() && z == anchor.z()).isPresent());
            assertTrue(SatelliteLayout.findHub(seed, 32, satelliteX, satelliteZ, "probe", 0, 2, 8,
                    source, (x, z) -> false).isEmpty());
        }
    }

    private static Set<SatelliteLayout.Offset> offsetsForSeed(long seed) {
        Set<SatelliteLayout.Offset> offsets = new HashSet<>();
        for (int index = 0; index < 20; index++) {
            offsets.add(SatelliteLayout.offset(seed, -317, 99, "lattice_temple", index, 3, 12));
        }
        return offsets;
    }

    @Test
    void satellitePresenceIsSeededPerHubAndSlot() {
        int present = 0;
        var outcomes = new HashSet<Boolean>();
        for (int hub = 0; hub < 1000; hub++) {
            boolean first = SatelliteLayout.present(77L, hub, -hub, "bunker", 0, 50);
            assertEquals(first, SatelliteLayout.present(77L, hub, -hub, "bunker", 0, 50));
            if (first) present++;
            outcomes.add(first);
        }
        assertEquals(2, outcomes.size());
        assertTrue(present > 400 && present < 600, "about half the hubs keep a 50% slot: " + present);
        int differentSlots = 0;
        for (int hub = 0; hub < 200; hub++) {
            if (SatelliteLayout.present(77L, hub, 0, "bunker", 0, 50) != SatelliteLayout.present(77L, hub, 0, "bunker", 1, 50)) {
                differentSlots++;
            }
        }
        assertTrue(differentSlots > 50, "slots are decided independently");
        assertTrue(SatelliteLayout.present(1, 0, 0, "bunker", 0, 100));
        assertThrows(IllegalArgumentException.class, () -> SatelliteLayout.present(1, 0, 0, "bunker", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> SatelliteLayout.present(1, 0, 0, "bunker", 32, 50));
        assertThrows(IllegalArgumentException.class, () -> SatelliteLayout.present(1, 0, 0, "Bad", 0, 50));
    }
}
