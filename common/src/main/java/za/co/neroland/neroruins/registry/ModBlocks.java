package za.co.neroland.neroruins.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import za.co.neroland.nerolandcore.registry.PropertiesCompat;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.platform.Services;
import za.co.neroland.neroruins.puzzle.BallastPlateBlock;
import za.co.neroland.neroruins.lattice.GlyphStoneBlock;
import za.co.neroland.neroruins.terminal.LogTerminalBlock;
import za.co.neroland.neroruins.vault.VaultCacheBlock;

public final class ModBlocks {
    private static final RegistrationProvider<Block> BLOCKS =
            Services.REGISTRATION.provider(Registries.BLOCK, NeroRuinsCommon.MOD_ID);

    public static final RegistrationProvider.RegistryEntry<Block> SURVEY_HULL = hull("survey_hull");
    public static final RegistrationProvider.RegistryEntry<Block> RUSTED_SURVEY_HULL = hull("rusted_survey_hull");
    public static final RegistrationProvider.RegistryEntry<LogTerminalBlock> LOG_TERMINAL =
            BLOCKS.register("log_terminal", key -> new LogTerminalBlock(PropertiesCompat.block(key)
                    .mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistrationProvider.RegistryEntry<VaultCacheBlock> VAULT_CACHE =
            BLOCKS.register("vault_cache", key -> new VaultCacheBlock(PropertiesCompat.block(key)
                    .mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).sound(SoundType.METAL)));

    public static final RegistrationProvider.RegistryEntry<BallastPlateBlock> BALLAST_PLATE =
            BLOCKS.register("ballast_plate", key -> new BallastPlateBlock(PropertiesCompat.block(key)
                    .mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).sound(SoundType.METAL).noOcclusion()));

    // Architect "Lattice" palette: hard but mineable (only puzzle and vault fixtures are unbreakable).
    public static final RegistrationProvider.RegistryEntry<Block> LATTICEWORK = lattice("latticework", 0);
    public static final RegistrationProvider.RegistryEntry<Block> LATTICE_FLOOR = lattice("lattice_floor", 0);
    public static final RegistrationProvider.RegistryEntry<Block> RESONANCE_CRYSTAL_BLOCK = lattice("resonance_crystal_block", 7);
    public static final RegistrationProvider.RegistryEntry<GlyphStoneBlock> GLYPH_STONE =
            BLOCKS.register("glyph_stone", key -> new GlyphStoneBlock(PropertiesCompat.block(key)
                    .mapColor(MapColor.COLOR_BLACK).strength(5.0F, 9.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE_BRICKS).lightLevel(state -> 5)));

