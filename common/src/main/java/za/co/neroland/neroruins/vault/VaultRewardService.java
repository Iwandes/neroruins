package za.co.neroland.neroruins.vault;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.config.NeroRuinsConfig;
import za.co.neroland.neroruins.data.ClaimLedger;
import za.co.neroland.neroruins.data.VaultClaimPolicy;
import za.co.neroland.neroruins.progression.RuinMilestones;
import za.co.neroland.neroruins.puzzle.BallastPlateBlock;
import za.co.neroland.neroruins.puzzle.BallastPuzzle;

/**
 * Server-side vault claims. A claim is made in two steps so a retry can never re-roll: the first attempt
 * rolls the table once and stores the encoded result as a reservation in the {@link ClaimLedger}; every
 * attempt (including that first one) then delivers the reservation and, in the same server-thread step,
 * turns it into a recorded claim. A full inventory leaves the reservation in place, so changed loot
 * conditions, player state or data packs cannot change what the player receives.
 */
public final class VaultRewardService {
    public enum Result { AVAILABLE, RESERVED, CLAIMED, ALREADY_CLAIMED, FULL, UNAVAILABLE, SEALED, TAKEN }
    /** How far from a cache its room's ballast plates may sit. */
    private static final int PUZZLE_RADIUS = 6;
    private VaultRewardService() { }

