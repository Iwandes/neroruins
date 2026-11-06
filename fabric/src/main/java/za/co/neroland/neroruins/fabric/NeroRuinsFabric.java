package za.co.neroland.neroruins.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.command.CodexCommands;
import za.co.neroland.neroruins.lifecycle.ServerState;
import za.co.neroland.neroruins.progression.QuestHooks;

/** Fabric entry point for NeroRuins. */
public final class NeroRuinsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NeroRuinsCommon.LOGGER.info("[NeroRuins] Fabric bootstrap");
        NeroRuinsCommon.init();
        FabricRuinsNetwork.registerCommon();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CodexCommands.register(dispatcher));

        // Server lifecycle + player join, forwarded to the loader-agnostic seams in common.
        ServerLifecycleEvents.SERVER_STARTED.register(ServerState::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerState.serverStopped());
        ServerTickEvents.END_SERVER_TICK.register(za.co.neroland.neroruins.expedition.ExpeditionTracker::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> QuestHooks.onJoin(handler.player));
    }
}
