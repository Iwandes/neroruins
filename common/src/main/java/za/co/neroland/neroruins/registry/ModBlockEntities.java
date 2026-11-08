package za.co.neroland.neroruins.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import za.co.neroland.nerolandcore.registry.BlockEntityTypeCompat;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.platform.Services;
import za.co.neroland.neroruins.terminal.LogTerminalBlockEntity;
import za.co.neroland.neroruins.vault.VaultCacheBlockEntity;

public final class ModBlockEntities {
    private static final RegistrationProvider<BlockEntityType<?>> TYPES =
            Services.REGISTRATION.provider(Registries.BLOCK_ENTITY_TYPE, NeroRuinsCommon.MOD_ID);
    public static final RegistrationProvider.RegistryEntry<BlockEntityType<LogTerminalBlockEntity>> LOG_TERMINAL =
            TYPES.register("log_terminal", key -> BlockEntityTypeCompat.create(LogTerminalBlockEntity::new, ModBlocks.LOG_TERMINAL.get()));
    public static final RegistrationProvider.RegistryEntry<BlockEntityType<VaultCacheBlockEntity>> VAULT_CACHE =
            TYPES.register("vault_cache", key -> BlockEntityTypeCompat.create(VaultCacheBlockEntity::new, ModBlocks.VAULT_CACHE.get()));
    public static final RegistrationProvider.RegistryEntry<BlockEntityType<za.co.neroland.neroruins.guardian.RuinSpawnerBlockEntity>> RUIN_SPAWNER =
            TYPES.register("ruin_spawner", key -> BlockEntityTypeCompat.create(za.co.neroland.neroruins.guardian.RuinSpawnerBlockEntity::new, ModBlocks.RUIN_SPAWNER.get()));
    public static final RegistrationProvider.RegistryEntry<BlockEntityType<za.co.neroland.neroruins.puzzle.GlyphLockBlockEntity>> GLYPH_LOCK =
            TYPES.register("glyph_lock", key -> BlockEntityTypeCompat.create(za.co.neroland.neroruins.puzzle.GlyphLockBlockEntity::new, ModBlocks.GLYPH_LOCK.get()));
    private ModBlockEntities() { }
    public static void init() { }
}
