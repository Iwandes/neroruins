package za.co.neroland.neroruins.vault;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.config.NeroRuinsConfig;
import za.co.neroland.neroruins.data.ClaimLedger;
import za.co.neroland.neroruins.data.VaultClaimPolicy;

public final class VaultRewardService {
    public enum Result { AVAILABLE, CLAIMED, ALREADY_CLAIMED, FULL, UNAVAILABLE }
    private VaultRewardService() { }

    public static Result availability(ServerPlayer player, ServerLevel level, BlockPos pos, String tableId) {
        var policy = VaultClaimPolicy.parse(NeroRuinsConfig.VAULT_MODE.get());
        Identifier table = tableId == null ? null : Identifier.tryParse(tableId);
        if (!NeroRuinsConfig.VAULTS_ENABLED.get() || policy.isEmpty() || table == null) return Result.UNAVAILABLE;
        var server = level.getServer();
        var key = ResourceKey.create(Registries.LOOT_TABLE, table);
        var registry = server.reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE);
        if (registry.get(key).isEmpty()) return Result.UNAVAILABLE;
        ClaimLedger ledger = ClaimLedger.get(server);
        var location = new ClaimLedger.Location(level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
        long now = System.currentTimeMillis();
        return ledger.mayClaim(location, player.getUUID(), policy.get(), now) ? Result.AVAILABLE : Result.ALREADY_CLAIMED;
    }

    public static Result claim(ServerPlayer player, ServerLevel level, BlockPos pos, String tableId) {
        Result available = availability(player, level, pos, tableId);
        if (available != Result.AVAILABLE) return available;
        var policy = VaultClaimPolicy.parse(NeroRuinsConfig.VAULT_MODE.get()).orElseThrow();
        Identifier table = Identifier.tryParse(tableId);
        var server = level.getServer();
        var key = ResourceKey.create(Registries.LOOT_TABLE, table);
        ClaimLedger ledger = ClaimLedger.get(server);
        var location = new ClaimLedger.Location(level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
        long now = System.currentTimeMillis();
        LootTable lootTable = server.reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player).withLuck(player.getLuck())
                .create(LootContextParamSets.CHEST);
        Long previous = ledger.lastClaim(location, player.getUUID());
        long seed = VaultLootSeed.forClaim(level.getSeed(), pos.asLong(), location.dimension(), table.toString(),
                player.getUUID(), previous);
        List<ItemStack> rewards;
        try {
            rewards = lootTable.getRandomItems(params, seed);
        } catch (RuntimeException failure) {
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] Could not roll vault loot ({})", failure.getClass().getName());
            return Result.UNAVAILABLE;
        }
        if (rewards.isEmpty()) return Result.UNAVAILABLE;
        Inventory inventory = player.getInventory();
        List<ItemStack> slots = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) slots.add(inventory.getItem(i).copy());
        var plan = VaultInventoryPlan.create(slots, rewards, inventory.getMaxStackSize());
        if (plan.isEmpty()) return Result.FULL;
        var delivery = VaultDelivery.commit(slots, plan.get(),
                () -> ledger.record(location, player.getUUID(), policy, now), inventory::setItem,
                () -> ledger.restoreClaim(location, player.getUUID(), previous),
                failure -> NeroRuinsCommon.LOGGER.warn("[NeroRuins] Vault delivery or rollback failed ({})",
                        failure.getClass().getName()));
        if (delivery == VaultDelivery.Result.NOT_CLAIMABLE) return Result.ALREADY_CLAIMED;
        if (delivery != VaultDelivery.Result.DELIVERED) return Result.UNAVAILABLE;
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        return Result.CLAIMED;
    }
}
