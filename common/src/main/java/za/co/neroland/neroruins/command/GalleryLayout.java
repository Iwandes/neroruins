package za.co.neroland.neroruins.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Where {@code /neroruins gallery} puts things, relative to the player who runs it. Pure arithmetic so the
 * build and {@code clear} always agree on the footprint and it can be tested without a world. Everything
 * lies east and south of the player (x and z from 2), so the player's own spot is never inside the box.
 */
public final class GalleryLayout {
    public static final int START_X = 4;
    public static final int BLOCK_ROW_Z = 4;
    public static final int PLATE_ROW_Z = 8;
    public static final int GLYPH_ROW_Z = 12;
    public static final int GLYPH_SPACING = 2;
    public static final int ITEM_WALL_Z = 16;
    public static final int TEMPLATE_START_Z = 21;
    public static final int BLOCK_SPACING = 3;
    public static final int PLATE_SPACING = 4;
    public static final int FRAME_ROWS = 3;
    /** The Sentinel marks stand on the plate row, after the eighth plate. */
    public static final int SENTINEL_START_X = START_X + 8 * PLATE_SPACING + 2;
    public static final int SENTINELS = 3;
    public static final int ROW_WIDTH = 96;
    public static final int GAP = 4;
    public static final int MAX_TEMPLATE_SIZE = 64;

    public record Piece(String name, int sizeX, int sizeY, int sizeZ) {
        public Piece {
            if (name == null || name.isEmpty() || sizeX < 1 || sizeY < 1 || sizeZ < 1
                    || sizeX > MAX_TEMPLATE_SIZE || sizeY > MAX_TEMPLATE_SIZE || sizeZ > MAX_TEMPLATE_SIZE) {
                throw new IllegalArgumentException("Invalid gallery piece");
            }
        }
    }

    /** A piece's minimum corner, relative to the origin; the floor is at y 0 and pieces stand on y 1. */
    public record Placement(Piece piece, int x, int y, int z) { }

    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }

    public record Plan(int blocks, int plates, int glyphs, int items, int frameColumns, List<Placement> templates,
            Box bounds) { }

    private GalleryLayout() { }

    public static Plan plan(int blocks, int plates, int glyphs, int items, List<Piece> pieces) {
        if (blocks < 0 || plates < 0 || glyphs < 0 || items < 0 || pieces == null) {
            throw new IllegalArgumentException("Invalid gallery");
        }
        int frameColumns = Math.max(1, (items + FRAME_ROWS - 1) / FRAME_ROWS);
        int maxX = Math.max(START_X + Math.max(0, blocks - 1) * BLOCK_SPACING,
                Math.max(START_X + Math.max(0, plates - 1) * PLATE_SPACING,
                        Math.max(START_X + Math.max(0, glyphs - 1) * GLYPH_SPACING, START_X + frameColumns - 1)));
        maxX = Math.max(maxX, SENTINEL_START_X + (SENTINELS - 1) * PLATE_SPACING);
        int maxY = 5;
        List<Placement> placements = new ArrayList<>();
        int x = START_X;
        int z = TEMPLATE_START_Z;
        int rowDepth = 0;
        for (Piece piece : pieces) {
            if (x > START_X && x + piece.sizeX() > START_X + ROW_WIDTH) {
                x = START_X;
                z += rowDepth + GAP;
                rowDepth = 0;
            }
            placements.add(new Placement(piece, x, 1, z));
            maxX = Math.max(maxX, x + piece.sizeX() - 1);
            maxY = Math.max(maxY, piece.sizeY() + 2);
            rowDepth = Math.max(rowDepth, piece.sizeZ());
            x += piece.sizeX() + GAP;
        }
        int maxZ = placements.isEmpty() ? ITEM_WALL_Z + 2 : z + rowDepth;
        return new Plan(blocks, plates, glyphs, items, frameColumns, List.copyOf(placements),
                new Box(START_X - 2, 0, BLOCK_ROW_Z - 2, maxX + 2, maxY, maxZ + 2));
    }
}
