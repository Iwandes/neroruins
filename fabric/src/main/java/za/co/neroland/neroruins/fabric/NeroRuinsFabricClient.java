package za.co.neroland.neroruins.fabric;

import net.fabricmc.api.ClientModInitializer;

import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.client.CodexClient;

/** Fabric client entry point for NeroRuins. */
public final class NeroRuinsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NeroRuinsCommon.LOGGER.info("[NeroRuins] Fabric client bootstrap");
        CodexClient.init();
        net.minecraft.client.gui.screens.MenuScreens.register(
                za.co.neroland.neroruins.registry.ModMenuTypes.LOG_TERMINAL.get(),
                za.co.neroland.neroruins.client.LogTerminalScreen::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                za.co.neroland.neroruins.registry.ModBlockEntities.LOG_TERMINAL.get(),
                context -> new za.co.neroland.neroruins.client.LogTerminalRenderer());
        net.minecraft.client.gui.screens.MenuScreens.register(
                za.co.neroland.neroruins.registry.ModMenuTypes.VAULT_CACHE.get(),
                za.co.neroland.neroruins.client.VaultCacheScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(
                za.co.neroland.neroruins.registry.ModMenuTypes.GLYPH_LOCK.get(),
                za.co.neroland.neroruins.client.GlyphLockScreen::new);
        za.co.neroland.neroruins.client.GuardianRenderers.registerAll(new za.co.neroland.neroruins.client.GuardianRenderers.Sink() {
            @Override
            public <E extends net.minecraft.world.entity.Entity> void register(net.minecraft.world.entity.EntityType<? extends E> type,
                    net.minecraft.client.renderer.entity.EntityRendererProvider<E> provider) {
                net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(type, provider);
            }
        });
        FabricRuinsNetwork.registerClient();
    }
}
