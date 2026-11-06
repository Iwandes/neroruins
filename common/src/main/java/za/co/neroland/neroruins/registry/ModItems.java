package za.co.neroland.neroruins.registry;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.PropertiesCompat;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.item.DataShardItem;
import za.co.neroland.neroruins.item.RelicItem;
import za.co.neroland.neroruins.lore.LogCatalog;
import za.co.neroland.neroruins.platform.Services;

public final class ModItems {
    private static final RegistrationProvider<DataComponentType<?>> COMPONENTS =
            Services.REGISTRATION.provider(Registries.DATA_COMPONENT_TYPE, NeroRuinsCommon.MOD_ID);
    private static final RegistrationProvider<Item> ITEMS =
            Services.REGISTRATION.provider(Registries.ITEM, NeroRuinsCommon.MOD_ID);

    public static final RegistrationProvider.RegistryEntry<DataComponentType<String>> LOG_ID =
            COMPONENTS.register("log_id", key -> DataComponentType.<String>builder().persistent(
                    Codec.STRING.validate(id -> LogCatalog.translationPrefix(id).isPresent()
                            ? DataResult.success(id) : DataResult.error(() -> "Unknown data-log id"))).build());
    public static final RegistrationProvider.RegistryEntry<Item> DATA_SHARD =
            ITEMS.register("data_shard", key -> new DataShardItem(PropertiesCompat.item(key).stacksTo(1)));
    public static final RegistrationProvider.RegistryEntry<BlockItem> SURVEY_HULL =
            ITEMS.register("survey_hull", key -> new BlockItem(ModBlocks.SURVEY_HULL.get(), PropertiesCompat.item(key)));
    public static final RegistrationProvider.RegistryEntry<BlockItem> RUSTED_SURVEY_HULL =
            ITEMS.register("rusted_survey_hull", key -> new BlockItem(ModBlocks.RUSTED_SURVEY_HULL.get(), PropertiesCompat.item(key)));
    public static final RegistrationProvider.RegistryEntry<BlockItem> LOG_TERMINAL =
            ITEMS.register("log_terminal", key -> new BlockItem(ModBlocks.LOG_TERMINAL.get(), PropertiesCompat.item(key)));
    public static final RegistrationProvider.RegistryEntry<BlockItem> VAULT_CACHE =
            ITEMS.register("vault_cache", key -> new BlockItem(ModBlocks.VAULT_CACHE.get(), PropertiesCompat.item(key)));
    public static final RegistrationProvider.RegistryEntry<BlockItem> BALLAST_PLATE =
            ITEMS.register("ballast_plate", key -> new BlockItem(ModBlocks.BALLAST_PLATE.get(), PropertiesCompat.item(key)));

    /** The nine chapter relics, in chapter order (Earth to the Lattice's last room). */
    public static final List<RegistrationProvider.RegistryEntry<Item>> RELICS = List.of(
            relic("surveyors_compass"),
            relic("pale_survey_lens"),
            relic("directors_seal"),
            relic("field_recorder"),
            relic("meridian_keystone"),
            relic("reach_thermal_core"),
            relic("rootglass_vessel"),
            relic("twilight_prism"),
            relic("resonance_heart"));

    private ModItems() { }

    private static RegistrationProvider.RegistryEntry<Item> relic(String name) {
        return ITEMS.register(name, key -> new RelicItem(PropertiesCompat.item(key).stacksTo(1).rarity(Rarity.EPIC)));
    }

    public static void init() { }
}
