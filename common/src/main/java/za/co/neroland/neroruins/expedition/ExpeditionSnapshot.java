package za.co.neroland.neroruins.expedition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.data.ExpeditionRecord;

/**
 * The requesting player's own discoveries, newest first, sent only to that player when they use an Expedition
 * Map. A discovery whose position was dropped by retention or never kept is sent without coordinates.
 */
public record ExpeditionSnapshot(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<ExpeditionSnapshot> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NeroRuinsCommon.MOD_ID, "expedition_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExpeditionSnapshot> CODEC =
            StreamCodec.of(ExpeditionSnapshot::write, ExpeditionSnapshot::read);
    public static final int MAX_ENTRIES = 256;
    private static final int MAX_ID_LENGTH = 128;

    public record Entry(String family, String sizeClass, String dimension, boolean positioned, int x, int z) {
        public Entry {
            if (family == null || !family.matches("[a-z0-9_]{1,64}") || sizeClass == null
                    || !(sizeClass.equals("outpost") || sizeClass.equals("complex") || sizeClass.equals("citadel"))
                    || dimension == null || dimension.length() > MAX_ID_LENGTH
                    || !dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || (!positioned && (x != 0 || z != 0))) {
                throw new IllegalArgumentException("Invalid expedition entry");
            }
        }
    }

    public ExpeditionSnapshot {
        entries = List.copyOf(entries);
        if (entries.size() > MAX_ENTRIES) throw new IllegalArgumentException("Too many expedition entries");
    }

    public static ExpeditionSnapshot of(List<ExpeditionRecord.Discovery> discoveries) {
        return new ExpeditionSnapshot(discoveries.stream()
                .sorted(Comparator.comparingLong(ExpeditionRecord.Discovery::firstSeen).reversed())
                .limit(MAX_ENTRIES)
                .map(discovery -> discovery.hub()
                        .map(hub -> new Entry(discovery.family(), discovery.sizeClass(), discovery.dimension(),
                                true, hub.x(), hub.z()))
                        .orElseGet(() -> new Entry(discovery.family(), discovery.sizeClass(),
                                discovery.dimension(), false, 0, 0)))
                .toList());
    }

    private static void write(RegistryFriendlyByteBuf buf, ExpeditionSnapshot snapshot) {
        buf.writeVarInt(snapshot.entries.size());
        for (Entry entry : snapshot.entries) {
            buf.writeUtf(entry.family(), 64);
            buf.writeUtf(entry.sizeClass(), 16);
            buf.writeUtf(entry.dimension(), MAX_ID_LENGTH);
            buf.writeBoolean(entry.positioned());
            if (entry.positioned()) {
                buf.writeVarInt(entry.x());
                buf.writeVarInt(entry.z());
            }
        }
    }

    private static ExpeditionSnapshot read(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("Invalid expedition entry count");
        List<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String family = buf.readUtf(64);
            String sizeClass = buf.readUtf(16);
            String dimension = buf.readUtf(MAX_ID_LENGTH);
            boolean positioned = buf.readBoolean();
            int x = positioned ? buf.readVarInt() : 0;
            int z = positioned ? buf.readVarInt() : 0;
            entries.add(new Entry(family, sizeClass, dimension, positioned, x, z));
        }
        return new ExpeditionSnapshot(entries);
    }

    @Override
    public Type<ExpeditionSnapshot> type() { return TYPE; }
}
