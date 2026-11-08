package za.co.neroland.neroruins.guardian;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import za.co.neroland.neroruins.registry.ModBlockEntities;

/**
 * A Ruin Spawner's state: which spawn table it reads, its tier, and how many guardians it has left.
 * The budget is world state for this structure, not personal data. Spawning only happens while a
 * survival or adventure player stands within {@link GuardianRules#SPAWNER_RANGE} blocks; creative and spectator players
 * never wake it.
 */
public final class RuinSpawnerBlockEntity extends BlockEntity {
    private static final int RANGE = GuardianRules.SPAWNER_RANGE;
    /** Ticks between spawns while active, and between idle checks. */
    static final int SPAWN_INTERVAL = 100;
    static final int CHECK_INTERVAL = 20;

    private String table = "";
    private int tier = 1;
    /** Guardians left; negative until first use, then initialised from the tier. */
    private int budget = -1;
    private int cooldown;

    public RuinSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUIN_SPAWNER.get(), pos, state);
    }

    public int tier() { return tier; }
    public int budget() { return budget < 0 ? GuardianRules.initialBudget(tier) : budget; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RuinSpawnerBlockEntity spawner) {
        if (!(level instanceof ServerLevel server) || state.getValue(RuinSpawnerBlock.DORMANT)) return;
        if (--spawner.cooldown > 0) return;
        spawner.cooldown = CHECK_INTERVAL;
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        AABB zone = new AABB(cx - RANGE, cy - RANGE, cz - RANGE, cx + RANGE, cy + RANGE, cz + RANGE);
        boolean playerInRange = !server.getEntitiesOfClass(Player.class, zone,
                player -> GuardianRules.wakes(player.isSpectator(), player.isCreative(), player.distanceToSqr(cx, cy, cz))).isEmpty();
        int nearby = server.getEntitiesOfClass(Mob.class, zone.inflate(4),
                mob -> mob.isAlive() && mob.getType().getCategory() == MobCategory.MONSTER).size();
        int remaining = spawner.budget();
        if (!GuardianRules.shouldSpawn(remaining, server.getDifficulty() == Difficulty.PEACEFUL, playerInRange, nearby)) return;

        Identifier tableId = Identifier.tryParse(spawner.table);
        Optional<SpawnTable> table = tableId == null ? Optional.empty() : SpawnTable.get(server.getServer(), tableId);
        Optional<String> pick = table.flatMap(found -> found.pick(spawner.tier,
                id -> registered(id), bound -> server.getRandom().nextInt(bound)));
        if (pick.isEmpty()) {
            spawner.cooldown = SPAWN_INTERVAL * 4;
            return;
        }
        Optional<BlockPos> at = findSpot(server, pos);
        if (at.isEmpty()) return;
        Identifier entityId = Identifier.tryParse(pick.get());
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(entityId);
        Entity entity = type.create(server, EntitySpawnReason.SPAWNER);
        if (!(entity instanceof Mob mob)) {
            if (entity != null) entity.discard();
            return;
        }
        BlockPos spot = at.get();
        mob.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, server.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(server, server.getCurrentDifficultyAt(spot), EntitySpawnReason.SPAWNER, null);
        mob.setPersistenceRequired();
        server.addFreshEntity(mob);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, spot.getX() + 0.5, spot.getY() + 1.0, spot.getZ() + 0.5,
                12, 0.4, 0.6, 0.4, 0.05);
        server.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, net.minecraft.sounds.SoundSource.HOSTILE, 0.6F, 1.4F);

        spawner.budget = remaining - 1;
        spawner.cooldown = SPAWN_INTERVAL;
        spawner.setChanged();
        if (spawner.budget == 0) {
            server.setBlock(pos, state.setValue(RuinSpawnerBlock.DORMANT, true), Block.UPDATE_ALL);
        }
    }

    private static boolean registered(String id) {
        Identifier parsed = Identifier.tryParse(id);
        return parsed != null && BuiltInRegistries.ENTITY_TYPE.containsKey(parsed);
    }

    /** A standing spot near the spawner: two clear blocks over a sturdy floor. */
    private static Optional<BlockPos> findSpot(ServerLevel level, BlockPos pos) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = level.getRandom().nextInt(7) - 3;
            int dz = level.getRandom().nextInt(7) - 3;
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos spot = pos.offset(dx, dy, dz);
                BlockPos below = spot.below();
                if (level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()
                        && level.getBlockState(spot.above()).getCollisionShape(level, spot.above()).isEmpty()
                        && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    return Optional.of(spot);
                }
            }
        }
        return Optional.empty();
    }

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    //?} else {
    /*@Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = ValueOutput.of(tag, registries);
    *///?}
        output.putString("table", table);
        output.putInt("tier", tier);
        output.putInt("budget", budget);
    }

    //? if >=1.21.6 {
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    //?} else {
    /*@Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
    *///?}
        String value = input.getStringOr("table", "");
        table = value.length() <= 128 && Identifier.tryParse(value) != null ? value : "";
        tier = Math.max(1, Math.min(SpawnTable.MAX_TIER, input.getIntOr("tier", 1)));
        budget = Math.max(-1, Math.min(GuardianRules.initialBudget(SpawnTable.MAX_TIER), input.getIntOr("budget", -1)));
    }
}
