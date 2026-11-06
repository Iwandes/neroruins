package za.co.neroland.neroruins.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.lore.LogCatalog;

public record CodexSnapshot(List<String> logs) implements CustomPacketPayload {
    public static final Type<CodexSnapshot> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NeroRuinsCommon.MOD_ID, "codex_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CodexSnapshot> CODEC =
            StreamCodec.of(CodexSnapshot::write, CodexSnapshot::read);
    private static final int MAX_LOGS = 54;
    private static final int MAX_ID_LENGTH = 64;

    public CodexSnapshot {
        logs = List.copyOf(logs);
        if (logs.size() > MAX_LOGS || logs.stream().anyMatch(id -> LogCatalog.translationPrefix(id).isEmpty())
                || Set.copyOf(logs).size() != logs.size()) {
            throw new IllegalArgumentException("Invalid Codex snapshot");
        }
    }

    public static CodexSnapshot fromArchive(Set<String> savedLogs) {
        return new CodexSnapshot(LogCatalog.ids().stream().filter(savedLogs::contains).toList());
    }

    private static void write(RegistryFriendlyByteBuf buf, CodexSnapshot snapshot) {
        buf.writeVarInt(snapshot.logs.size());
        for (String log : snapshot.logs) buf.writeUtf(log, MAX_ID_LENGTH);
    }

    private static CodexSnapshot read(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_LOGS) throw new IllegalArgumentException("Invalid Codex entry count");
        List<String> logs = new ArrayList<>(count);
        for (int index = 0; index < count; index++) logs.add(buf.readUtf(MAX_ID_LENGTH));
        return new CodexSnapshot(logs);
    }

    @Override
    public Type<CodexSnapshot> type() { return TYPE; }
}
