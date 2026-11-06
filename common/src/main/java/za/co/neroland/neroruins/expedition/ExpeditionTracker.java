package za.co.neroland.neroruins.expedition;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.config.NeroRuinsConfig;
import za.co.neroland.neroruins.data.ClaimLedger;
import za.co.neroland.neroruins.data.ExpeditionRecord;
import za.co.neroland.neroruins.worldgen.structure.ComplexHub;

/**
 * Notices when a player first stands inside a ruin complex's hub and records it in that player's own
 * {@link ExpeditionRecord}. Credit is by the player's own position, never anyone else's. Also runs the hourly
 * retention sweeps. Driven by each loader's server-tick hook.
 */
public final class ExpeditionTracker {
    private static final int CHECK_INTERVAL = 40;
    private static final int RETENTION_INTERVAL = 72_000;
    private static int ticks;

    private ExpeditionTracker() { }

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks % RETENTION_INTERVAL == 0) applyRetention(server);
        if (ticks % CHECK_INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isSpectator()) check(player);
        }
    }

    /** World-start and hourly retention for the expedition record and claim ledger. Failures are reported. */
    public static void applyRetention(MinecraftServer server) {
        long now = System.currentTimeMillis();
        try {
            ExpeditionRecord.applyRetention(server, now, NeroRuinsConfig.EXPEDITION_RETENTION_DAYS.get(),
                    NeroRuinsConfig.RECORD_POSITIONS.get());
        } catch (RuntimeException failure) {
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] Expedition retention failed ({})", failure.getClass().getName());
        }
        try {
            ClaimLedger.applyRetention(server, now, NeroRuinsConfig.CLAIM_RETENTION_DAYS.get());
        } catch (RuntimeException failure) {
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] Claim retention failed ({})", failure.getClass().getName());
        }
    }

    private static void check(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        BlockPos pos = player.blockPosition();
        var manager = level.structureManager();
        for (Structure structure : manager.getAllStructuresAt(pos).keySet()) {
            if (!(structure instanceof ComplexHub hub)) continue;
            StructureStart start = manager.getStructureAt(pos, hub);
            if (!start.isValid()) continue;
            //? if >=1.21.2 {
            Identifier id = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(hub);
            //?} else {
            /*Identifier id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(hub);
            *///?}
            if (id == null) continue;
            BlockPos centre = start.getBoundingBox().getCenter();
            Optional<ExpeditionRecord.Hub> position = NeroRuinsConfig.RECORD_POSITIONS.get()
                    ? Optional.of(new ExpeditionRecord.Hub(centre.getX(), centre.getY(), centre.getZ()))
                    : Optional.empty();
            var discovery = new ExpeditionRecord.Discovery(id.toString(), hub.family(), hub.sizeClass(),
                    level.dimension().identifier().toString(), position, System.currentTimeMillis());
            if (ExpeditionRecord.get(level.getServer()).discover(player.getUUID(), discovery)) {
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(
                        "message.neroruins.discovered", Component.translatable("family.neroruins." + hub.family()))));
            }
        }
    }
}
