package za.co.neroland.neroruins.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GalleryLayoutTest {
    private static List<GalleryLayout.Piece> pieces(int count) {
        List<GalleryLayout.Piece> pieces = new ArrayList<>();
        for (int i = 0; i < count; i++) pieces.add(new GalleryLayout.Piece("piece_" + i, 5 + i % 7, 4 + i % 6, 3 + i % 9));
        return pieces;
    }

    @Test
    void piecesNeverOverlapAndAllLieInsideTheClearedBox() {
        var plan = GalleryLayout.plan(7, 8, 18, pieces(40));
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
        assertFalse(box.contains(0, 1, 0), "the player's own spot is never cleared");
        assertFalse(box.contains(0, 0, 0));
    }

    @Test
    void rowsWrapAndTheLayoutIsRepeatable() {
        var first = GalleryLayout.plan(3, 8, 10, pieces(30));
        assertEquals(first, GalleryLayout.plan(3, 8, 10, pieces(30)), "build and clear compute the same footprint");
        assertTrue(first.templates().stream().map(GalleryLayout.Placement::z).distinct().count() > 1, "long rows wrap");
        for (var placement : first.templates()) {
            assertTrue(placement.x() + placement.piece().sizeX() <= GalleryLayout.START_X + GalleryLayout.ROW_WIDTH
                    || placement.x() == GalleryLayout.START_X);
        }
        var empty = GalleryLayout.plan(0, 0, 0, List.of());
        assertTrue(empty.templates().isEmpty());
        assertTrue(empty.bounds().maxZ() >= GalleryLayout.ITEM_WALL_Z + 1);
    }

    @Test
    void invalidInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GalleryLayout.Piece("x", 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new GalleryLayout.Piece("x", 1, 1, GalleryLayout.MAX_TEMPLATE_SIZE + 1));
        assertThrows(IllegalArgumentException.class, () -> new GalleryLayout.Piece("", 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> GalleryLayout.plan(-1, 0, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> GalleryLayout.plan(0, 0, 0, null));
    }
}
