package za.co.neroland.neroruins.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import za.co.neroland.neroruins.client.LogTerminalScreen;
import za.co.neroland.neroruins.registry.ModMenuTypes;

public final class ForgeClientSetup {
    private ForgeClientSetup() { }
    //? if >=1.21.6 {
    public static void init(net.minecraftforge.eventbus.api.bus.BusGroup bus) {
        FMLClientSetupEvent.getBus(bus).addListener(ForgeClientSetup::setup);
        //? if >=1.21.9 {
        net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers.BUS.addListener(ForgeClientSetup::registerRenderers);
        //?} else {
        /*net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers.getBus(bus).addListener(ForgeClientSetup::registerRenderers);
        *///?}
    }
    //?} else {
    /*public static void init(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener(ForgeClientSetup::setup);
        bus.addListener(ForgeClientSetup::registerRenderers);
    }
    *///?}
    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.LOG_TERMINAL.get(), LogTerminalScreen::new);
            MenuScreens.register(ModMenuTypes.VAULT_CACHE.get(), za.co.neroland.neroruins.client.VaultCacheScreen::new);
            MenuScreens.register(ModMenuTypes.GLYPH_LOCK.get(), za.co.neroland.neroruins.client.GlyphLockScreen::new);
        });
    }

    private static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(za.co.neroland.neroruins.registry.ModBlockEntities.LOG_TERMINAL.get(),
                context -> new za.co.neroland.neroruins.client.LogTerminalRenderer());
        za.co.neroland.neroruins.client.GuardianRenderers.registerAll(new za.co.neroland.neroruins.client.GuardianRenderers.Sink() {
            @Override
            public <E extends net.minecraft.world.entity.Entity> void register(net.minecraft.world.entity.EntityType<? extends E> type,
                    net.minecraft.client.renderer.entity.EntityRendererProvider<E> provider) {
                event.registerEntityRenderer(type, provider);
            }
        });
    }
}
