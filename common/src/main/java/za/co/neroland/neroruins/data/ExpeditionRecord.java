package za.co.neroland.neroruins.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import za.co.neroland.nerolandcore.data.SavedDataCompat;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;
import za.co.neroland.neroruins.NeroRuinsCommon;

/**
 * Each player's own record of the ruin complexes they have entered. Personal data: it pairs a player with
 * where they have been. Positions are kept only when {@code privacy.recordDiscoveryPositions} is on and are
 * dropped after {@code privacy.expeditionRetentionDays}; the structure type and world are kept until erasure.
 * Nothing here is ever shown to another player.
 */
public final class ExpeditionRecord extends SavedData {
    private static final String RECOVERY_NAME = "neroruins:expedition_record";
    private static final Identifier ID = Identifier.fromNamespaceAndPath(NeroRuinsCommon.MOD_ID, "expedition_record");
    private static final SavedDataType<ExpeditionRecord> TYPE = SavedDataCompat.type(ID, ExpeditionRecord::new, codec());
    public static final int MAX_PER_PLAYER = 4096;

    private final Map<UUID, List<Discovery>> records = new LinkedHashMap<>();

    public record Hub(int x, int y, int z) { }

    public record Discovery(String structure, String family, String sizeClass, String dimension, Optional<Hub> hub,
            long firstSeen) {
        public Discovery {
            if (!validId(structure) || !validId(dimension) || family == null
                    || !family.matches("[a-z0-9_]{1,64}") || sizeClass == null
                    || !(sizeClass.equals("outpost") || sizeClass.equals("complex") || sizeClass.equals("citadel"))
                    || hub == null || firstSeen < 0) {
                throw new IllegalArgumentException("Invalid discovery");
            }
        }

        boolean sameSite(Discovery other) {
            return structure.equals(other.structure) && dimension.equals(other.dimension)
                    && (other.hub.isEmpty() || hub.equals(other.hub));
        }

        Discovery withoutPosition() {
            return new Discovery(structure, family, sizeClass, dimension, Optional.empty(), firstSeen);
        }
    }

    private static boolean validId(String value) {
        return value != null && value.length() <= 128 && value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
    }

    public static ExpeditionRecord get(MinecraftServer server) {
        return SavedDataRecovery.get(server.overworld(), TYPE, ExpeditionRecord::new, RECOVERY_NAME);
    }

    /**
     * Records a first visit. A discovery with a position matches only an earlier visit to that hub. A discovery
     * without one (minimisation on) matches any earlier visit to the same structure type in the same world, so
     * it is recorded once per type and world.
     */
    public boolean discover(UUID player, Discovery discovery) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(discovery, "discovery");
        List<Discovery> list = records.computeIfAbsent(player, ignored -> new ArrayList<>());
        for (Discovery existing : list) {
            if (existing.sameSite(discovery)) return false;
        }
        if (list.size() >= MAX_PER_PLAYER) return false;
        list.add(discovery);
        setDirty();
        return true;
    }

    public List<Discovery> export(UUID player) {
        Objects.requireNonNull(player, "player");
        return List.copyOf(records.getOrDefault(player, List.of()));
    }

    public void erase(UUID player) {
        Objects.requireNonNull(player, "player");
        if (records.remove(player) != null) setDirty();
    }

    /** Drops positions older than the retention period; zero keeps them until erasure. Returns how many. */
    public int dropExpiredPositions(long now, int retentionDays) {
        if (now < 0 || retentionDays < 0 || retentionDays > 36500) {
            throw new IllegalArgumentException("Invalid expedition retention");
        }
        if (retentionDays == 0) return 0;
        long duration = retentionDays * 86_400_000L;
        if (now < duration) return 0;
        return dropPositionsUpTo(now - duration);
    }

    /** Removes every stored position, for when a server turns position recording off. */
    public int dropAllPositions() {
        return dropPositionsUpTo(Long.MAX_VALUE);
    }

    private int dropPositionsUpTo(long cutoff) {
        int dropped = 0;
        for (List<Discovery> list : records.values()) {
            for (int i = 0; i < list.size(); i++) {
                Discovery discovery = list.get(i);
                if (discovery.hub().isPresent() && discovery.firstSeen() <= cutoff) {
                    list.set(i, discovery.withoutPosition());
                    dropped++;
                }
            }
        }
        if (dropped > 0) setDirty();
        return dropped;
    }

    public static void applyRetention(MinecraftServer server, long now, int retentionDays, boolean keepPositions) {
        ExpeditionRecord record = get(server);
        int changed = keepPositions ? record.dropExpiredPositions(now, retentionDays) : record.dropAllPositions();
        if (changed > 0) VerifiedBackup.refresh(server.overworld(), TYPE, record, RECOVERY_NAME);
    }

    static RuinPlayerData.Store erasureStore() {
        return new RuinPlayerData.Store() {
            public void erase(MinecraftServer server, UUID player) { ExpeditionRecord.get(server).erase(player); }
            public void backupNow(MinecraftServer server, UUID player) {
                VerifiedBackup.refresh(server.overworld(), TYPE, ExpeditionRecord.get(server), RECOVERY_NAME);
            }
        };
    }

    private record Entry(String player, String structure, String family, String sizeClass, String dimension,
            Optional<List<Integer>> hub, long firstSeen) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.<Entry>create(instance -> instance.group(
                Codec.STRING.fieldOf("player").forGetter(Entry::player),
                Codec.STRING.fieldOf("structure").forGetter(Entry::structure),
                Codec.STRING.fieldOf("family").forGetter(Entry::family),
                Codec.STRING.fieldOf("size_class").forGetter(Entry::sizeClass),
                Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
                Codec.INT.listOf().optionalFieldOf("hub").forGetter(Entry::hub),
                Codec.LONG.fieldOf("first_seen").forGetter(Entry::firstSeen)
        ).apply(instance, Entry::new)).validate(entry -> {
            try {
                UUID.fromString(entry.player);
                entry.discovery();
                return DataResult.success(entry);
            } catch (IllegalArgumentException failure) {
                return DataResult.error(() -> "Invalid expedition entry");
            }
        });

        private Discovery discovery() {
            Optional<Hub> position = hub.map(values -> {
                if (values.size() != 3) throw new IllegalArgumentException("Invalid hub position");
                return new Hub(values.get(0), values.get(1), values.get(2));
            });
            return new Discovery(structure, family, sizeClass, dimension, position, firstSeen);
        }
    }

    public static Codec<ExpeditionRecord> codec() {
        return Entry.CODEC.listOf().fieldOf("entries").xmap(ExpeditionRecord::decode, ExpeditionRecord::encode).codec();
    }

    private static ExpeditionRecord decode(List<Entry> entries) {
        ExpeditionRecord record = new ExpeditionRecord();
        for (Entry entry : entries) {
            List<Discovery> list = record.records.computeIfAbsent(UUID.fromString(entry.player), ignored -> new ArrayList<>());
            Discovery discovery = entry.discovery();
            if (list.size() < MAX_PER_PLAYER && list.stream().noneMatch(existing -> existing.sameSite(discovery))) {
                list.add(discovery);
            }
        }
        return record;
    }

    private List<Entry> encode() {
        List<Entry> result = new ArrayList<>();
        records.forEach((player, list) -> list.forEach(discovery -> result.add(new Entry(player.toString(),
                discovery.structure(), discovery.family(), discovery.sizeClass(), discovery.dimension(),
                discovery.hub().map(hub -> List.of(hub.x(), hub.y(), hub.z())), discovery.firstSeen()))));
        return result;
    }
}
