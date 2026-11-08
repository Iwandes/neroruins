package za.co.neroland.neroruins.puzzle;

import java.util.OptionalInt;
import net.minecraft.world.item.ItemStack;

/**
 * The one place a card reader asks what access an item grants. In 0.5.0 only NeroRuins' own keycards count;
 * NeroSecurity's shared keycard framework maps its cards here when it ships, without readers changing.
 */
public final class KeycardCompat {
    private KeycardCompat() { }

    public static OptionalInt tier(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof KeycardItem card) return OptionalInt.of(card.tier());
        return OptionalInt.empty();
    }

    /** Whether a card of {@code cardTier} opens a reader that needs {@code readerTier}. */
    public static boolean opens(OptionalInt cardTier, int readerTier) {
        return cardTier.isPresent() && cardTier.getAsInt() >= readerTier;
    }
}
