package za.co.neroland.neroruins.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;

class VaultInventoryPlanTest {
    record Stack(String item, int count, int limit) { }
    private static final StackRules RULES = new StackRules();
    private static final class StackRules implements VaultInventoryPlan.StackRules<Stack> {
        public boolean empty(Stack stack) { return stack.count == 0; }
        public int count(Stack stack) { return stack.count; }
        public int limit(Stack stack) { return stack.limit; }
        public boolean matches(Stack first, Stack second) { return first.item.equals(second.item); }
        public Stack withCount(Stack stack, int count) { return new Stack(stack.item, count, stack.limit); }
    }

    @Test
    void combinesStacksWithoutMutatingTheOriginalInventory() {
        var original = List.of(new Stack("diamond", 60, 64), new Stack("empty", 0, 64));
        var plan = VaultInventoryPlan.create(original, List.of(new Stack("diamond", 9, 64)), RULES).orElseThrow();
        assertEquals(64, plan.get(0).count);
        assertEquals(5, plan.get(1).count);
        assertEquals(60, original.get(0).count);
    }

    @Test
    void rejectsRewardsThatDoNotFitWithoutChangingAnySlot() {
        var original = List.of(new Stack("diamond", 64, 64));
        assertTrue(VaultInventoryPlan.create(original, List.of(new Stack("emerald", 1, 64)), RULES).isEmpty());
        assertEquals(64, original.get(0).count);
    }

    @Test
    void respectsSlotLimitsAndHandlesMultipleRewardsWithoutLosingAny() {
        var original = List.of(new Stack("diamond", 63, 64), new Stack("empty", 0, 64),
                new Stack("empty", 0, 64));
        var plan = VaultInventoryPlan.create(original,
                List.of(new Stack("diamond", 2, 64), new Stack("emerald", 3, 64)), RULES).orElseThrow();
        assertEquals(new Stack("diamond", 64, 64), plan.get(0));
        assertEquals(new Stack("diamond", 1, 64), plan.get(1));
        assertEquals(new Stack("emerald", 3, 64), plan.get(2));
        assertTrue(VaultInventoryPlan.create(original,
                List.of(new Stack("diamond", 2, 64), new Stack("emerald", 65, 64)), RULES).isEmpty());
    }
}
