package za.co.neroland.neroruins.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import za.co.neroland.nerolandcore.data.SavedDataCompat;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;
import za.co.neroland.neroruins.NeroRuinsCommon;

public final class ClaimLedger extends SavedData {
    private static final String RECOVERY_NAME = "neroruins:claim_ledger";
    private static final Identifier ID = Identifier.fromNamespaceAndPath(NeroRuinsCommon.MOD_ID, "claim_ledger");
    private static final SavedDataType<ClaimLedger> TYPE = SavedDataCompat.type(ID, ClaimLedger::new, codec());
    private final Map<Location, Map<UUID, Long>> claims = new LinkedHashMap<>();
    private final Map<Location, Map<UUID, Reservation>> reservations = new LinkedHashMap<>();

    /** Most item stacks one reservation may hold; a larger roll is refused rather than stored. */
    public static final int MAX_RESERVED_STACKS = 64;

    /**
     * A roll that has been made for one player at one cache but not yet delivered. The items are stored
     * already encoded (registry-aware encoding happens in the caller), so a retry delivers exactly what was
     * rolled even if the loot table, the player's state or the data packs changed in between.
     */
    public record Reservation(String lootTable, long reservedAt, Long previousClaim, List<CompoundTag> items) {
        public Reservation {
            if (lootTable == null || lootTable.length() > 128 || Identifier.tryParse(lootTable) == null) {
                throw new IllegalArgumentException("Invalid reserved loot table");
            }
            if (reservedAt < 0 || (previousClaim != null && previousClaim < 0)) {
                throw new IllegalArgumentException("Invalid reservation time");
            }
            if (items == null || items.isEmpty() || items.size() > MAX_RESERVED_STACKS) {
                throw new IllegalArgumentException("Invalid reserved items");
            }
            items = items.stream().map(CompoundTag::copy).toList();
        }

        @Override
        public List<CompoundTag> items() {
            return items.stream().map(CompoundTag::copy).toList();
        }
    }

    public record Location(String dimension, int x, int y, int z) {
        public Location {
            if (dimension == null || dimension.length() > 128
                    || !dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("Invalid vault dimension");
            }
        }
    }

    public static ClaimLedger get(MinecraftServer server) {
        return SavedDataRecovery.get(server.overworld(), TYPE, ClaimLedger::new, RECOVERY_NAME);
    }

