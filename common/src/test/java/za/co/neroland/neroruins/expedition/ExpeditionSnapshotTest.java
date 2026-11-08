package za.co.neroland.neroruins.expedition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroruins.data.ExpeditionRecord;

class ExpeditionSnapshotTest {
    private static ExpeditionRecord.Discovery found(long time, Optional<ExpeditionRecord.Hub> hub) {
        return new ExpeditionRecord.Discovery("neroruins:bunker_earth", "bunker", "outpost", "minecraft:overworld",
                hub, time);
    }

    @Test
    void showsOnlyTheRequestersRecordNewestFirstAndHidesDroppedPositions() {
        var player = UUID.randomUUID();
        var other = UUID.randomUUID();
        var record = new ExpeditionRecord();
        record.discover(player, found(1, Optional.of(new ExpeditionRecord.Hub(10, 50, 20))));
        record.discover(player, found(5, Optional.of(new ExpeditionRecord.Hub(900, 50, 20))));
        record.discover(other, found(9, Optional.of(new ExpeditionRecord.Hub(-5000, 50, 77))));
        record.dropExpiredPositions(3L + 86_400_000L, 1);

        var snapshot = ExpeditionSnapshot.of(record.export(player));
        assertEquals(2, snapshot.entries().size(), "another player's discovery is never included");
        assertTrue(snapshot.entries().get(0).positioned());
        assertEquals(900, snapshot.entries().get(0).x());
        assertFalse(snapshot.entries().get(1).positioned(), "the expired position shows as not kept");
        assertEquals(0, snapshot.entries().get(1).x());
    }

    @Test
    void roundTripsOverTheWire() {
        var snapshot = ExpeditionSnapshot.of(List.of(found(2, Optional.of(new ExpeditionRecord.Hub(-12, 4, 33))),
                found(1, Optional.empty())));
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        ExpeditionSnapshot.CODEC.encode(buf, snapshot);
        assertEquals(snapshot, ExpeditionSnapshot.CODEC.decode(buf));
    }

    @Test
    void capsTheListAndRejectsMalformedEntries() {
        List<ExpeditionRecord.Discovery> many = new ArrayList<>();
        for (int i = 0; i < ExpeditionSnapshot.MAX_ENTRIES + 10; i++) {
            many.add(found(i, Optional.of(new ExpeditionRecord.Hub(i * 1000, 0, 0))));
        }
        var capped = ExpeditionSnapshot.of(many);
        assertEquals(ExpeditionSnapshot.MAX_ENTRIES, capped.entries().size());
        assertEquals((ExpeditionSnapshot.MAX_ENTRIES + 9) * 1000, capped.entries().get(0).x(), "newest kept");
        assertThrows(IllegalArgumentException.class,
                () -> new ExpeditionSnapshot.Entry("bunker", "huge", "minecraft:overworld", true, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpeditionSnapshot.Entry("bunker", "outpost", "not a dimension", true, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpeditionSnapshot.Entry("bunker", "outpost", "minecraft:overworld", false, 5, 0),
                "an unpositioned entry carries no coordinates");
        var oversized = new ArrayList<ExpeditionSnapshot.Entry>();
        for (int i = 0; i <= ExpeditionSnapshot.MAX_ENTRIES; i++) {
            oversized.add(new ExpeditionSnapshot.Entry("bunker", "outpost", "minecraft:overworld", false, 0, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> new ExpeditionSnapshot(oversized));
    }
}
