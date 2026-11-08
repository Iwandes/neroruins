package za.co.neroland.neroruins.puzzle;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Traces power through floor conduits. Power leaves a source in its facing direction and passes into a
 * neighbouring conduit only if that conduit has an opening toward the source; it then leaves through the
 * conduit's other opening. The trace ends at a conduit lock, at a gap, or after {@link #MAX_STEPS} pieces.
 */
public final class ConduitRouting {
    public static final int MAX_STEPS = 64;

    /** What occupies a floor cell, as far as power is concerned. */
    public sealed interface Cell permits Pipe, Lock, Empty { }
    public record Pipe(Set<Direction> openings) implements Cell { }
    public record Lock() implements Cell { }
    public record Empty() implements Cell { }

    public interface Grid { Cell at(BlockPos pos); }

    /** The conduits that carry power, in order, and whether the power reached a lock. */
    public record Trace(List<BlockPos> powered, BlockPos lock) {
        public boolean solved() { return lock != null; }
    }

    private ConduitRouting() { }

    public static Set<Direction> openings(Direction facing, boolean corner) {
        if (!facing.getAxis().isHorizontal()) throw new IllegalArgumentException("Conduits lie flat");
        return corner ? EnumSet.of(facing, facing.getClockWise()) : EnumSet.of(facing, facing.getOpposite());
    }

    public static Trace trace(Grid grid, BlockPos source, Direction out) {
        List<BlockPos> powered = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        BlockPos at = source.relative(out);
        Direction entering = out;
        for (int step = 0; step < MAX_STEPS; step++) {
            Cell cell = grid.at(at);
            if (cell instanceof Lock) return new Trace(List.copyOf(powered), at);
            if (!(cell instanceof Pipe pipe) || !seen.add(at)) break;
            Direction from = entering.getOpposite();
            if (!pipe.openings().contains(from)) break;
            powered.add(at);
            Direction next = null;
            for (Direction opening : pipe.openings()) if (opening != from) next = opening;
            if (next == null) break;
            at = at.relative(next);
            entering = next;
        }
        return new Trace(List.copyOf(powered), null);
    }
}
