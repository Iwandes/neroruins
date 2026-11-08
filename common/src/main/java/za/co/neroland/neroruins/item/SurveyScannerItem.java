package za.co.neroland.neroruins.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import za.co.neroland.neroruins.expedition.Bearing;
import za.co.neroland.neroruins.expedition.RuinLocator;

/**
 * Pings for the nearest ruin complex and reports a compass point and distance band on the action bar. The search
 * is bounded by {@link RuinLocator} and the scanner has a cooldown, so repeated use cannot drive chunk loading.
 */
public final class SurveyScannerItem extends Item {
    public static final int COOLDOWN_TICKS = 200;

    public SurveyScannerItem(Properties properties) { super(properties); }

    @Override
    //? if >=1.21.2 {
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //?} else {
    /*public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    *///?}
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel server) {
            //? if >=1.21.2 {
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
            //?} else {
            /*player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            *///?}
            Component reading = RuinLocator.nearest(server, serverPlayer.blockPosition(), found -> false)
                    .<Component>map(found -> Component.translatable("message.neroruins.scanner.signal",
                            Component.translatable("family.neroruins." + found.family()),
                            Bearing.describe(found.x() - serverPlayer.getBlockX(), found.z() - serverPlayer.getBlockZ())))
                    .orElseGet(() -> Component.translatable("message.neroruins.scanner.none"));
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(reading));
        }
        //? if >=1.21.2 {
        return InteractionResult.SUCCESS;
        //?} else {
        /*return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        *///?}
    }
}
