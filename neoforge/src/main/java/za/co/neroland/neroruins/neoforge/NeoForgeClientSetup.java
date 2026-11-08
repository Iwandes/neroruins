package za.co.neroland.neroruins.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import za.co.neroland.neroruins.client.LogTerminalScreen;
import za.co.neroland.neroruins.registry.ModMenuTypes;

public final class NeoForgeClientSetup {
    private NeoForgeClientSetup() { }
    public static void init(IEventBus bus) {
        bus.addListener((RegisterMenuScreensEvent event) -> event.register(ModMenuTypes.LOG_TERMINAL.get(), LogTerminalScreen::new));
        bus.addListener((RegisterMenuScreensEvent event) -> event.register(ModMenuTypes.VAULT_CACHE.get(),
                za.co.neroland.neroruins.client.VaultCacheScreen::new));
        bus.addListener((RegisterMenuScreensEvent event) -> event.register(ModMenuTypes.GLYPH_LOCK.get(),
                za.co.neroland.neroruins.client.GlyphLockScreen::new));
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(
                za.co.neroland.neroruins.registry.ModBlockEntities.LOG_TERMINAL.get(),
                context -> new za.co.neroland.neroruins.client.LogTerminalRenderer()));
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> za.co.neroland.neroruins.client.GuardianRenderers.registerAll(
                new za.co.neroland.neroruins.client.GuardianRenderers.Sink() {
                    @Override
                    public <E extends net.minecraft.world.entity.Entity> void register(net.minecraft.world.entity.EntityType<? extends E> type,
                            net.minecraft.client.renderer.entity.EntityRendererProvider<E> provider) {
                        event.registerEntityRenderer(type, provider);
                    }
                }));
    }
}
