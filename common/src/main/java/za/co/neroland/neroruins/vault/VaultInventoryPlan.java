package za.co.neroland.neroruins.vault;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public final class VaultInventoryPlan {
    public interface StackRules<T> {
        boolean empty(T stack);
        int count(T stack);
        int limit(T stack);
        boolean matches(T first, T second);
        T withCount(T stack, int count);
    }

    private VaultInventoryPlan() { }

    public static Optional<List<ItemStack>> create(List<ItemStack> inventory, List<ItemStack> rewards, int containerLimit) {
        return create(inventory, rewards, new StackRules<ItemStack>() {
            public boolean empty(ItemStack stack) { return stack.isEmpty(); }
            public int count(ItemStack stack) { return stack.getCount(); }
            public int limit(ItemStack stack) { return Math.min(stack.getMaxStackSize(), containerLimit); }
            public boolean matches(ItemStack first, ItemStack second) { return ItemStack.isSameItemSameComponents(first, second); }
            public ItemStack withCount(ItemStack stack, int count) { return stack.copyWithCount(count); }
        });
    }

    public static <T> Optional<List<T>> create(List<T> inventory, List<T> rewards, StackRules<T> rules) {
        List<T> result = new ArrayList<>(inventory);
        for (T reward : rewards) {
            int remaining = rules.count(reward);
            if (remaining <= 0 || rules.empty(reward)) continue;
            for (int i = 0; i < result.size() && remaining > 0; i++) {
                T slot = result.get(i);
                if (!rules.empty(slot) && rules.matches(slot, reward)) {
                    int count = Math.min(remaining, Math.max(0, rules.limit(slot) - rules.count(slot)));
                    if (count > 0) {
                        result.set(i, rules.withCount(slot, rules.count(slot) + count));
                        remaining -= count;
                    }
                }
            }
            for (int i = 0; i < result.size() && remaining > 0; i++) {
                if (rules.empty(result.get(i))) {
                    int count = Math.min(remaining, rules.limit(reward));
                    if (count <= 0) return Optional.empty();
                    result.set(i, rules.withCount(reward, count));
                    remaining -= count;
                }
            }
            if (remaining > 0) return Optional.empty();
        }
        return Optional.of(List.copyOf(result));
    }
}
