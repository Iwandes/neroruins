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

    private ModBlocks() { }

    private static RegistrationProvider.RegistryEntry<Block> hull(String name) {
        return BLOCKS.register(name, key -> new Block(PropertiesCompat.block(key)
                .mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    }

    public static void init() { }
}
