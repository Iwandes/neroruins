package za.co.neroland.neroruins.guardian;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import za.co.neroland.neroruins.NeroRuinsCommon;

/**
 * A guardian spawn table ({@code data/<namespace>/spawn_tables/<path>.json}). Entries name entity ids
 * with a weight and the spawner tiers they belong to. An entry marked {@code "required": false} names
 * another mod's mob: when that id is not registered the entry is simply skipped, so tables can list
 * NeroCreatures guardians next to the Sentinel without any dependency. Required entries must exist;
 * the resource tests check that.
 */
public record SpawnTable(List<Entry> entries) {
    public static final int MAX_TIER = 3;

    public record Entry(String entity, int weight, int minTier, int maxTier, boolean required) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.<Entry>create(instance -> instance.group(
                Codec.STRING.validate(id -> Identifier.tryParse(id) != null && id.contains(":")
                        ? DataResult.success(id) : DataResult.error(() -> "Invalid entity id")).fieldOf("entity").forGetter(Entry::entity),
                Codec.intRange(1, 1000).fieldOf("weight").forGetter(Entry::weight),
                Codec.intRange(1, MAX_TIER).optionalFieldOf("min_tier", 1).forGetter(Entry::minTier),
                Codec.intRange(1, MAX_TIER).optionalFieldOf("max_tier", MAX_TIER).forGetter(Entry::maxTier),
                Codec.BOOL.optionalFieldOf("required", true).forGetter(Entry::required)
        ).apply(instance, Entry::new)).validate(entry -> entry.minTier() <= entry.maxTier()
                ? DataResult.success(entry) : DataResult.error(() -> "min_tier is above max_tier"));

        boolean covers(int tier) { return tier >= minTier && tier <= maxTier; }
    }

    public static final Codec<SpawnTable> CODEC = RecordCodecBuilder.<SpawnTable>create(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("entries").forGetter(SpawnTable::entries)
    ).apply(instance, SpawnTable::new)).validate(table -> !table.entries().isEmpty() && table.entries().size() <= 64
            ? DataResult.success(table) : DataResult.error(() -> "A spawn table needs 1 to 64 entries"));

    public SpawnTable {
        entries = List.copyOf(entries);
    }

    /**
     * The entries a spawner of {@code tier} may use right now: in its tier range and registered.
     * Unregistered optional entries drop out here.
     */
    public List<Entry> available(int tier, Predicate<String> registered) {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.covers(tier) && registered.test(entry.entity())) result.add(entry);
        }
        return result;
    }

    /** A weighted pick among {@link #available}; empty when nothing fits. {@code nextInt} is {@code bound -> [0, bound)}. */
    public Optional<String> pick(int tier, Predicate<String> registered, IntUnaryOperator nextInt) {
        List<Entry> candidates = available(tier, registered);
        int total = 0;
        for (Entry entry : candidates) total += entry.weight();
        if (total == 0) return Optional.empty();
        int roll = nextInt.applyAsInt(total);
        for (Entry entry : candidates) {
            roll -= entry.weight();
            if (roll < 0) return Optional.of(entry.entity());
        }
        return Optional.empty();
    }

    public static DataResult<SpawnTable> parse(Reader reader) {
        try {
            return CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader));
        } catch (RuntimeException exception) {
            return DataResult.error(() -> "Malformed spawn table JSON");
        }
    }

    private static final Map<ResourceManager, Map<Identifier, Optional<SpawnTable>>> CACHE = new WeakHashMap<>();

    /**
     * The table {@code id} from the server's current data packs, cached per resource manager so a
     * {@code /reload} picks up changes. A missing or invalid table yields empty and is reported once.
     */
    public static Optional<SpawnTable> get(MinecraftServer server, Identifier id) {
        ResourceManager manager = server.getResourceManager();
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(manager, ignored -> new HashMap<>()).computeIfAbsent(id, key -> load(manager, key));
        }
    }

    private static Optional<SpawnTable> load(ResourceManager manager, Identifier id) {
        Identifier file = Identifier.fromNamespaceAndPath(id.getNamespace(), "spawn_tables/" + id.getPath() + ".json");
        Optional<Resource> resource = manager.getResource(file);
        if (resource.isEmpty()) {
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] Spawn table {} is missing; its spawners stay idle", id);
            return Optional.empty();
        }
        try (Reader reader = resource.get().openAsReader()) {
            DataResult<SpawnTable> result = parse(reader);
            if (result.error().isPresent()) {
                NeroRuinsCommon.LOGGER.warn("[NeroRuins] Spawn table {} is invalid; its spawners stay idle", id);
            }
            return result.result();
        } catch (java.io.IOException exception) {
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] Spawn table {} could not be read; its spawners stay idle", id);
            return Optional.empty();
        }
    }
}
