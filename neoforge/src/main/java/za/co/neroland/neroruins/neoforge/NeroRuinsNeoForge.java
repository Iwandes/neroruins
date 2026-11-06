package za.co.neroland.neroruins.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.command.CodexCommands;
import za.co.neroland.neroruins.client.CodexClient;
import za.co.neroland.neroruins.platform.Services;
import za.co.neroland.neroruins.lifecycle.ServerState;
import za.co.neroland.neroruins.progression.QuestHooks;

/** NeoForge entry point for NeroRuins. */
@Mod(NeroRuinsCommon.MOD_ID)
public final class NeroRuinsNeoForge {

    public NeroRuinsNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NeroRuinsCommon.LOGGER.info("[NeroRuins] NeoForge bootstrap");
        NeroRuinsCommon.init();
        if (Services.PLATFORM.isClient()) {
            CodexClient.init();
            NeoForgeClientSetup.init(modEventBus);
        }
        Services.REGISTRATION.attach(modEventBus);
        NeoForgeRuinsNetwork.register(modEventBus);

        // Server lifecycle + player join, forwarded to the loader-agnostic seams in common.
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> CodexCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> ServerState.serverStarted(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> ServerState.serverStopped());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) ->
                za.co.neroland.neroruins.expedition.ExpeditionTracker.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                QuestHooks.onJoin(player);
            }
        });
    }
}
