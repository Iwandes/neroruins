package za.co.neroland.neroruins.vault;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import za.co.neroland.neroruins.registry.ModMenuTypes;

public final class VaultCacheMenu extends AbstractContainerMenu {
    private final VaultCacheBlockEntity cache;
    private final ContainerData data;
    private int status;

    public VaultCacheMenu(int id, Inventory inventory) { this(id, inventory, null); }

    @SuppressWarnings("this-escape")
    public VaultCacheMenu(int id, Inventory inventory, VaultCacheBlockEntity cache) {
        super(ModMenuTypes.VAULT_CACHE.get(), id);
        this.cache = cache;
        this.data = cache == null ? new SimpleContainerData(1) : new ContainerData() {
            public int get(int index) {
                if (index != 0) return 0;
                if (!(inventory.player instanceof ServerPlayer player) || !(cache.getLevel() instanceof ServerLevel level)) return 4;
                return switch (VaultRewardService.availability(player, level, cache)) {
                    case AVAILABLE -> status == 4 ? status : 0;
                    case RESERVED -> status == 3 ? 3 : 5;
                    case SEALED -> 6;
                    case TAKEN -> 7;
                    case ALREADY_CLAIMED -> status == 1 ? 1 : 2;
                    default -> 4;
                };
            }
            public void set(int index, int value) { }
            public int getCount() { return 1; }
        };
        addDataSlots(data);
    }

    public int status() { return data.get(0); }
    public boolean stillValid(Player player) { return cache == null || cache.stillValid(player); }
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || cache == null || !cache.stillValid(player)
                || !(player instanceof ServerPlayer serverPlayer) || !(cache.getLevel() instanceof ServerLevel level)) return false;
        var result = VaultRewardService.claim(serverPlayer, level, cache);
        status = switch (result) {
            case AVAILABLE -> 0;
            case RESERVED -> 5;
            case SEALED -> 6;
            case TAKEN -> 7;
            case CLAIMED -> 1;
            case ALREADY_CLAIMED -> 2;
            case FULL -> 3;
            case UNAVAILABLE -> 4;
        };
        return true;
    }
}
