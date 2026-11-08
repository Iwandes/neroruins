package za.co.neroland.neroruins.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import za.co.neroland.neroruins.data.ExpeditionRecord;
import za.co.neroland.neroruins.expedition.ExpeditionSnapshot;
import za.co.neroland.neroruins.platform.Services;

/** Opens the holder's own Expedition Record. Nobody else's discoveries are ever read or sent. */
public final class ExpeditionMapItem extends Item {
    public ExpeditionMapItem(Properties properties) { super(properties); }

    @Override
    //? if >=1.21.2 {
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //?} else {
    /*public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Level level, Player player, InteractionHand hand) {
    *///?}
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            var own = ExpeditionRecord.get(level.getServer()).export(serverPlayer.getUUID());
            if (!Services.NETWORK.sendToPlayer(serverPlayer, ExpeditionSnapshot.of(own))) {
                // Plain text: a client without NeroRuins has no translations for this mod.
                message(serverPlayer, Component.literal("The Expedition Map screen requires NeroRuins on your "
                        + "client. Your record can still be exported with /neroruins privacy export."));
            }
        }
        //? if >=1.21.2 {
        return InteractionResult.SUCCESS;
        //?} else {
        /*return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
        *///?}
    }

    private static void message(ServerPlayer player, Component text) {
        //? if >=26.1 {
        player.sendSystemMessage(text);
        //?} else {
        /*player.displayClientMessage(text, false);
        *///?}
    }
}
