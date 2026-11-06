package za.co.neroland.neroruins.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.platform.Services;
import za.co.neroland.neroruins.lore.LogCatalog;

public final class ModCreativeTab {
    private static final RegistrationProvider<CreativeModeTab> TABS =
            Services.REGISTRATION.provider(Registries.CREATIVE_MODE_TAB, NeroRuinsCommon.MOD_ID);

    public static final RegistrationProvider.RegistryEntry<CreativeModeTab> TAB = TABS.register("neroruins",
            key -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.neroruins"))
                    .icon(() -> new ItemStack(Items.CHISELED_STONE_BRICKS))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SURVEY_HULL.get());
                        output.accept(ModItems.RUSTED_SURVEY_HULL.get());
                        output.accept(ModItems.LOG_TERMINAL.get());
                        output.accept(ModItems.VAULT_CACHE.get());
                        output.accept(ModItems.BALLAST_PLATE.get());
                        for (var relic : ModItems.RELICS) {
                            output.accept(relic.get());
                        }
                        for (String id : LogCatalog.ids()) {
                            ItemStack shard = new ItemStack(ModItems.DATA_SHARD.get());
                            shard.set(ModItems.LOG_ID.get(), id);
                            output.accept(shard);
                        }
                    })
                    .build());

    private ModCreativeTab() { }

    public static void init() { }
}
