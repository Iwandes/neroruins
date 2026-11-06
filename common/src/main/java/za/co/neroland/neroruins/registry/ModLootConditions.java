package za.co.neroland.neroruins.registry;

import net.minecraft.core.registries.Registries;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.loot.ModLoadedCondition;
import za.co.neroland.neroruins.loot.RelicChanceCondition;
import za.co.neroland.neroruins.platform.Services;

//? if >=26.1 {
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
//?} else {
/*import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
*///?}

/** Loot item condition types. 26.x registers the condition's MapCodec; 1.21.x wraps it in a type. */
public final class ModLootConditions {
    //? if >=26.1 {
    private static final RegistrationProvider<MapCodec<? extends LootItemCondition>> CONDITIONS =
            Services.REGISTRATION.provider(Registries.LOOT_CONDITION_TYPE, NeroRuinsCommon.MOD_ID);
    public static final RegistrationProvider.RegistryEntry<MapCodec<ModLoadedCondition>> MOD_LOADED =
            CONDITIONS.register("mod_loaded", key -> ModLoadedCondition.CODEC);
    public static final RegistrationProvider.RegistryEntry<MapCodec<RelicChanceCondition>> RELIC_CHANCE =
            CONDITIONS.register("relic_chance", key -> RelicChanceCondition.CODEC);
    //?} else {
    /*private static final RegistrationProvider<LootItemConditionType> CONDITIONS =
            Services.REGISTRATION.provider(Registries.LOOT_CONDITION_TYPE, NeroRuinsCommon.MOD_ID);
    public static final RegistrationProvider.RegistryEntry<LootItemConditionType> MOD_LOADED =
            CONDITIONS.register("mod_loaded", key -> new LootItemConditionType(ModLoadedCondition.CODEC));
    public static final RegistrationProvider.RegistryEntry<LootItemConditionType> RELIC_CHANCE =
            CONDITIONS.register("relic_chance", key -> new LootItemConditionType(RelicChanceCondition.CODEC));
    *///?}

    private ModLootConditions() { }

    public static void init() { }
}
