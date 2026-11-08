package za.co.neroland.neroruins.puzzle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import za.co.neroland.neroruins.registry.ModMenuTypes;

/**
 * The glyph keypad. Buttons 0–15 enter a glyph, {@link #CLEAR} clears the entry and {@link #SUBMIT} tries it.
 * The entry exists only while the screen is open; the server checks every answer.
 */
public final class GlyphLockMenu extends AbstractContainerMenu {
    public static final int CLEAR = 16;
    public static final int SUBMIT = 17;
    /** Status values shown by the screen. */
    public static final int ENTERING = 0, WRONG = 1, OPEN = 2, INERT = 3, COOLING = 4;
    private static final int SLOTS = 2 + PuzzleSeeds.GLYPH_LENGTH;

    private final GlyphLockBlockEntity lock;
    private final ContainerData data;
    private final int[] entered = new int[PuzzleSeeds.GLYPH_LENGTH];
    private int count;
    private int status;

    public GlyphLockMenu(int id, Inventory inventory) { this(id, inventory, null); }

    @SuppressWarnings("this-escape")
    public GlyphLockMenu(int id, Inventory inventory, GlyphLockBlockEntity lock) {
        super(ModMenuTypes.GLYPH_LOCK.get(), id);
        this.lock = lock;
        this.data = lock == null ? new SimpleContainerData(SLOTS) : new ContainerData() {
            public int get(int index) {
                if (index == 0) {
                    if (lock.solved()) return OPEN;
                    if (!lock.configured()) return INERT;
                    if (lock.coolingDown()) return status == WRONG ? WRONG : COOLING;
                    return status == WRONG ? ENTERING : status;
                }
                if (index == 1) return count;
                int slot = index - 2;
                return slot < count ? entered[slot] : -1;
            }
            public void set(int index, int value) { }
            public int getCount() { return SLOTS; }
        };
        addDataSlots(data);
    }

    public int status() { return data.get(0); }
    public int count() { return Math.max(0, Math.min(PuzzleSeeds.GLYPH_LENGTH, data.get(1))); }
    public int entered(int slot) { return data.get(2 + slot); }

    public boolean stillValid(Player player) { return lock == null || lock.stillValid(player); }
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (lock == null || !lock.stillValid(player) || !(player instanceof ServerPlayer serverPlayer)
                || !(lock.getLevel() instanceof ServerLevel level) || button < 0 || button > SUBMIT) return false;
        if (button < 16) {
            if (count < entered.length && !lock.solved()) entered[count++] = button;
            status = ENTERING;
        } else if (button == CLEAR) {
            count = 0;
            status = ENTERING;
        } else if (!lock.solved()) {
            if (lock.coolingDown() || count != entered.length) return true;
            boolean open = lock.attempt(entered.clone());
            count = 0;
            status = open ? OPEN : WRONG;
            if (open) PuzzleUnlock.open(level, lock.getBlockPos(), serverPlayer);
        }
        return true;
    }
}
