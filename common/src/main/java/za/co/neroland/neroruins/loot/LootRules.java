package za.co.neroland.neroruins.loot;

import java.util.regex.Pattern;

/**
 * Pure rules behind NeroRuins' loot conditions, kept free of Minecraft types so they can be tested
 * without bootstrapping the game.
 */
public final class LootRules {
    /** Mod ids accepted by Fabric, Forge and NeoForge: lowercase, starting with a letter. */
    private static final Pattern MOD_ID = Pattern.compile("[a-z][a-z0-9_-]{1,63}");

    private LootRules() { }

    /** Whether {@code id} is a syntactically valid mod id for {@code neroruins:mod_loaded}. */
    public static boolean isValidModId(String id) {
        return id != null && MOD_ID.matcher(id).matches();
    }

    /**
     * The effective relic chance: the table's base chance scaled by the {@code loot.relicRarity}
     * lever and clamped to {@code [0, 1]}. A non-finite input yields 0, so a bad value never turns a
     * relic roll into a guaranteed drop.
     */
    public static float relicChance(float base, double multiplier) {
        double chance = base * multiplier;
        if (!Double.isFinite(chance) || chance <= 0.0D) {
            return 0.0F;
        }
        return (float) Math.min(1.0D, chance);
    }
}
