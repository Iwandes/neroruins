package za.co.neroland.neroruins.puzzle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
//? if >=1.21.2 {
import net.minecraft.world.level.block.state.properties.EnumProperty;
//?} else {
/*import net.minecraft.world.level.block.state.properties.DirectionProperty;
*///?}
import net.minecraft.world.phys.BlockHitResult;
import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.neroruins.registry.ModItems;

/**
 * A {@code relic_socket}: shaped for one chapter's relic. Using it with that relic in your main hand reads the relic
 * and opens the room; the relic stays with you. Using it with anything else names the relic it needs, so the room
 * is solvable without a wiki. Unbreakable.
 */
public final class RelicSocketBlock extends Block {
    public static final MapCodec<RelicSocketBlock> CODEC = BlockCodecs.simple(RelicSocketBlock::new);
    //? if >=1.21.2 {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    //?} else {
    /*public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    *///?}
    public static final IntegerProperty CHAPTER = IntegerProperty.create("chapter", 1, 9);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    @SuppressWarnings("this-escape")
    public RelicSocketBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CHAPTER, 1)
                .setValue(LIT, false));
    }

    protected MapCodec<RelicSocketBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CHAPTER, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** The relic a socket of this chapter accepts. */
    public static Item relic(int chapter) { return ModItems.RELICS.get(chapter - 1).get(); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            if (state.getValue(LIT)) return InteractionResult.CONSUME;
            Item wanted = relic(state.getValue(CHAPTER));
            if (!player.getMainHandItem().is(wanted)) {
                serverPlayer.sendSystemMessage(Component.translatable("gui.neroruins.relic_socket.wants",
                        wanted.getDefaultInstance().getHoverName()));
                return InteractionResult.CONSUME;
            }
            server.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.2F);
            serverPlayer.sendSystemMessage(Component.translatable("gui.neroruins.relic_socket.returned"));
            PuzzleUnlock.open(server, pos, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
