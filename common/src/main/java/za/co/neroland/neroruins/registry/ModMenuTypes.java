package za.co.neroland.neroruins.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.platform.Services;
import za.co.neroland.neroruins.terminal.LogTerminalMenu;
import za.co.neroland.neroruins.vault.VaultCacheMenu;

public final class ModMenuTypes {
    private static final RegistrationProvider<MenuType<?>> TYPES =
            Services.REGISTRATION.provider(Registries.MENU, NeroRuinsCommon.MOD_ID);
    public static final RegistrationProvider.RegistryEntry<MenuType<LogTerminalMenu>> LOG_TERMINAL =
            TYPES.register("log_terminal", key -> new MenuType<>(LogTerminalMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistrationProvider.RegistryEntry<MenuType<VaultCacheMenu>> VAULT_CACHE =
            TYPES.register("vault_cache", key -> new MenuType<>(VaultCacheMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistrationProvider.RegistryEntry<MenuType<za.co.neroland.neroruins.puzzle.GlyphLockMenu>> GLYPH_LOCK =
            TYPES.register("glyph_lock", key -> new MenuType<>(za.co.neroland.neroruins.puzzle.GlyphLockMenu::new, FeatureFlags.VANILLA_SET));
    private ModMenuTypes() { }
    public static void init() { }
}
