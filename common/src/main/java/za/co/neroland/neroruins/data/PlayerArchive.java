package za.co.neroland.neroruins.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.io.IOException;
import java.io.UncheckedIOException;

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

public final class PlayerArchive extends SavedData {
    private static final String RECOVERY_NAME = "neroruins:player_archive";
    private static final Identifier ID = Identifier.fromNamespaceAndPath(NeroRuinsCommon.MOD_ID, "player_archive");
    private static final SavedDataType<PlayerArchive> TYPE = SavedDataCompat.type(ID, PlayerArchive::new, codec());

    private final Map<UUID, Map<String, Long>> entries = new LinkedHashMap<>();

    public static PlayerArchive get(MinecraftServer server) {
        return SavedDataRecovery.get(server.overworld(), TYPE, PlayerArchive::new, RECOVERY_NAME);
    }

    public boolean record(UUID player, String logId, long readAt) {
        Objects.requireNonNull(player, "player");
        if (!validLogId(logId) || readAt < 0) throw new IllegalArgumentException("Invalid archive entry");
        Map<String, Long> logs = entries.computeIfAbsent(player, ignored -> new LinkedHashMap<>());
        if (logs.putIfAbsent(logId, readAt) != null) return false;
        setDirty();
        return true;
    }

    public Map<String, Long> export(UUID player) {
        return Map.copyOf(entries.getOrDefault(Objects.requireNonNull(player, "player"), Map.of()));
    }

    public boolean contains(UUID player, String logId) {
        return entries.getOrDefault(Objects.requireNonNull(player, "player"), Map.of()).containsKey(logId);
    }

    public void erase(UUID player) {
        if (entries.remove(Objects.requireNonNull(player, "player")) != null) setDirty();
    }

    static RuinPlayerData.Store erasureStore() {
        return new RuinPlayerData.Store() {
            @Override
            public void erase(MinecraftServer server, UUID player) {
                PlayerArchive.get(server).erase(player);
            }

            @Override
            public void backupNow(MinecraftServer server, UUID player) {
                PlayerArchive archive = PlayerArchive.get(server);
                try {
                    SavedDataRecovery.backupNow(server.overworld(), TYPE, archive, RECOVERY_NAME);
                } catch (RuntimeException | Error failure) {
                    try {
                        deleteExport(server, player);
                    } catch (RuntimeException | Error cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                    throw failure;
                }
                deleteExport(server, player);
            }
        };
    }

    private static void deleteExport(MinecraftServer server, UUID player) {
        try {
            ArchiveExport.erase(server.getWorldPath(LevelResource.ROOT), player);
        } catch (IOException failure) {
            throw new UncheckedIOException("Could not delete archive export", failure);
        }
    }

    private static boolean validLogId(String id) {
        return id != null && id.length() <= 128 && id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
    }

    private record Entry(String player, String log, long readAt) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.<Entry>create(instance -> instance.group(
                Codec.STRING.fieldOf("player").forGetter(Entry::player),
                Codec.STRING.fieldOf("log").forGetter(Entry::log),
                Codec.LONG.fieldOf("read_at").forGetter(Entry::readAt)
        ).apply(instance, Entry::new)).validate(entry -> {
            try {
                UUID.fromString(entry.player);
            } catch (IllegalArgumentException failure) {
                return DataResult.error(() -> "Invalid archive player identifier");
            }
            return validLogId(entry.log) && entry.readAt >= 0
                    ? DataResult.success(entry) : DataResult.error(() -> "Invalid archive entry");
        });
    }

    public static Codec<PlayerArchive> codec() {
        return Entry.CODEC.listOf().fieldOf("entries").xmap(PlayerArchive::decode, PlayerArchive::encode).codec();
    }

    private static PlayerArchive decode(List<Entry> saved) {
        PlayerArchive archive = new PlayerArchive();
        for (Entry entry : saved) {
            archive.entries.computeIfAbsent(UUID.fromString(entry.player), ignored -> new LinkedHashMap<>())
                    .merge(entry.log, entry.readAt, Math::min);
        }
        return archive;
    }

    private List<Entry> encode() {
        List<Entry> saved = new ArrayList<>();
        entries.forEach((player, logs) -> logs.forEach((log, time) ->
                saved.add(new Entry(player.toString(), log, time))));
        return saved;
    }
}
