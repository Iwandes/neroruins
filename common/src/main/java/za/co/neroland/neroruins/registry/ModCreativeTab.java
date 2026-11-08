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
                        output.accept(ModItems.LATTICEWORK.get());
                        output.accept(ModItems.LATTICE_FLOOR.get());
                        output.accept(ModItems.RESONANCE_CRYSTAL_BLOCK.get());
                        output.accept(ModItems.GLYPH_STONE.get());
                        for (var fixture : java.util.List.of(ModItems.SEALED_DOOR, ModItems.GLYPH_CLUE, ModItems.GLYPH_LOCK,
                                ModItems.CONDUIT, ModItems.CONDUIT_SOURCE, ModItems.CONDUIT_LOCK, ModItems.RESONANCE_PYLON,
                                ModItems.RESONANCE_CLUE, ModItems.CARD_READER, ModItems.RELIC_SOCKET)) {
                            output.accept(fixture.get());
                        }
                        for (var shell : ModItems.REINFORCED) output.accept(shell.get());
                        for (var card : ModItems.KEYCARDS) output.accept(card.get());
                        output.accept(ModItems.RUIN_SPAWNER.get());
                        output.accept(ModItems.ARC_GRATE.get());
                        for (var egg : ModItems.SENTINEL_EGGS) {
                            output.accept(egg.get());
                        }
                        output.accept(ModItems.EXPEDITION_MAP.get());
                        output.accept(ModItems.SURVEY_SCANNER.get());
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