    public boolean mayClaim(Location location, UUID player, VaultClaimPolicy policy, long now) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(policy, "policy");
        Map<UUID, Long> vault = claims.getOrDefault(location, Map.of());
        Map<UUID, Reservation> pending = reservations.getOrDefault(location, Map.of());
        if (pending.containsKey(player)) return false;
        boolean occupied = !vault.isEmpty() || !pending.isEmpty();
        return policy.allows(occupied, vault.get(player), now);
    }

    /** The undelivered roll held for this player at this cache, if any. */
    public java.util.Optional<Reservation> reservation(Location location, UUID player) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(player, "player");
        return java.util.Optional.ofNullable(reservations.getOrDefault(location, Map.of()).get(player));
    }

    /**
     * Holds a rolled reward for {@code player}. Only succeeds when the policy would allow a claim now, so a
     * reservation can never be created to bypass the claim rules; under {@code first_come} it occupies the
     * cache exactly as a completed claim does.
     */
    public boolean reserve(Location location, UUID player, VaultClaimPolicy policy, long now, Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation");
        if (!mayClaim(location, player, policy, now)) return false;
        if (!Objects.equals(reservation.previousClaim(), lastClaim(location, player))) return false;
        reservations.computeIfAbsent(location, ignored -> new LinkedHashMap<>()).put(player, reservation);
        setDirty();
        return true;
    }

    /**
     * Converts the player's reservation into a recorded claim in one in-memory step. Returns the reservation
     * that was consumed, or empty if there was none.
     */
    public java.util.Optional<Reservation> complete(Location location, UUID player, long now) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(player, "player");
        if (now < 0) return java.util.Optional.empty();
        Map<UUID, Reservation> pending = reservations.get(location);
        if (pending == null) return java.util.Optional.empty();
        Reservation reservation = pending.remove(player);
        if (reservation == null) return java.util.Optional.empty();
        if (pending.isEmpty()) reservations.remove(location);
        claims.computeIfAbsent(location, ignored -> new LinkedHashMap<>()).put(player, now);
        setDirty();
        return java.util.Optional.of(reservation);
    }

    /** Undoes {@link #complete}: restores the previous claim time and puts the reservation back. */
    public void reopen(Location location, UUID player, Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation");
        restoreClaim(location, player, reservation.previousClaim());
        reservations.computeIfAbsent(location, ignored -> new LinkedHashMap<>()).put(player, reservation);
        setDirty();
    }

    public Long lastClaim(Location location, UUID player) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(player, "player");
        return claims.getOrDefault(location, Map.of()).get(player);
    }

    public boolean record(Location location, UUID player, VaultClaimPolicy policy, long now) {
        if (!mayClaim(location, player, policy, now)) return false;
        claims.computeIfAbsent(location, ignored -> new LinkedHashMap<>()).put(player, now);
        setDirty();
        return true;
    }

    public void restoreClaim(Location location, UUID player, Long previous) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(player, "player");
        if (previous != null && previous < 0) throw new IllegalArgumentException("Invalid previous claim time");
        if (previous == null) {
            Map<UUID, Long> owners = claims.get(location);
            if (owners == null || owners.remove(player) == null) return;
            if (owners.isEmpty()) claims.remove(location);
        } else {
            claims.computeIfAbsent(location, ignored -> new LinkedHashMap<>()).put(player, previous);
        }
        setDirty();
    }

    public Map<Location, Long> export(UUID player) {
        Objects.requireNonNull(player, "player");
        Map<Location, Long> result = new LinkedHashMap<>();
        claims.forEach((location, owners) -> {
            Long time = owners.get(player);
            if (time != null) result.put(location, time);
        });
        return Map.copyOf(result);
    }

    public Map<Location, Reservation> exportReservations(UUID player) {
        Objects.requireNonNull(player, "player");
        Map<Location, Reservation> result = new LinkedHashMap<>();
        reservations.forEach((location, owners) -> {
            Reservation reservation = owners.get(player);
            if (reservation != null) result.put(location, reservation);
        });
        return java.util.Collections.unmodifiableMap(result);
    }

    public void erase(UUID player) {
        Objects.requireNonNull(player, "player");
        boolean changed = false;
        var iterator = claims.entrySet().iterator();
        while (iterator.hasNext()) {
            Map<UUID, Long> owners = iterator.next().getValue();
            if (owners.remove(player) != null) changed = true;
            if (owners.isEmpty()) iterator.remove();
        }
        var pending = reservations.entrySet().iterator();
        while (pending.hasNext()) {
            Map<UUID, Reservation> owners = pending.next().getValue();
            if (owners.remove(player) != null) changed = true;
            if (owners.isEmpty()) pending.remove();
        }
        if (changed) setDirty();
    }

    public int purgeExpired(long now, int retentionDays) {
        if (now < 0 || retentionDays < 0 || retentionDays > 36500) {
            throw new IllegalArgumentException("Invalid claim retention");
        }
        if (retentionDays == 0) return 0;
        long duration = retentionDays * 86_400_000L;
        if (now < duration) return 0;
        long cutoff = now - duration;
        int removed = 0;
        var vaults = claims.entrySet().iterator();
        while (vaults.hasNext()) {
            Map<UUID, Long> owners = vaults.next().getValue();
            var entries = owners.entrySet().iterator();
            while (entries.hasNext()) {
                var entry = entries.next();
                if (entry.getValue() <= cutoff) {
                    entries.remove();
                    removed++;
                }
            }
            if (owners.isEmpty()) vaults.remove();
        }
        var pending = reservations.entrySet().iterator();
        while (pending.hasNext()) {
            Map<UUID, Reservation> owners = pending.next().getValue();
            var entries = owners.entrySet().iterator();
            while (entries.hasNext()) {
                if (entries.next().getValue().reservedAt() <= cutoff) {
                    entries.remove();
                    removed++;
                }
            }
            if (owners.isEmpty()) pending.remove();
        }
        if (removed > 0) setDirty();
        return removed;
    }

    public static void applyRetention(MinecraftServer server, long now, int retentionDays) {
        if (retentionDays == 0) return;
        ClaimLedger ledger = get(server);
        applyRetention(ledger, server.getWorldPath(LevelResource.ROOT), now, retentionDays,
                () -> VerifiedBackup.refresh(server.overworld(), TYPE, ledger, RECOVERY_NAME));
    }

    static void applyRetention(ClaimLedger ledger, Path worldRoot, long now, int retentionDays, Runnable backup) {
        if (retentionDays == 0) return;
        var affected = new LinkedHashSet<UUID>();
        if (now < 0 || retentionDays < 0 || retentionDays > 36500) {
            throw new IllegalArgumentException("Invalid claim retention");
        }
        long duration = retentionDays * 86_400_000L;
        if (now < duration) return;
        long cutoff = now - duration;
        ledger.claims.values().forEach(owners -> owners.forEach((player, time) -> {
            if (time <= cutoff) affected.add(player);
        }));
        ledger.reservations.values().forEach(owners -> owners.forEach((player, reservation) -> {
            if (reservation.reservedAt() <= cutoff) affected.add(player);
        }));
        if (affected.isEmpty()) return;
        RuntimeException failure = null;
        for (UUID player : affected) {
            try {
                ArchiveExport.erase(worldRoot, player);
            } catch (IOException error) {
                RuntimeException wrapped = new UncheckedIOException("Could not remove an expired claim export", error);
                if (failure == null) failure = wrapped;
                else failure.addSuppressed(wrapped);
            }
        }
        if (failure != null) throw failure;
        Map<Location, Map<UUID, Long>> before = new LinkedHashMap<>();
        ledger.claims.forEach((location, owners) -> before.put(location, new LinkedHashMap<>(owners)));
        Map<Location, Map<UUID, Reservation>> reservedBefore = new LinkedHashMap<>();
        ledger.reservations.forEach((location, owners) -> reservedBefore.put(location, new LinkedHashMap<>(owners)));
        if (ledger.purgeExpired(now, retentionDays) > 0) {
            try {
                backup.run();
            } catch (RuntimeException | Error error) {
                ledger.claims.clear();
                ledger.claims.putAll(before);
                ledger.reservations.clear();
                ledger.reservations.putAll(reservedBefore);
                ledger.setDirty();
                throw error;
            }
        }
    }

    static RuinPlayerData.Store erasureStore() {
        return new RuinPlayerData.Store() {
            public void erase(MinecraftServer server, UUID player) { ClaimLedger.get(server).erase(player); }
            public void backupNow(MinecraftServer server, UUID player) {
                VerifiedBackup.refresh(server.overworld(), TYPE, ClaimLedger.get(server), RECOVERY_NAME);
            }
        };
    }

    private record Entry(String dimension, int x, int y, int z, String player, long claimedAt) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.<Entry>create(instance -> instance.group(
                Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
                Codec.INT.fieldOf("x").forGetter(Entry::x), Codec.INT.fieldOf("y").forGetter(Entry::y),
                Codec.INT.fieldOf("z").forGetter(Entry::z), Codec.STRING.fieldOf("player").forGetter(Entry::player),
                Codec.LONG.fieldOf("claimed_at").forGetter(Entry::claimedAt)
        ).apply(instance, Entry::new)).validate(entry -> {
            try {
                new Location(entry.dimension, entry.x, entry.y, entry.z);
                UUID.fromString(entry.player);
                if (entry.claimedAt < 0) return DataResult.error(() -> "Invalid claim time");
                return DataResult.success(entry);
            } catch (IllegalArgumentException failure) {
                return DataResult.error(() -> "Invalid vault claim");
            }
        });
    }

    private record Pending(String dimension, int x, int y, int z, String player, String lootTable, long reservedAt,
            java.util.Optional<Long> previousClaim, List<CompoundTag> items) {
        private static final Codec<Pending> CODEC = RecordCodecBuilder.<Pending>create(instance -> instance.group(
                Codec.STRING.fieldOf("dimension").forGetter(Pending::dimension),
                Codec.INT.fieldOf("x").forGetter(Pending::x), Codec.INT.fieldOf("y").forGetter(Pending::y),
                Codec.INT.fieldOf("z").forGetter(Pending::z), Codec.STRING.fieldOf("player").forGetter(Pending::player),
                Codec.STRING.fieldOf("loot_table").forGetter(Pending::lootTable),
                Codec.LONG.fieldOf("reserved_at").forGetter(Pending::reservedAt),
                Codec.LONG.optionalFieldOf("previous_claim").forGetter(Pending::previousClaim),
                CompoundTag.CODEC.listOf().fieldOf("items").forGetter(Pending::items)
        ).apply(instance, Pending::new)).validate(pending -> {
            try {
                new Location(pending.dimension, pending.x, pending.y, pending.z);
                UUID.fromString(pending.player);
                pending.reservation();
                return DataResult.success(pending);
            } catch (IllegalArgumentException failure) {
                return DataResult.error(() -> "Invalid vault reservation");
            }
        });

        private Reservation reservation() {
            return new Reservation(lootTable, reservedAt, previousClaim.orElse(null), items);
        }
    }

    private record Stored(List<Entry> entries, List<Pending> reservations) { }

    public static Codec<ClaimLedger> codec() {
        return RecordCodecBuilder.<Stored>create(instance -> instance.group(
                Entry.CODEC.listOf().fieldOf("entries").forGetter(Stored::entries),
                Pending.CODEC.listOf().optionalFieldOf("reservations", List.of()).forGetter(Stored::reservations)
        ).apply(instance, Stored::new)).xmap(ClaimLedger::decode, ClaimLedger::encode);
    }

    private static ClaimLedger decode(Stored stored) {
        ClaimLedger ledger = decodeClaims(stored.entries());
        for (Pending pending : stored.reservations()) {
            ledger.reservations.computeIfAbsent(new Location(pending.dimension, pending.x, pending.y, pending.z),
                    ignored -> new LinkedHashMap<>()).put(UUID.fromString(pending.player), pending.reservation());
        }
        return ledger;
    }

    private Stored encode() {
        List<Pending> pending = new ArrayList<>();
        reservations.forEach((location, owners) -> owners.forEach((player, reservation) -> pending.add(new Pending(
                location.dimension, location.x, location.y, location.z, player.toString(), reservation.lootTable(),
                reservation.reservedAt(), java.util.Optional.ofNullable(reservation.previousClaim()),
                reservation.items()))));
        return new Stored(encodeClaims(), pending);
    }

    private static ClaimLedger decodeClaims(List<Entry> entries) {
        ClaimLedger ledger = new ClaimLedger();
        for (Entry entry : entries) {
            Location location = new Location(entry.dimension, entry.x, entry.y, entry.z);
            ledger.claims.computeIfAbsent(location, ignored -> new LinkedHashMap<>())
                    .merge(UUID.fromString(entry.player), entry.claimedAt, Math::max);
        }
        return ledger;
    }

    private List<Entry> encodeClaims() {
        List<Entry> result = new ArrayList<>();
        claims.forEach((location, owners) -> owners.forEach((player, time) -> result.add(new Entry(
                location.dimension, location.x, location.y, location.z, player.toString(), time))));
        return result;
    }
}
