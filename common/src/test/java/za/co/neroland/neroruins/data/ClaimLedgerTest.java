package za.co.neroland.neroruins.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class ClaimLedgerTest {
    @Test
    void storesPersonalClaimsAcrossDimensionsAndErasesOnlyTheRequester() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var overworld = new ClaimLedger.Location("minecraft:overworld", 4, 70, -8);
        var otherWorld = new ClaimLedger.Location("minecraft:the_nether", 4, 70, -8);
        var policy = VaultClaimPolicy.parse("per_player").orElseThrow();
        var ledger = new ClaimLedger();
        assertTrue(ledger.record(overworld, first, policy, 100));
        assertFalse(ledger.record(overworld, first, policy, 101));
        assertTrue(ledger.record(overworld, second, policy, 102));
        assertTrue(ledger.record(otherWorld, first, policy, 103));
        var json = ClaimLedger.codec().encodeStart(JsonOps.INSTANCE, ledger).getOrThrow();
        var loaded = ClaimLedger.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(2, loaded.export(first).size());
        assertThrows(UnsupportedOperationException.class, () -> loaded.export(first).clear());
        loaded.erase(first);
        assertTrue(loaded.export(first).isEmpty());
        assertEquals(102L, loaded.export(second).get(overworld));
    }

    @Test
    void policyAndCorruptDataCannotCreateUnintendedClaims() {
        var player = UUID.randomUUID();
        var other = UUID.randomUUID();
        var location = new ClaimLedger.Location("minecraft:overworld", 0, 64, 0);
        var ledger = new ClaimLedger();
        var firstCome = VaultClaimPolicy.parse("first_come").orElseThrow();
        assertFalse(ledger.record(location, player, firstCome, -1));
        assertTrue(ledger.record(location, player, firstCome, 1));
        assertFalse(ledger.record(location, other, firstCome, 2));
        assertTrue(ledger.export(other).isEmpty());
        var corrupt = JsonParser.parseString("{\"entries\":[{\"dimension\":\"bad\",\"x\":0,\"y\":64,\"z\":0,\"player\":\"bad\",\"claimed_at\":1}]}");
        assertTrue(ClaimLedger.codec().parse(JsonOps.INSTANCE, corrupt).error().isPresent());
    }

    @Test
    void timedClaimRecordsTheLatestTime() {
        var player = UUID.randomUUID();
        var location = new ClaimLedger.Location("minecraft:overworld", 0, 64, 0);
        var policy = VaultClaimPolicy.parse("timed(1)").orElseThrow();
        var ledger = new ClaimLedger();
        assertTrue(ledger.record(location, player, policy, 100));
        assertFalse(ledger.record(location, player, policy, 3_600_099));
        assertTrue(ledger.record(location, player, policy, 3_600_100));
        assertEquals(3_600_100L, ledger.export(player).get(location));
        ledger.restoreClaim(location, player, 100L);
        assertEquals(100L, ledger.lastClaim(location, player));
        ledger.restoreClaim(location, player, null);
        assertTrue(ledger.export(player).isEmpty());
    }

    @Test
    void retentionRemovesOnlyExpiredClaimsAndCanBeDisabled() {
        var old = UUID.randomUUID();
        var recent = UUID.randomUUID();
        var location = new ClaimLedger.Location("minecraft:overworld", 0, 64, 0);
        var ledger = new ClaimLedger();
        var policy = VaultClaimPolicy.parse("per_player").orElseThrow();
        assertTrue(ledger.record(location, old, policy, 10));
        assertTrue(ledger.record(location, recent, policy, 100));
        assertEquals(0, ledger.purgeExpired(86_400_010L, 0));
        assertEquals(1, ledger.purgeExpired(86_400_010L, 1));
        assertTrue(ledger.export(old).isEmpty());
        assertEquals(100L, ledger.export(recent).get(location));
        assertThrows(IllegalArgumentException.class, () -> ledger.purgeExpired(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> ledger.purgeExpired(100, -1));
    }

    @Test
    void retentionRemovesStaleExportsAndKeepsClaimsIfCleanupFails(@TempDir Path world) throws Exception {
        var player = UUID.randomUUID();
        var location = new ClaimLedger.Location("minecraft:overworld", 1, 64, 2);
        var ledger = new ClaimLedger();
        assertTrue(ledger.record(location, player, VaultClaimPolicy.parse("per_player").orElseThrow(), 10));
        var exported = world.resolve(ArchiveExport.write(world, player, Map.of(), ledger.export(player), ledger.exportReservations(player), java.util.List.of()));
        var backups = new AtomicInteger();
        ClaimLedger.applyRetention(ledger, world, 86_400_010L, 1, backups::incrementAndGet);
        assertFalse(Files.exists(exported));
        assertEquals(1, backups.get());
        assertTrue(ledger.export(player).isEmpty());

        assertTrue(ledger.record(location, player, VaultClaimPolicy.parse("per_player").orElseThrow(), 10));
        Files.delete(world.resolve("neroruins-exports"));
        Files.writeString(world.resolve("neroruins-exports"), "not a directory");
        assertThrows(UncheckedIOException.class,
                () -> ClaimLedger.applyRetention(ledger, world, 86_400_010L, 1, backups::incrementAndGet));
        assertEquals(10L, ledger.export(player).get(location));
        assertEquals(1, backups.get());
    }

    @Test
    void retentionRestoresClaimsWhenBackupRefreshFails(@TempDir Path world) throws Exception {
        var player = UUID.randomUUID();
        var location = new ClaimLedger.Location("minecraft:overworld", 1, 64, 2);
        var ledger = new ClaimLedger();
        assertTrue(ledger.record(location, player, VaultClaimPolicy.parse("per_player").orElseThrow(), 10));
        var exported = world.resolve(ArchiveExport.write(world, player, Map.of(), ledger.export(player), ledger.exportReservations(player), java.util.List.of()));
        assertThrows(IllegalStateException.class, () -> ClaimLedger.applyRetention(ledger, world,
                86_400_010L, 1, () -> { throw new IllegalStateException("backup failed"); }));
        assertEquals(10L, ledger.export(player).get(location));
        assertFalse(Files.exists(exported));
    }
}
