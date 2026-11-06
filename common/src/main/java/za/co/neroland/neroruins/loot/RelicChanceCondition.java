package za.co.neroland.neroruins.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import za.co.neroland.neroruins.config.NeroRuinsConfig;

/**
 * {@code neroruins:relic_chance}: a random chance scaled by the server's {@code loot.relicRarity}
 * config lever (0.25x to 4x). The base chance lives in the loot table so data packs can retune it.
 * JSON: {@code {"condition": "neroruins:relic_chance", "chance": 0.25}}.
 */
public record RelicChanceCondition(float chance) implements LootItemCondition {
    public static final MapCodec<RelicChanceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(RelicChanceCondition::chance))
            .apply(instance, RelicChanceCondition::new));

    @Override
    public boolean test(LootContext context) {
        return context.getRandom().nextFloat() < LootRules.relicChance(chance, NeroRuinsConfig.RELIC_RARITY.get());
    }

    //? if >=26.1 {
    @Override
    public MapCodec<RelicChanceCondition> codec() {
        return CODEC;
    }
    //?} else {
    /*@Override
    public net.minecraft.world.level.storage.loot.predicates.LootItemConditionType getType() {
        return za.co.neroland.neroruins.registry.ModLootConditions.RELIC_CHANCE.get();
    }
    *///?}
}
