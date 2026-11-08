package za.co.neroland.neroruins.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

/** The receiver of a conduit room. Once power reaches it, it stays lit and the room's doors open. */
public final class ConduitLockBlock extends Block {
    public static final MapCodec<ConduitLockBlock> CODEC = BlockCodecs.simple(ConduitLockBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    @SuppressWarnings("this-escape")
    public ConduitLockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    protected MapCodec<ConduitLockBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
