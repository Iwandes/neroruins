package za.co.neroland.neroruins.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VaultReservationTest {
    private static final ClaimLedger.Location VAULT = new ClaimLedger.Location("minecraft:overworld", 3, 40, -7);
    private static final VaultClaimPolicy PER_PLAYER = VaultClaimPolicy.parse("per_player").orElseThrow();
    private static final VaultClaimPolicy FIRST_COME = VaultClaimPolicy.parse("first_come").orElseThrow();

    private static CompoundTag item(String id) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        return tag;
    }

    private static ClaimLedger.Reservation reservation(long at, Long previous, String... ids) {
        List<CompoundTag> items = new ArrayList<>();
        for (String id : ids) items.add(item(id));
        return new ClaimLedger.Reservation("neroruins:chests/prototype_vault", at, previous, items);
    }

    @Test
    void reservedRollSurvivesSaveAndIsDeliveredUnchanged() {
        var player = UUID.randomUUID();
        var ledger = new ClaimLedger();
        assertTrue(ledger.reserve(VAULT, player, PER_PLAYER, 10, reservation(10, null, "minecraft:diamond")));
        assertFalse(ledger.mayClaim(VAULT, player, PER_PLAYER, 11), "a pending roll must not allow a second roll");
        assertFalse(ledger.reserve(VAULT, player, PER_PLAYER, 11, reservation(11, null, "minecraft:dirt")));

        var saved = ClaimLedger.codec().encodeStart(NbtOps.INSTANCE, ledger).getOrThrow();
        var loaded = ClaimLedger.codec().parse(NbtOps.INSTANCE, saved).getOrThrow();
        var held = loaded.reservation(VAULT, player).orElseThrow();
        assertTrue(held.items().get(0).toString().contains("minecraft:diamond"));

        var consumed = loaded.complete(VAULT, player, 20).orElseThrow();
        assertEquals(held, consumed);
        assertTrue(loaded.reservation(VAULT, player).isEmpty());
        assertEquals(20L, loaded.lastClaim(VAULT, player));
        assertFalse(loaded.mayClaim(VAULT, player, PER_PLAYER, 21));
        assertTrue(loaded.complete(VAULT, player, 22).isEmpty(), "completion without a reservation is refused");
    }

    @Test
    void failedDeliveryReopensTheSameReservation() {
        var player = UUID.randomUUID();
        var ledger = new ClaimLedger();
        var roll = reservation(5, null, "minecraft:emerald");
        assertTrue(ledger.reserve(VAULT, player, PER_PLAYER, 5, roll));
        var consumed = ledger.complete(VAULT, player, 6).orElseThrow();
        ledger.reopen(VAULT, player, consumed);
        assertEquals(roll, ledger.reservation(VAULT, player).orElseThrow());
        assertEquals(null, ledger.lastClaim(VAULT, player));
    }

    @Test
    void firstComeReservationOccupiesTheCacheForOthers() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var ledger = new ClaimLedger();
        assertTrue(ledger.reserve(VAULT, first, FIRST_COME, 1, reservation(1, null, "minecraft:gold_ingot")));
        assertFalse(ledger.mayClaim(VAULT, second, FIRST_COME, 2));
        assertFalse(ledger.reserve(VAULT, second, FIRST_COME, 2, reservation(2, null, "minecraft:gold_ingot")));
        assertTrue(ledger.mayClaim(VAULT, second, PER_PLAYER, 2), "per-player caches stay open to others");
    }

    @Test
    void reservationMustMatchThePreviousClaimItWasRolledFor() {
        var player = UUID.randomUUID();
        var ledger = new ClaimLedger();
        var timed = VaultClaimPolicy.parse("timed(1)").orElseThrow();
        assertTrue(ledger.record(VAULT, player, timed, 100));
        assertFalse(ledger.reserve(VAULT, player, timed, 3_600_200, reservation(3_600_200, null, "minecraft:apple")));
        assertTrue(ledger.reserve(VAULT, player, timed, 3_600_200, reservation(3_600_200, 100L, "minecraft:apple")));
    }

    @Test
    void erasureExportAndRetentionCoverReservations(@TempDir Path world) throws Exception {
        var player = UUID.randomUUID();
        var other = UUID.randomUUID();
        var ledger = new ClaimLedger();
        assertTrue(ledger.reserve(VAULT, player, PER_PLAYER, 10, reservation(10, null, "minecraft:diamond")));
        assertTrue(ledger.reserve(VAULT, other, PER_PLAYER, 86_400_000L, reservation(86_400_000L, null, "minecraft:coal")));
        assertTrue(ledger.exportReservations(other).size() == 1 && ledger.exportReservations(player).size() == 1);

        var exported = world.resolve(ArchiveExport.write(world, player, java.util.Map.of(), ledger.export(player),
                ledger.exportReservations(player), List.of()));
        var json = JsonParser.parseString(Files.readString(exported)).getAsJsonObject();
        var held = json.getAsJsonArray("held_vault_rewards");
        assertEquals(1, held.size());
        assertTrue(held.get(0).getAsJsonObject().getAsJsonArray("items").get(0).getAsString().contains("minecraft:diamond"));
        assertFalse(Files.readString(exported).contains(other.toString()));

        ClaimLedger.applyRetention(ledger, world, 86_400_010L, 1, () -> { });
        assertTrue(ledger.reservation(VAULT, player).isEmpty(), "an expired reservation is purged");
        assertFalse(Files.exists(exported), "the expired holder's export is removed");
        assertTrue(ledger.reservation(VAULT, other).isPresent());

        ledger.erase(other);
        assertTrue(ledger.exportReservations(other).isEmpty());
    }

    @Test
    void oldSavesLoadAndCorruptReservationsAreRejected() {
        var old = JsonParser.parseString("{\"entries\":[{\"dimension\":\"minecraft:overworld\",\"x\":1,\"y\":2,\"z\":3,"
                + "\"player\":\"" + UUID.randomUUID() + "\",\"claimed_at\":5}]}");
        var loaded = ClaimLedger.codec().parse(JsonOps.INSTANCE, old).getOrThrow();
        assertEquals(1, loaded.export(UUID.fromString(old.getAsJsonObject().getAsJsonArray("entries").get(0)
                .getAsJsonObject().get("player").getAsString())).size());

        var corrupt = JsonParser.parseString("{\"entries\":[],\"reservations\":[{\"dimension\":\"minecraft:overworld\","
                + "\"x\":1,\"y\":2,\"z\":3,\"player\":\"" + UUID.randomUUID() + "\",\"loot_table\":\"neroruins:x\","
                + "\"reserved_at\":1,\"items\":[]}]}");
        assertTrue(ClaimLedger.codec().parse(JsonOps.INSTANCE, corrupt).error().isPresent(), "empty rolls are invalid");

        assertThrows(IllegalArgumentException.class, () -> new ClaimLedger.Reservation("bad table", 1, null,
                List.of(item("minecraft:stone"))));
        List<CompoundTag> tooMany = new ArrayList<>();
        for (int i = 0; i <= ClaimLedger.MAX_RESERVED_STACKS; i++) tooMany.add(item("minecraft:stone"));
        assertThrows(IllegalArgumentException.class, () -> new ClaimLedger.Reservation("neroruins:x", 1, null, tooMany));
    }
}