    private static ClaimLedger.Location location(ServerLevel level, BlockPos pos) {
        return new ClaimLedger.Location(level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static Result availability(ServerPlayer player, ServerLevel level, VaultCacheBlockEntity cache) {
        var policy = VaultClaimPolicy.parse(NeroRuinsConfig.VAULT_MODE.get());
        if (!NeroRuinsConfig.VAULTS_ENABLED.get() || policy.isEmpty()) return Result.UNAVAILABLE;
        if (cache.sealed()) return Result.SEALED;
        BlockPos pos = cache.getBlockPos();
        String tableId = cache.lootTable();
        var server = level.getServer();
        ClaimLedger ledger = ClaimLedger.get(server);
        var location = location(level, pos);
        if (ledger.reservation(location, player.getUUID()).isPresent()) return Result.RESERVED;
        Identifier table = tableId == null ? null : Identifier.tryParse(tableId);
        if (table == null) return Result.UNAVAILABLE;
        var key = ResourceKey.create(Registries.LOOT_TABLE, table);
        var registry = server.reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE);
        if (registry.get(key).isEmpty()) return Result.UNAVAILABLE;
        if (ledger.mayClaim(location, player.getUUID(), policy.get(), System.currentTimeMillis())) return Result.AVAILABLE;
        return refusal(policy.get(), ledger.lastClaim(location, player.getUUID()) != null);
    }

    /**
     * Why a player may not claim: under {@code first_come} a player who never claimed here was beaten to it by
     * someone else, which is a different message from having claimed already.
     */
    static Result refusal(VaultClaimPolicy policy, boolean claimedHere) {
        return policy.mode() == VaultClaimPolicy.Mode.FIRST_COME && !claimedHere ? Result.TAKEN : Result.ALREADY_CLAIMED;
    }

    /**
     * Under {@code first_come}, reserves the solver's roll the moment a puzzle unseals the cache, so the cache is
     * theirs to collect when they reach it. Other policies need nothing: every player gets their own claim.
     */
    public static void holdForSolver(ServerPlayer solver, ServerLevel level, VaultCacheBlockEntity cache) {
        var policy = VaultClaimPolicy.parse(NeroRuinsConfig.VAULT_MODE.get());
        if (policy.isEmpty() || policy.get().mode() != VaultClaimPolicy.Mode.FIRST_COME) return;
        if (availability(solver, level, cache) != Result.AVAILABLE) return;
        ClaimLedger ledger = ClaimLedger.get(level.getServer());
        var location = location(level, cache.getBlockPos());
        long now = System.currentTimeMillis();
        roll(solver, level, cache.getBlockPos(), cache.lootTable(), ledger, location, now)
                .ifPresent(rolled -> ledger.reserve(location, solver.getUUID(), policy.get(), now, rolled));
    }

    public static Result claim(ServerPlayer player, ServerLevel level, VaultCacheBlockEntity cache) {
        if (cache.sealed() && NeroRuinsConfig.VAULTS_ENABLED.get()) {
            if (!puzzleSolved(level, cache.getBlockPos())) return Result.SEALED;
            cache.unseal();
            RuinMilestones.puzzleSolver(player);
        }
        Result result = deliver(player, level, cache);
        if (result == Result.CLAIMED && cache.puzzle()) {
            RuinMilestones.firstVault(player);
            Identifier structure = Identifier.tryParse(cache.structure());
            if (structure != null && !cache.structure().isEmpty()) RuinMilestones.vaultClaimed(level.getServer(), structure);
        }
        return result;
    }

    /** Reads the ballast plates around a cache at the moment of the attempt. */
    static boolean puzzleSolved(ServerLevel level, BlockPos pos) {
        List<BallastPuzzle.Plate> plates = new ArrayList<>();
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-PUZZLE_RADIUS, -3, -PUZZLE_RADIUS),
                pos.offset(PUZZLE_RADIUS, 3, PUZZLE_RADIUS))) {
            BlockState state = level.getBlockState(at);
            if (!(state.getBlock() instanceof BallastPlateBlock)) continue;
            plates.add(new BallastPuzzle.Plate(state.getValue(BallastPlateBlock.TARGET), BallastPlateBlock.load(level, at)));
            if (plates.size() > BallastPuzzle.MAX_PLATES) return false;
        }
        return BallastPuzzle.solved(plates);
    }

    private static Result deliver(ServerPlayer player, ServerLevel level, VaultCacheBlockEntity cache) {
        BlockPos pos = cache.getBlockPos();
        String tableId = cache.lootTable();
        Result available = availability(player, level, cache);
        if (available != Result.AVAILABLE && available != Result.RESERVED) return available;
        var server = level.getServer();
        ClaimLedger ledger = ClaimLedger.get(server);
        var location = location(level, pos);
        long now = System.currentTimeMillis();
        Optional<ClaimLedger.Reservation> reservation = ledger.reservation(location, player.getUUID());
        if (reservation.isEmpty()) {
            Optional<ClaimLedger.Reservation> rolled = roll(player, level, pos, tableId, ledger, location, now);
            if (rolled.isEmpty()) return Result.UNAVAILABLE;
            var policy = VaultClaimPolicy.parse(NeroRuinsConfig.VAULT_MODE.get()).orElseThrow();
            if (!ledger.reserve(location, player.getUUID(), policy, now, rolled.get())) return Result.ALREADY_CLAIMED;
            reservation = rolled;
        }
        List<ItemStack> rewards = decode(level, reservation.get().items());
        if (rewards.isEmpty()) return Result.UNAVAILABLE;
        Inventory inventory = player.getInventory();
        List<ItemStack> slots = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) slots.add(inventory.getItem(i).copy());
        var plan = VaultInventoryPlan.create(slots, rewards, inventory.getMaxStackSize());
        if (plan.isEmpty()) return Result.FULL;
        ClaimLedger.Reservation held = reservation.get();
        var delivery = VaultDelivery.commit(slots, plan.get(),
                () -> ledger.complete(location, player.getUUID(), now).isPresent(), inventory::setItem,
                () -> ledger.reopen(location, player.getUUID(), held),
                failure -> NeroRuinsCommon.LOGGER.warn("[NeroRuins] Vault delivery or rollback failed ({})",
                        failure.getClass().getName()));
        if (delivery == VaultDelivery.Result.NOT_CLAIMABLE) return Result.ALREADY_CLAIMED;
        if (delivery != VaultDelivery.Result.DELIVERED) return Result.UNAVAILABLE;
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        return Result.CLAIMED;
    }

    private static Optional<ClaimLedger.Reservation> roll(ServerPlayer player, ServerLevel level, BlockPos pos,
            String tableId, ClaimLedger ledger, ClaimLedger.Location location, long now) {
        Identifier table = Identifier.tryParse(tableId);
        var server = level.getServer();
        LootTable lootTable = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, table));
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
            return Optional.empty();
        }
        var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        List<CompoundTag> encoded = new ArrayList<>();
        for (ItemStack stack : rewards) {
            if (stack.isEmpty()) continue;
            Optional<Tag> tag = ItemStack.CODEC.encodeStart(ops, stack).result();
            if (tag.isEmpty() || !(tag.get() instanceof CompoundTag compound)) {
                NeroRuinsCommon.LOGGER.warn("[NeroRuins] A vault reward could not be stored and was refused");
                return Optional.empty();
            }
            encoded.add(compound);
        }
        if (encoded.isEmpty() || encoded.size() > ClaimLedger.MAX_RESERVED_STACKS) return Optional.empty();
        return Optional.of(new ClaimLedger.Reservation(table.toString(), now, previous, encoded));
    }

    private static List<ItemStack> decode(ServerLevel level, List<CompoundTag> items) {
        var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        List<ItemStack> result = new ArrayList<>(items.size());
        for (CompoundTag item : items) {
            Optional<ItemStack> stack = ItemStack.CODEC.parse(ops, item).result();
            if (stack.isEmpty() || stack.get().isEmpty()) {
                // An item from a removed mod or data pack: never substitute or re-roll, just refuse delivery.
                NeroRuinsCommon.LOGGER.warn("[NeroRuins] A reserved vault reward no longer exists");
                return List.of();
            }
            result.add(stack.get());
        }
        return result;
    }
}
