package za.co.neroland.neroruins.guardian;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * Arc Grate: a Survey floor panel that discharges into a survival player standing on it, then needs
 * three seconds to recharge (its lit lines go dark meanwhile). It hurts only the player and never
 * touches blocks or sets fires. Mobs pass over it unharmed, so guardians can patrol across it.
 */
public final class ArcGrateBlock extends Block {
    public static final MapCodec<ArcGrateBlock> CODEC = BlockCodecs.simple(ArcGrateBlock::new);
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
    public static final float DAMAGE = 3.0F;
    public static final int RECHARGE_TICKS = 60;

    @SuppressWarnings("this-escape")
    public ArcGrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGED);
    }

    protected MapCodec<ArcGrateBlock> codec() { return CODEC; }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        Player player = entity instanceof Player found ? found : null;
        if (!(level instanceof ServerLevel server) || !GuardianRules.discharges(state.getValue(CHARGED), player != null,
                player != null && player.isSpectator(), player != null && player.isCreative())) return;
        entity.hurtServer(server, server.damageSources().lightningBolt(), DAMAGE);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                16, 0.35, 0.2, 0.35, 0.1);
        server.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.4F, 2.0F);
        server.setBlock(pos, state.setValue(CHARGED, false), Block.UPDATE_ALL);
        server.scheduleTick(pos, this, RECHARGE_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CHARGED)) level.setBlock(pos, state.setValue(CHARGED, true), Block.UPDATE_ALL);
    }
}
