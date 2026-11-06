package za.co.neroland.neroruins.forge;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.command.CodexCommands;
import za.co.neroland.neroruins.client.CodexClient;
import za.co.neroland.neroruins.platform.Services;
import za.co.neroland.neroruins.lifecycle.ServerState;
import za.co.neroland.neroruins.progression.QuestHooks;

/** MinecraftForge entry point for NeroRuins. */
@Mod(NeroRuinsCommon.MOD_ID)
public final class NeroRuinsForge {

    public NeroRuinsForge(FMLJavaModLoadingContext context) {
        NeroRuinsCommon.LOGGER.info("[NeroRuins] Forge bootstrap");
        NeroRuinsCommon.init();
        if (Services.PLATFORM.isClient()) {
            CodexClient.init();
            //? if >=1.21.6 {
            ForgeClientSetup.init(context.getModBusGroup());
            //?} else {
            /*ForgeClientSetup.init(context.getModEventBus());
            *///?}
        }
        //? if >=1.21.6 {
        Services.REGISTRATION.attach(context.getModBusGroup());
        //?} else {
        /*Services.REGISTRATION.attach(context.getModEventBus());
        *///?}
        ForgeRuinsNetwork.register();

        // Server lifecycle + player join, forwarded to the loader-agnostic seams in common.
        //? if >=1.21.6 {
        RegisterCommandsEvent.BUS.addListener(event -> CodexCommands.register(event.getDispatcher()));
        ServerStartedEvent.BUS.addListener(event -> ServerState.serverStarted(event.getServer()));
        ServerStoppedEvent.BUS.addListener(event -> ServerState.serverStopped());
        //? if >=1.21.9 {
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(event ->
                za.co.neroland.neroruins.expedition.ExpeditionTracker.tick(event.server()));
        //?} else {
        /*net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(event ->
                za.co.neroland.neroruins.expedition.ExpeditionTracker.tick(event.getServer()));
        *///?}
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                QuestHooks.onJoin(player);
            }
        });
        //?} else {
        /*// Forge before 1.21.6 runs EventBus 6: one game bus, listeners typed by their parameter.
        net.minecraftforge.eventbus.api.IEventBus game = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        game.addListener((RegisterCommandsEvent event) -> CodexCommands.register(event.getDispatcher()));
        game.addListener((ServerStartedEvent event) -> ServerState.serverStarted(event.getServer()));
        game.addListener((ServerStoppedEvent event) -> ServerState.serverStopped());
        game.addListener((net.minecraftforge.event.TickEvent.ServerTickEvent.Post event) ->
                za.co.neroland.neroruins.expedition.ExpeditionTracker.tick(event.getServer()));
        game.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                QuestHooks.onJoin(player);
            }
        });
        *///?}
    }
}
