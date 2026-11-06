package za.co.neroland.neroruins.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import za.co.neroland.neroruins.NeroRuinsCommon;

public final class MenuOpener {
    private MenuOpener() { }

    public static InteractionResult openOrConsume(ServerPlayer player, MenuProvider provider) {
        try {
            if (player.openMenu(provider).isPresent()) return InteractionResult.SUCCESS;
            player.sendSystemMessage(Component.translatable("gui.neroruins.menu_open_failed"));
            return InteractionResult.CONSUME;
        } catch (RuntimeException | LinkageError failure) {
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] Could not open menu {} ({})",
                    provider.getClass().getName(), failure.getClass().getName());
            try {
                player.closeContainer();
                player.sendSystemMessage(Component.translatable("gui.neroruins.menu_open_failed"));
            } catch (RuntimeException | LinkageError recoveryFailure) {
                NeroRuinsCommon.LOGGER.warn("[NeroRuins] Could not recover after a menu failed to open ({})",
                        recoveryFailure.getClass().getName());
            }
            return InteractionResult.CONSUME;
        }
    }
}
