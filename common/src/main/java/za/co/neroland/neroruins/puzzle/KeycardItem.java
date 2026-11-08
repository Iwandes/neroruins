package za.co.neroland.neroruins.puzzle;

import net.minecraft.world.item.Item;

/** A Halcyon Survey access card. A reader opens for a card of its tier or higher. Not consumed when used. */
public final class KeycardItem extends Item {
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 3;
    private final int tier;

    public KeycardItem(int tier, Properties properties) {
        super(properties);
        if (tier < MIN_TIER || tier > MAX_TIER) throw new IllegalArgumentException("Invalid keycard tier");
        this.tier = tier;
    }

    public int tier() { return tier; }
}
