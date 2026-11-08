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
    public static final RegistrationProvider.RegistryEntry<BlockItem> LATTICEWORK =
            ITEMS.register("latticework", key -> new BlockItem(ModBlocks.LATTICEWORK.get(), PropertiesCompat.item(key)));
    public static final RegistrationProvider.RegistryEntry<BlockItem> LATTICE_FLOOR =
            ITEMS.register("lattice_floor", key -> new BlockItem(ModBlocks.LATTICE_FLOOR.get(), PropertiesCompat.item(key)));
    public static final RegistrationProvider.RegistryEntry<BlockItem> RESONANCE_CRYSTAL_BLOCK =
            ITEMS.register("resonance_crystal_block", key -> new BlockItem(ModBlocks.RESONANCE_CRYSTAL_BLOCK.get(), PropertiesCompat.item(key)));
    public static final RegistrationProvider.RegistryEntry<BlockItem> GLYPH_STONE =
            ITEMS.register("glyph_stone", key -> new BlockItem(ModBlocks.GLYPH_STONE.get(), PropertiesCompat.item(key)));
    public static final RegistrationProvider.RegistryEntry<BlockItem> BALLAST_PLATE =
            ITEMS.register("ballast_plate", key -> new BlockItem(ModBlocks.BALLAST_PLATE.get(), PropertiesCompat.item(key)));
    public static final RegistrationProvider.RegistryEntry<Item> EXPEDITION_MAP =
            ITEMS.register("expedition_map", key -> new za.co.neroland.neroruins.item.ExpeditionMapItem(
                    PropertiesCompat.item(key).stacksTo(1)));
    public static final RegistrationProvider.RegistryEntry<Item> SURVEY_SCANNER =
            ITEMS.register("survey_scanner", key -> new za.co.neroland.neroruins.item.SurveyScannerItem(
                    PropertiesCompat.item(key).stacksTo(1)));
    public static final RegistrationProvider.RegistryEntry<BlockItem> RUIN_SPAWNER =
            ITEMS.register("ruin_spawner", key -> new BlockItem(ModBlocks.RUIN_SPAWNER.get(), PropertiesCompat.item(key)));
    public static final RegistrationProvider.RegistryEntry<BlockItem> ARC_GRATE =
            ITEMS.register("arc_grate", key -> new BlockItem(ModBlocks.ARC_GRATE.get(), PropertiesCompat.item(key)));
    /** Sentinel spawn eggs, in mark order. */
    public static final List<RegistrationProvider.RegistryEntry<Item>> SENTINEL_EGGS = List.of(
            ITEMS.register("sentinel_mk1_spawn_egg", key -> new za.co.neroland.neroruins.guardian.SentinelSpawnEggItem(
                    PropertiesCompat.item(key), ModEntities.SENTINEL_MK1)),
            ITEMS.register("sentinel_mk2_spawn_egg", key -> new za.co.neroland.neroruins.guardian.SentinelSpawnEggItem(
                    PropertiesCompat.item(key), ModEntities.SENTINEL_MK2)),
            ITEMS.register("sentinel_mk3_spawn_egg", key -> new za.co.neroland.neroruins.guardian.SentinelSpawnEggItem(
                    PropertiesCompat.item(key), ModEntities.SENTINEL_MK3)));

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

    // Puzzle fixtures and keycards (decision 032).
    public static final RegistrationProvider.RegistryEntry<BlockItem> SEALED_DOOR = blockItem("sealed_door", ModBlocks.SEALED_DOOR);
    public static final RegistrationProvider.RegistryEntry<BlockItem> GLYPH_CLUE = blockItem("glyph_clue", ModBlocks.GLYPH_CLUE);
    public static final RegistrationProvider.RegistryEntry<BlockItem> GLYPH_LOCK = blockItem("glyph_lock", ModBlocks.GLYPH_LOCK);
    public static final RegistrationProvider.RegistryEntry<BlockItem> CONDUIT = blockItem("conduit", ModBlocks.CONDUIT);
    public static final RegistrationProvider.RegistryEntry<BlockItem> CONDUIT_SOURCE = blockItem("conduit_source", ModBlocks.CONDUIT_SOURCE);
    public static final RegistrationProvider.RegistryEntry<BlockItem> CONDUIT_LOCK = blockItem("conduit_lock", ModBlocks.CONDUIT_LOCK);
    public static final RegistrationProvider.RegistryEntry<BlockItem> RESONANCE_PYLON = blockItem("resonance_pylon", ModBlocks.RESONANCE_PYLON);
    public static final RegistrationProvider.RegistryEntry<BlockItem> RESONANCE_CLUE = blockItem("resonance_clue", ModBlocks.RESONANCE_CLUE);
    public static final RegistrationProvider.RegistryEntry<BlockItem> CARD_READER = blockItem("card_reader", ModBlocks.CARD_READER);
    public static final RegistrationProvider.RegistryEntry<BlockItem> RELIC_SOCKET = blockItem("relic_socket", ModBlocks.RELIC_SOCKET);
    /** Keycards by tier: survey, security, command. */
    public static final List<RegistrationProvider.RegistryEntry<Item>> KEYCARDS = List.of(
            keycard("survey_keycard", 1), keycard("security_keycard", 2), keycard("command_keycard", 3));

    public static final List<RegistrationProvider.RegistryEntry<BlockItem>> REINFORCED = List.of(
            blockItem("reinforced_survey_hull", ModBlocks.REINFORCED_SURVEY_HULL),
            blockItem("reinforced_rusted_survey_hull", ModBlocks.REINFORCED_RUSTED_SURVEY_HULL),
            blockItem("reinforced_latticework", ModBlocks.REINFORCED_LATTICEWORK),
            blockItem("reinforced_lattice_floor", ModBlocks.REINFORCED_LATTICE_FLOOR));

    private ModItems() { }

    private static RegistrationProvider.RegistryEntry<BlockItem> blockItem(String name,
            RegistrationProvider.RegistryEntry<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, key -> new BlockItem(block.get(), PropertiesCompat.item(key)));
    }

    private static RegistrationProvider.RegistryEntry<Item> keycard(String name, int tier) {
        return ITEMS.register(name, key -> new za.co.neroland.neroruins.puzzle.KeycardItem(tier,
                PropertiesCompat.item(key).stacksTo(1).rarity(tier == 3 ? Rarity.RARE : Rarity.UNCOMMON)));
    }

    private static RegistrationProvider.RegistryEntry<Item> relic(String name) {
        return ITEMS.register(name, key -> new RelicItem(PropertiesCompat.item(key).stacksTo(1).rarity(Rarity.EPIC)));
    }

    public static void init() { }
}
