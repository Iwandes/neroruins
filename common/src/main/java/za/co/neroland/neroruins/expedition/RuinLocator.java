package za.co.neroland.neroruins.expedition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import za.co.neroland.neroruins.data.ExpeditionRecord;
import za.co.neroland.neroruins.worldgen.structure.ComplexHub;

/**
 * Finds the nearest NeroRuins complex hub for the Survey Scanner and the Surveyor's Compass.
 *
 * <p>Cost is bounded: candidate cells are walked ring by ring around the player for at most {@link #MAX_RINGS}
 * rings of each hub structure set's spacing grid, the cheap placement check runs first, and at most
 * {@link #MAX_START_LOADS} candidate chunks per query are brought to structure-start status to confirm a hub
 * really generated there (that step runs the hub's ground check). When the budget runs out the nearest hub
 * found so far is returned, or nothing. The search reads only world generation, never any player's data; the
 * caller supplies the filter that skips hubs the asking player has already entered.
 */
public final class RuinLocator {
    public static final int MAX_RINGS = 6;
    public static final int MAX_START_LOADS = 16;
    /** Two hubs closer than this are the same site when matching a discovery's stored position. */
    public static final int SAME_SITE_BLOCKS = 96;

    public record Found(String structure, String family, int x, int z) { }

    private RuinLocator() { }

    public static Optional<Found> nearest(ServerLevel level, BlockPos origin, Predicate<Found> skip) {
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        StructureManager manager = level.structureManager();
        long seed = state.getLevelSeed();
        int originX = origin.getX() >> 4;
        int originZ = origin.getZ() >> 4;
        int[] budget = { MAX_START_LOADS };
        Found best = null;
        long bestDistance = Long.MAX_VALUE;
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            if (!(set.value().placement() instanceof RandomSpreadStructurePlacement spread)) continue;
            List<ComplexHub> hubs = new ArrayList<>();
            for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                if (entry.structure().value() instanceof ComplexHub hub) hubs.add(hub);
            }
            if (hubs.isEmpty()) continue;
            int spacing = spread.spacing();
            for (int ring = 0; ring <= MAX_RINGS && budget[0] > 0; ring++) {
                boolean foundInRing = false;
                for (int[] cell : ringCells(ring)) {
                    ChunkPos candidate = spread.getPotentialStructureChunk(seed,
                            originX + cell[0] * spacing, originZ + cell[1] * spacing);
                    int chunkX = candidate.getMinBlockX() >> 4;
                    int chunkZ = candidate.getMinBlockZ() >> 4;
                    if (!spread.isStructureChunk(state, chunkX, chunkZ)) continue;
                    for (ComplexHub hub : hubs) {
                        Optional<Found> found = confirm(level, manager, candidate, hub, spread, budget);
                        if (found.isEmpty() || skip.test(found.get())) continue;
                        long dx = found.get().x() - origin.getX();
                        long dz = found.get().z() - origin.getZ();
                        long distance = dx * dx + dz * dz;
                        foundInRing = true;
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = found.get();
                        }
                    }
                }
                if (foundInRing) break;
            }
        }
        return Optional.ofNullable(best);
    }

    private static Optional<Found> confirm(ServerLevel level, StructureManager manager, ChunkPos candidate,
            ComplexHub hub, RandomSpreadStructurePlacement spread, int[] budget) {
        StructureCheckResult presence = manager.checkStructurePresence(candidate, hub, spread, false);
        if (presence == StructureCheckResult.START_NOT_PRESENT || budget[0] <= 0) return Optional.empty();
        budget[0]--;
        ChunkAccess chunk = level.getChunk(candidate.getMinBlockX() >> 4, candidate.getMinBlockZ() >> 4,
                ChunkStatus.STRUCTURE_STARTS);
        StructureStart start = chunk.getStartForStructure(hub);
        if (start == null || !start.isValid()) return Optional.empty();
        Identifier id = structureId(level, hub);
        if (id == null) return Optional.empty();
        BlockPos centre = start.getBoundingBox().getCenter();
        return Optional.of(new Found(id.toString(), hub.family(), centre.getX(), centre.getZ()));
    }

    static Identifier structureId(ServerLevel level, ComplexHub hub) {
        //? if >=1.21.2 {
        return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(hub);
        //?} else {
        /*return level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(hub);
        *///?}
    }

    /** The cells on the square ring {@code ring} steps from the centre: one cell at ring 0, {@code 8 * ring} after. */
    public static List<int[]> ringCells(int ring) {
        if (ring < 0) throw new IllegalArgumentException("Negative ring");
        List<int[]> cells = new ArrayList<>();
        if (ring == 0) {
            cells.add(new int[] { 0, 0 });
            return cells;
        }
        for (int dx = -ring; dx <= ring; dx++) {
            for (int dz = -ring; dz <= ring; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) cells.add(new int[] { dx, dz });
            }
        }
        return cells;
    }

    /**
     * Whether the player has already entered this hub, judged only from that player's own record. A discovery
     * with a stored position matches a hub within {@link #SAME_SITE_BLOCKS}; one whose position was not kept
     * matches every hub of that structure type in that world.
     */
    public static boolean explored(List<ExpeditionRecord.Discovery> own, String dimension, Found hub) {
        for (ExpeditionRecord.Discovery discovery : own) {
            if (!discovery.dimension().equals(dimension) || !discovery.structure().equals(hub.structure())) continue;
            if (discovery.hub().isEmpty()) return true;
            long dx = discovery.hub().get().x() - hub.x();
            long dz = discovery.hub().get().z() - hub.z();
            if (dx * dx + dz * dz <= (long) SAME_SITE_BLOCKS * SAME_SITE_BLOCKS) return true;
        }
        return false;
    }
}
