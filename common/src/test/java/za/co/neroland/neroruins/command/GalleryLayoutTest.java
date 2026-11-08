package za.co.neroland.neroruins.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GalleryLayoutTest {
    private static List<GalleryLayout.Piece> pieces(int count) {
        List<GalleryLayout.Piece> pieces = new ArrayList<>();
        for (int i = 0; i < count; i++) pieces.add(new GalleryLayout.Piece("piece_" + i, 5 + i % 7, 4 + i % 6, 3 + i % 9));
        return pieces;
    }

    @Test
    void piecesNeverOverlapAndAllLieInsideTheClearedBox() {
        var plan = GalleryLayout.plan(7, 8, 16, 18, pieces(40));
        var box = plan.bounds();
        var placed = plan.templates();
        assertEquals(40, placed.size());
        for (int i = 0; i < placed.size(); i++) {
            var a = placed.get(i);
            assertTrue(box.contains(a.x(), a.y(), a.z()));
            assertTrue(box.contains(a.x() + a.piece().sizeX() - 1, a.y() + a.piece().sizeY() - 1,
                    a.z() + a.piece().sizeZ() - 1), "the clear box covers the whole piece");
            assertTrue(a.z() >= GalleryLayout.TEMPLATE_START_Z, "pieces never reach the item wall");
            for (int j = i + 1; j < placed.size(); j++) {
                var b = placed.get(j);
                boolean apart = a.x() + a.piece().sizeX() <= b.x() || b.x() + b.piece().sizeX() <= a.x()
                        || a.z() + a.piece().sizeZ() <= b.z() || b.z() + b.piece().sizeZ() <= a.z();
                assertTrue(apart, a.piece().name() + " overlaps " + b.piece().name());
            }
        }
        assertTrue(box.contains(GalleryLayout.START_X + 6 * GalleryLayout.BLOCK_SPACING, 3, GalleryLayout.BLOCK_ROW_Z));
        assertTrue(box.contains(GalleryLayout.START_X + 7 * GalleryLayout.PLATE_SPACING, 1, GalleryLayout.PLATE_ROW_Z));
        assertTrue(box.contains(GalleryLayout.START_X + plan.frameColumns() - 1, 3, GalleryLayout.ITEM_WALL_Z + 1));
        assertTrue(box.contains(GalleryLayout.START_X + 15 * GalleryLayout.GLYPH_SPACING, 2, GalleryLayout.GLYPH_ROW_Z));
        assertTrue(GalleryLayout.PLATE_ROW_Z < GalleryLayout.GLYPH_ROW_Z && GalleryLayout.GLYPH_ROW_Z < GalleryLayout.ITEM_WALL_Z);
        assertFalse(box.contains(0, 1, 0), "the player's own spot is never cleared");
        assertFalse(box.contains(0, 0, 0));
    }

    @Test
    void rowsWrapAndTheLayoutIsRepeatable() {
        var first = GalleryLayout.plan(3, 8, 16, 10, pieces(30));
        assertEquals(first, GalleryLayout.plan(3, 8, 16, 10, pieces(30)), "build and clear compute the same footprint");
        assertTrue(first.templates().stream().map(GalleryLayout.Placement::z).distinct().count() > 1, "long rows wrap");
        for (var placement : first.templates()) {
            assertTrue(placement.x() + placement.piece().sizeX() <= GalleryLayout.START_X + GalleryLayout.ROW_WIDTH
                    || placement.x() == GalleryLayout.START_X);
        }
        var empty = GalleryLayout.plan(0, 0, 0, 0, List.of());
        assertTrue(empty.templates().isEmpty());
        assertTrue(empty.bounds().maxZ() >= GalleryLayout.ITEM_WALL_Z + 1);
        int lastSentinel = GalleryLayout.SENTINEL_START_X + (GalleryLayout.SENTINELS - 1) * GalleryLayout.PLATE_SPACING;
        assertTrue(empty.bounds().contains(lastSentinel, 1, GalleryLayout.PLATE_ROW_Z),
                "clearing the gallery must reach the Sentinels");
        assertTrue(GalleryLayout.SENTINEL_START_X > GalleryLayout.START_X + 7 * GalleryLayout.PLATE_SPACING,
                "Sentinels must not stand on a plate");
    }

    @Test
    void templatesBelongToTheLongestMatchingFamily() {
        assertEquals(Optional.of("bunker"), GalleryLayout.familyOf("bunker_hub_a"));
        assertEquals(Optional.of("bunker"), GalleryLayout.familyOf("bunker"));
        assertEquals(Optional.of("lattice_temple"), GalleryLayout.familyOf("lattice_temple_citadel_hub"));
        assertEquals(Optional.of("puzzle"), GalleryLayout.familyOf("puzzle_glyph"));
        assertEquals(Optional.empty(), GalleryLayout.familyOf("bunkerish_room"), "a prefix must end at an underscore");
        assertEquals(Optional.empty(), GalleryLayout.familyOf("probe_hub"));
        assertEquals(Optional.empty(), GalleryLayout.familyOf(null));
        assertEquals(List.of("bunker", "halcyon_ark", "puzzle"),
                GalleryLayout.familiesPresent(List.of("puzzle_relic", "bunker_hub_a", "halcyon_ark_bridge", "probe")));
    }

    @Test
    void theDefaultShowsBunkerAndPuzzlesAndAFamilyShowsOnlyItself() {
        List<String> paths = List.of("bunker_hub_a", "puzzle_glyph", "lattice_temple_hall_a", "halcyon_ark_bridge_a",
                "probe_hub");
        assertEquals(List.of("bunker_hub_a", "puzzle_glyph"),
                paths.stream().filter(path -> GalleryLayout.shows(path, Optional.empty())).toList());
        assertEquals(List.of("lattice_temple_hall_a"),
                paths.stream().filter(path -> GalleryLayout.shows(path, Optional.of("lattice_temple"))).toList());
        assertTrue(paths.stream().noneMatch(path -> GalleryLayout.shows(path, Optional.of("crash_site"))));
    }

    @Test
    void clearingWithTheSameFamilyCoversTheBuildAndTheDefaultDoesNot() {
        List<GalleryLayout.Piece> all = List.of(new GalleryLayout.Piece("bunker_hub_a", 9, 14, 9),
                new GalleryLayout.Piece("puzzle_glyph", 11, 6, 13),
                new GalleryLayout.Piece("lattice_temple_citadel_hub", 48, 30, 48),
                new GalleryLayout.Piece("lattice_temple_hall_a", 40, 20, 40),
                new GalleryLayout.Piece("lattice_temple_hall_b", 40, 20, 40));
        var built = plan(all, Optional.of("lattice_temple"));
        assertEquals(built, plan(all, Optional.of("lattice_temple")), "build and clear plan the same footprint");
        var defaultBox = plan(all, Optional.empty()).bounds();
        boolean defaultCoversFamily = built.templates().stream().allMatch(placed -> defaultBox.contains(
                placed.x() + placed.piece().sizeX() - 1, placed.y() + placed.piece().sizeY() - 1,
                placed.z() + placed.piece().sizeZ() - 1));
        assertFalse(defaultCoversFamily, "a family gallery must be cleared with its family named");
    }

    private static GalleryLayout.Plan plan(List<GalleryLayout.Piece> pieces, Optional<String> family) {
        return GalleryLayout.plan(21, 8, 16, 39,
                pieces.stream().filter(piece -> GalleryLayout.shows(piece.name(), family)).toList());
    }

    @Test
    void invalidInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GalleryLayout.Piece("x", 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new GalleryLayout.Piece("x", 1, 1, GalleryLayout.MAX_TEMPLATE_SIZE + 1));
        assertThrows(IllegalArgumentException.class, () -> new GalleryLayout.Piece("", 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> GalleryLayout.plan(-1, 0, 0, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> GalleryLayout.plan(0, 0, 0, 0, null));
    }
}
