package za.co.neroland.neroruins.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import za.co.neroland.neroruins.platform.Services;

/**
 * {@code neroruins:mod_loaded}: passes when the named mod is present in this launch. Lets a whole
 * sibling-flavoured pool exist without an item tag per item, and without any dependency on that
 * sibling. JSON: {@code {"condition": "neroruins:mod_loaded", "mod": "nerotech"}} (26.3 and later:
 * {@code "type"} instead of {@code "condition"}).
 */
public record ModLoadedCondition(String mod) implements LootItemCondition {
    public static final MapCodec<ModLoadedCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.validate(id -> LootRules.isValidModId(id)
                    ? DataResult.success(id) : DataResult.error(() -> "Invalid mod id: " + id))
                    .fieldOf("mod").forGetter(ModLoadedCondition::mod))
            .apply(instance, ModLoadedCondition::new));

    @Override
    public boolean test(LootContext context) {
        return Services.PLATFORM.isModLoaded(mod);
    }

    //? if >=26.1 {
    @Override
    public MapCodec<ModLoadedCondition> codec() {
        return CODEC;
    }
    //?} else {
    /*@Override
    public net.minecraft.world.level.storage.loot.predicates.LootItemConditionType getType() {
        return za.co.neroland.neroruins.registry.ModLootConditions.MOD_LOADED.get();
    }
    *///?}
}
