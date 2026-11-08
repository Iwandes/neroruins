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

    private ModBlocks() { }

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
