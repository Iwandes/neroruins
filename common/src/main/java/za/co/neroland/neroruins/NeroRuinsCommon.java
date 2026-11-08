package za.co.neroland.neroruins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import za.co.neroland.neroruins.config.NeroRuinsConfig;
import za.co.neroland.neroruins.data.RuinPlayerData;
import za.co.neroland.neroruins.platform.Services;
import za.co.neroland.neroruins.network.RuinsNetwork;
import za.co.neroland.neroruins.progression.QuestHooks;
import za.co.neroland.neroruins.registry.ModCreativeTab;
import za.co.neroland.neroruins.registry.ModBlocks;
import za.co.neroland.neroruins.registry.ModBlockEntities;
import za.co.neroland.neroruins.registry.ModMenuTypes;
import za.co.neroland.neroruins.registry.ModItems;
import za.co.neroland.neroruins.registry.ModLootConditions;
import za.co.neroland.neroruins.registry.ModWorldgen;
import za.co.neroland.neroruins.telemetry.NeroRuinsTelemetry;

/**
 * Loader-agnostic entry point for NeroRuins. Each loader entry point
 * (Fabric / Forge / NeoForge) calls {@link #init()} once during mod
 * construction. Shared systems reach loader-specific behaviour through platform seams.
 */
public final class NeroRuinsCommon {

    public static final String MOD_ID = "neroruins";
    public static final Logger LOGGER = LoggerFactory.getLogger("NeroRuins");

    private NeroRuinsCommon() {
    }

    /** Called once per loader during mod construction. */
    public static void init() {
        LOGGER.info("[NeroRuins] common init");

        // 0. Platform seam, resolved here during construction and never lazily on a tick path — a
        //    late ServiceLoader read can throw ServiceConfigurationError out of gameplay code.
        Services.init();

        // 1. Config first: everything below reads it, including telemetry's opt-out flag.
        NeroRuinsConfig.init();

        // 2. Anonymous, NeroRuins-only crash reporting. Must follow the config registration and
        //    precede the rest of init so early failures are still reported. On by default (opt-out via
        //    telemetryEnabled=false).
        NeroRuinsTelemetry.init();

        ModBlocks.init();
        ModBlockEntities.init();
        za.co.neroland.neroruins.registry.ModEntities.init();
        ModMenuTypes.init();
        ModItems.init();
        ModCreativeTab.init();
        ModLootConditions.init();
        ModWorldgen.init();
        RuinsNetwork.init();
        // Entity attributes go through Core's seam after the types exist (Fabric applies them at once).
        za.co.neroland.neroruins.registry.ModEntities.registerEntitySupport();

        RuinPlayerData.init();

        // 7. Quest-hook listener on Neroland Core's gate events (the `mod_hook` rewards of the
        //    Expedition Notes quest pack). Pure Core contract: no quest mod is imported. Each loader's
        //    entry point also forwards server start/stop (lifecycle.ServerState) and player join
        //    (QuestHooks.onJoin) so a hook that opened while a player was offline is delivered.
        QuestHooks.init();
    }
}
