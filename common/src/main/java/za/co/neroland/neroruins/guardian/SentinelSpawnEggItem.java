package za.co.neroland.neroruins.guardian;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A Sentinel spawn egg. Not a vanilla {@code SpawnEggItem}: that binds its type in the constructor,
 * and on the deferred-register loaders items register before entity types. This resolves the type
 * when used, as NeroCreatures' and Nerospace's eggs do.
 */
public class SentinelSpawnEggItem extends Item {
    private final Supplier<? extends EntityType<? extends Mob>> type;

    public SentinelSpawnEggItem(Properties properties, Supplier<? extends EntityType<? extends Mob>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                ? clicked : clicked.relative(face);
        Player player = context.getPlayer();
        Mob mob = this.type.get().spawn(level, stack, player, spawnPos, EntitySpawnReason.SPAWN_ITEM_USE,
                true, !clicked.equals(spawnPos) && face == Direction.UP);
        if (mob != null && (player == null || !player.getAbilities().instabuild)) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
