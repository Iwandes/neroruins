package za.co.neroland.neroruins.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * A ballast plate: shows its target as lit pips and counts the items lying on it. It has no state of its own
 * beyond the target the {@code neroruins:puzzle_seed} processor set at generation, so nothing a player does to
 * it is stored. Unbreakable in survival; the vault it gates is unbreakable too.
 */
public final class BallastPlateBlock extends Block {
    public static final MapCodec<BallastPlateBlock> CODEC = BlockCodecs.simple(BallastPlateBlock::new);
    public static final IntegerProperty TARGET = IntegerProperty.create("target", BallastPuzzle.MIN, BallastPuzzle.MAX);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 1, 15);

    @SuppressWarnings("this-escape")
    public BallastPlateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TARGET, BallastPuzzle.MIN));
    }

    protected MapCodec<BallastPlateBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TARGET);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Total item count resting on the plate. */
    public static int load(Level level, BlockPos pos) {
        AABB area = new AABB(pos.getX() + 0.0625, pos.getY(), pos.getZ() + 0.0625,
                pos.getX() + 0.9375, pos.getY() + 0.5, pos.getZ() + 0.9375);
        int total = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            total += item.getItem().getCount();
            if (total > BallastPuzzle.MAX) return total;
        }
        return total;
    }
}
