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
        return policy.allows(!vault.isEmpty(), vault.get(player), now);
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

    public void erase(UUID player) {
        Objects.requireNonNull(player, "player");
        boolean changed = false;
        var iterator = claims.entrySet().iterator();
        while (iterator.hasNext()) {
            Map<UUID, Long> owners = iterator.next().getValue();
            if (owners.remove(player) != null) changed = true;
            if (owners.isEmpty()) iterator.remove();
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
        if (removed > 0) setDirty();
        return removed;
    }

    public static void applyRetention(MinecraftServer server, long now, int retentionDays) {
        if (retentionDays == 0) return;
        ClaimLedger ledger = get(server);
        applyRetention(ledger, server.getWorldPath(LevelResource.ROOT), now, retentionDays,
                () -> SavedDataRecovery.backupNow(server.overworld(), TYPE, ledger, RECOVERY_NAME));
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
        if (ledger.purgeExpired(now, retentionDays) > 0) {
            try {
                backup.run();
            } catch (RuntimeException | Error error) {
                ledger.claims.clear();
                ledger.claims.putAll(before);
                ledger.setDirty();
                throw error;
            }
        }
    }

    static RuinPlayerData.Store erasureStore() {
        return new RuinPlayerData.Store() {
            public void erase(MinecraftServer server, UUID player) { ClaimLedger.get(server).erase(player); }
            public void backupNow(MinecraftServer server, UUID player) {
                SavedDataRecovery.backupNow(server.overworld(), TYPE, ClaimLedger.get(server), RECOVERY_NAME);
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

    public static Codec<ClaimLedger> codec() {
        return Entry.CODEC.listOf().fieldOf("entries").xmap(ClaimLedger::decode, ClaimLedger::encode).codec();
    }

    private static ClaimLedger decode(List<Entry> entries) {
        ClaimLedger ledger = new ClaimLedger();
        for (Entry entry : entries) {
            Location location = new Location(entry.dimension, entry.x, entry.y, entry.z);
            ledger.claims.computeIfAbsent(location, ignored -> new LinkedHashMap<>())
                    .merge(UUID.fromString(entry.player), entry.claimedAt, Math::max);
        }
        return ledger;
    }

    private List<Entry> encode() {
        List<Entry> result = new ArrayList<>();
        claims.forEach((location, owners) -> owners.forEach((player, time) -> result.add(new Entry(
                location.dimension, location.x, location.y, location.z, player.toString(), time))));
        return result;
    }
}
