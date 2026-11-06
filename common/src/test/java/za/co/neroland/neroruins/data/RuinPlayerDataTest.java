package za.co.neroland.neroruins.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.MinecraftServer;
import org.junit.jupiter.api.Test;

class RuinPlayerDataTest {
    @Test
    void erasesAndRefreshesBackup() {
        AtomicInteger steps = new AtomicInteger();
        RuinPlayerData.Store store = new RuinPlayerData.Store() {
            public void erase(MinecraftServer server, UUID player) { assertEquals(0, steps.getAndIncrement()); }
            public void backupNow(MinecraftServer server, UUID player) { assertEquals(1, steps.getAndIncrement()); }
        };
        RuinPlayerData.eraseStores(List.of(store), null, UUID.randomUUID());
        assertEquals(2, steps.get());
    }

    @Test
    void erasesAndBacksUpRemainingStoresWhenOneFails() {
        AtomicInteger erased = new AtomicInteger();
        AtomicInteger backedUp = new AtomicInteger();
        RuinPlayerData.Store failing = new RuinPlayerData.Store() {
            public void erase(MinecraftServer server, UUID player) { throw new IllegalStateException("failure"); }
            public void backupNow(MinecraftServer server, UUID player) { throw new AssertionError("must not back up failed erasure"); }
        };
        RuinPlayerData.Store healthy = new RuinPlayerData.Store() {
            public void erase(MinecraftServer server, UUID player) { erased.incrementAndGet(); }
            public void backupNow(MinecraftServer server, UUID player) { backedUp.incrementAndGet(); }
        };

        assertThrows(IllegalStateException.class,
                () -> RuinPlayerData.eraseStores(List.of(failing, healthy), null, UUID.randomUUID()));
        assertEquals(1, erased.get());
        assertEquals(1, backedUp.get());
    }

    @Test
    void reportsBackupFailureAndContinuesErasing() {
        AtomicInteger erased = new AtomicInteger();
        RuinPlayerData.Store failingBackup = new RuinPlayerData.Store() {
            public void erase(MinecraftServer server, UUID player) { erased.incrementAndGet(); }
            public void backupNow(MinecraftServer server, UUID player) { throw new IllegalStateException("backup failure"); }
        };
        RuinPlayerData.Store next = new RuinPlayerData.Store() {
            public void erase(MinecraftServer server, UUID player) { erased.incrementAndGet(); }
            public void backupNow(MinecraftServer server, UUID player) { }
        };
        assertThrows(IllegalStateException.class,
                () -> RuinPlayerData.eraseStores(List.of(failingBackup, next), null, UUID.randomUUID()));
        assertEquals(2, erased.get());
    }
}
