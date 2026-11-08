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

/**
 * A keycard reader. Use it with a card of its {@link #TIER} or higher in your main hand; the card is kept. The
 * required tier shows as lit bars on its face. Unbreakable.
 */
public final class CardReaderBlock extends Block {
    public static final MapCodec<CardReaderBlock> CODEC = BlockCodecs.simple(CardReaderBlock::new);
    //? if >=1.21.2 {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    //?} else {
    /*public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    *///?}
    public static final IntegerProperty TIER = IntegerProperty.create("tier", KeycardItem.MIN_TIER, KeycardItem.MAX_TIER);
    public static final BooleanProperty ACCEPTED = BooleanProperty.create("accepted");

    @SuppressWarnings("this-escape")
    public CardReaderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER, 1)
                .setValue(ACCEPTED, false));
    }

    protected MapCodec<CardReaderBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TIER, ACCEPTED);
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            if (state.getValue(ACCEPTED)) return InteractionResult.CONSUME;
            int tier = state.getValue(TIER);
            if (!KeycardCompat.opens(KeycardCompat.tier(player.getMainHandItem()), tier)) {
                server.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.8F, 0.6F);
                serverPlayer.sendSystemMessage(Component.translatable("gui.neroruins.card_reader.denied", tier));
                return InteractionResult.CONSUME;
            }
            server.setBlock(pos, state.setValue(ACCEPTED, true), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
            PuzzleUnlock.open(server, pos, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
