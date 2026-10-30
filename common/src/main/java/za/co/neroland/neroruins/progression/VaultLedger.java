package za.co.neroland.neroruins.progression;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerolandcore.data.SavedDataCompat;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.neroruins.NeroRuinsCommon;

/**
 * The world's <b>vault ledger</b>: how many vaults of each structure type have been claimed in this
 * world, kept as one {@link SavedData} store in the overworld's data folder. It backs the
 * {@code neroruins:vault_claimed} threshold channel (see {@link RuinMilestones#vaultClaimed}).
 *
 * <p>Loaded through Neroland Core's {@link SavedDataRecovery}, so a corrupt or unreadable file degrades
 * to the last-known-good backup (or a fresh ledger) instead of crashing the tick loop.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> the ledger holds structure <em>type</em> ids and counts only — never a
 * player, a UUID or a position. There is nothing in it to erase per player.
 */
public final class VaultLedger extends SavedData {

    /** Saved-data id: {@code data/neroruins/vault_ledger.dat} in the overworld. */
    public static final Identifier ID = Identifier.fromNamespaceAndPath(NeroRuinsCommon.MOD_ID, "vault_ledger");
    private static final String NAME = ID.toString();

    public static final SavedDataType<VaultLedger> TYPE =
            SavedDataCompat.type(ID, VaultLedger::new, codec());

    /** structure type id (as a string) -> claimed count. */
    private final Map<String, Long> claimed = new LinkedHashMap<>();

    public VaultLedger() {
    }

    private VaultLedger(Map<String, Long> loaded) {
        claimed.putAll(loaded);
    }

    /** The world's ledger, loaded (or recovered) from the overworld's data storage. */
    public static VaultLedger get(MinecraftServer server) {
        return SavedDataRecovery.get(server.overworld(), TYPE, VaultLedger::new, NAME);
    }

    /** How many vaults of {@code structureType} have been claimed in this world. */
    public long count(Identifier structureType) {
        return claimed.getOrDefault(structureType.toString(), 0L);
    }

    /** Records one more claimed vault of {@code structureType} and returns the new count. */
    public long increment(Identifier structureType) {
        long next = count(structureType) + 1L;
        claimed.put(structureType.toString(), next);
        setDirty();
        return next;
    }

    /** Claimed vaults across every structure type. */
    public long total() {
        long sum = 0L;
        for (long value : claimed.values()) {
            sum += value;
        }
        return sum;
    }

    private static Codec<VaultLedger> codec() {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("claimed", Map.of())
                        .forGetter(ledger -> Map.copyOf(ledger.claimed))
        ).apply(instance, VaultLedger::new));
    }
}
