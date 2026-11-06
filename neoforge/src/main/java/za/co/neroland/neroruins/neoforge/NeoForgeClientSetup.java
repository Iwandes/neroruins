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
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(
                za.co.neroland.neroruins.registry.ModBlockEntities.LOG_TERMINAL.get(),
                context -> new za.co.neroland.neroruins.client.LogTerminalRenderer()));
    }
}
