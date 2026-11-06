package za.co.neroland.neroruins.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class VaultDeliveryTest {
    @Test
    void deliversOnlyWhenTheClaimSucceeds() {
        var inventory = new ArrayList<>(List.of("old", "empty"));
        var claimed = new AtomicBoolean();
        assertEquals(VaultDelivery.Result.NOT_CLAIMABLE, VaultDelivery.commit(List.copyOf(inventory),
                List.of("new", "loot"), () -> false, inventory::set, () -> claimed.set(false), error -> { throw new AssertionError(error); }));
        assertEquals(List.of("old", "empty"), inventory);
        assertEquals(VaultDelivery.Result.DELIVERED, VaultDelivery.commit(List.copyOf(inventory),
                List.of("new", "loot"), () -> { claimed.set(true); return true; }, inventory::set,
                () -> claimed.set(false), error -> { throw new AssertionError(error); }));
        assertEquals(List.of("new", "loot"), inventory);
        assertTrue(claimed.get());
    }

    @Test
    void restoresInventoryAndClaimAfterAWriteFailure() {
        var inventory = new ArrayList<>(List.of("old", "empty"));
        var claimed = new AtomicBoolean();
        var reports = new AtomicInteger();
        var failOnce = new AtomicBoolean(true);
        var result = VaultDelivery.commit(List.copyOf(inventory), List.of("new", "loot"),
                () -> { claimed.set(true); return true; }, (index, item) -> {
                    if (index == 1 && failOnce.getAndSet(false)) throw new IllegalStateException("write failed");
                    inventory.set(index, item);
                }, () -> claimed.set(false), error -> reports.incrementAndGet());
        assertEquals(VaultDelivery.Result.FAILED, result);
        assertEquals(List.of("old", "empty"), inventory);
        assertFalse(claimed.get());
        assertEquals(1, reports.get());
    }

    @Test
    void keepsTheClaimWhenInventoryRollbackFails() {
        var claimed = new AtomicBoolean();
        var reports = new AtomicInteger();
        var result = VaultDelivery.commit(List.of("old"), List.of("new"),
                () -> { claimed.set(true); return true; }, (index, item) -> { throw new IllegalStateException("write failed"); },
                () -> claimed.set(false), error -> reports.incrementAndGet());
        assertEquals(VaultDelivery.Result.ROLLBACK_FAILED, result);
        assertTrue(claimed.get());
        assertEquals(2, reports.get());
    }
}
