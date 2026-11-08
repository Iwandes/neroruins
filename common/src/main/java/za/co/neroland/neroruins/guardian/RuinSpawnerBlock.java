package za.co.neroland.neroruins.guardian;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.neroruins.registry.ModBlockEntities;

/** Ruin Spawner: an unbreakable guardian source that goes visibly dormant once its budget is spent. */
public final class RuinSpawnerBlock extends BaseEntityBlock {
    public static final MapCodec<RuinSpawnerBlock> CODEC = BlockCodecs.simple(RuinSpawnerBlock::new);
    public static final BooleanProperty DORMANT = BooleanProperty.create("dormant");

    @SuppressWarnings("this-escape")
    public RuinSpawnerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DORMANT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DORMANT);
    }

    protected MapCodec<RuinSpawnerBlock> codec() { return CODEC; }
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RuinSpawnerBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.RUIN_SPAWNER.get(), RuinSpawnerBlockEntity::serverTick);
    }
}