    // Guardians: the spawner is an unbreakable fixture; the arc grate is a mineable floor trap.
    public static final RegistrationProvider.RegistryEntry<za.co.neroland.neroruins.guardian.RuinSpawnerBlock> RUIN_SPAWNER =
            BLOCKS.register("ruin_spawner", key -> new za.co.neroland.neroruins.guardian.RuinSpawnerBlock(PropertiesCompat.block(key)
                    .mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(za.co.neroland.neroruins.guardian.RuinSpawnerBlock.DORMANT) ? 0 : 6)));
    public static final RegistrationProvider.RegistryEntry<za.co.neroland.neroruins.guardian.ArcGrateBlock> ARC_GRATE =
            BLOCKS.register("arc_grate", key -> new za.co.neroland.neroruins.guardian.ArcGrateBlock(PropertiesCompat.block(key)
                    .mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(za.co.neroland.neroruins.guardian.ArcGrateBlock.CHARGED) ? 4 : 0)));

    // Puzzle fixtures (decision 032): unbreakable so a puzzle cannot be skipped by mining.
    public static final RegistrationProvider.RegistryEntry<za.co.neroland.neroruins.puzzle.SealedDoorBlock> SEALED_DOOR =
            BLOCKS.register("sealed_door", key -> new za.co.neroland.neroruins.puzzle.SealedDoorBlock(fixture(key)));
    public static final RegistrationProvider.RegistryEntry<za.co.neroland.neroruins.puzzle.GlyphClueBlock> GLYPH_CLUE =
            BLOCKS.register("glyph_clue", key -> new za.co.neroland.neroruins.puzzle.GlyphClueBlock(fixture(key)
                    .lightLevel(state -> 5)));
    public static final RegistrationProvider.RegistryEntry<za.co.neroland.neroruins.puzzle.GlyphLockBlock> GLYPH_LOCK =
            BLOCKS.register("glyph_lock", key -> new za.co.neroland.neroruins.puzzle.GlyphLockBlock(fixture(key)
                    .lightLevel(state -> 4)));
    public static final RegistrationProvider.RegistryEntry<za.co.neroland.neroruins.puzzle.ConduitBlock> CONDUIT =
            BLOCKS.register("conduit", key -> new za.co.neroland.neroruins.puzzle.ConduitBlock(fixture(key).noOcclusion()
                    .lightLevel(state -> state.getValue(za.co.neroland.neroruins.puzzle.ConduitBlock.POWERED) ? 6 : 0)));
    public static final RegistrationProvider.RegistryEntry<za.co.neroland.neroruins.puzzle.ConduitSourceBlock> CONDUIT_SOURCE =
            BLOCKS.register("conduit_source", key -> new za.co.neroland.neroruins.puzzle.ConduitSourceBlock(fixture(key)
                    .noOcclusion().lightLevel(state -> 8)));
    public static final RegistrationProvider.RegistryEntry<za.co.neroland.neroruins.puzzle.ConduitLockBlock> CONDUIT_LOCK =
            BLOCKS.register("conduit_lock", key -> new za.co.neroland.neroruins.puzzle.ConduitLockBlock(fixture(key)
                    .noOcclusion().lightLevel(state -> state.getValue(za.co.neroland.neroruins.puzzle.ConduitLockBlock.POWERED) ? 10 : 2)));
    public static final RegistrationProvider.RegistryEntry<za.co.neroland.neroruins.puzzle.ResonancePylonBlock> RESONANCE_PYLON =
            BLOCKS.register("resonance_pylon", key -> new za.co.neroland.neroruins.puzzle.ResonancePylonBlock(fixture(key)
                    .noOcclusion().lightLevel(state -> 6)));
    public static final RegistrationProvider.RegistryEntry<za.co.neroland.neroruins.puzzle.ResonanceClueBlock> RESONANCE_CLUE =
            BLOCKS.register("resonance_clue", key -> new za.co.neroland.neroruins.puzzle.ResonanceClueBlock(fixture(key)
                    .lightLevel(state -> 9)));
    public static final RegistrationProvider.RegistryEntry<za.co.neroland.neroruins.puzzle.CardReaderBlock> CARD_READER =
            BLOCKS.register("card_reader", key -> new za.co.neroland.neroruins.puzzle.CardReaderBlock(fixture(key)
                    .lightLevel(state -> 3)));
    public static final RegistrationProvider.RegistryEntry<za.co.neroland.neroruins.puzzle.RelicSocketBlock> RELIC_SOCKET =
            BLOCKS.register("relic_socket", key -> new za.co.neroland.neroruins.puzzle.RelicSocketBlock(fixture(key)
                    .lightLevel(state -> state.getValue(za.co.neroland.neroruins.puzzle.RelicSocketBlock.LIT) ? 12 : 3)));

    // Reinforced shells (decision 039): enclose every room behind a puzzle gate so it cannot be mined into.
    public static final RegistrationProvider.RegistryEntry<Block> REINFORCED_SURVEY_HULL =
            reinforced("reinforced_survey_hull", MapColor.METAL, SoundType.METAL);
    public static final RegistrationProvider.RegistryEntry<Block> REINFORCED_RUSTED_SURVEY_HULL =
            reinforced("reinforced_rusted_survey_hull", MapColor.METAL, SoundType.METAL);
    public static final RegistrationProvider.RegistryEntry<Block> REINFORCED_LATTICEWORK =
            reinforced("reinforced_latticework", MapColor.COLOR_BLACK, SoundType.DEEPSLATE_BRICKS);
    public static final RegistrationProvider.RegistryEntry<Block> REINFORCED_LATTICE_FLOOR =
            reinforced("reinforced_lattice_floor", MapColor.COLOR_BLACK, SoundType.DEEPSLATE_BRICKS);

    private ModBlocks() { }

    // Hardness -1 also keeps pistons from moving the block, as for bedrock.
    private static RegistrationProvider.RegistryEntry<Block> reinforced(String name, MapColor color, SoundType sound) {
        return BLOCKS.register(name, key -> new Block(PropertiesCompat.block(key).mapColor(color)
                .strength(-1.0F, 3600000.0F).sound(sound).noLootTable()));
    }

    private static Block.Properties fixture(net.minecraft.resources.ResourceKey<Block> key) {
        return PropertiesCompat.block(key).mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).sound(SoundType.METAL);
    }

    private static RegistrationProvider.RegistryEntry<Block> lattice(String name, int light) {
        return BLOCKS.register(name, key -> new Block(PropertiesCompat.block(key).mapColor(MapColor.COLOR_BLACK)
                .strength(5.0F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)
                .lightLevel(state -> light)));
    }

    private static RegistrationProvider.RegistryEntry<Block> hull(String name) {
        return BLOCKS.register(name, key -> new Block(PropertiesCompat.block(key)
                .mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    }

    public static void init() { }
}
