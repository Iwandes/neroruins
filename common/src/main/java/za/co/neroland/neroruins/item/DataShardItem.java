package za.co.neroland.neroruins.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import za.co.neroland.neroruins.config.NeroRuinsConfig;
import za.co.neroland.neroruins.data.PlayerArchive;
import za.co.neroland.neroruins.lore.LogCatalog;
import za.co.neroland.neroruins.lore.ArchiveReading;
import za.co.neroland.neroruins.progression.RuinMilestones;
import za.co.neroland.neroruins.registry.ModItems;

public final class DataShardItem extends Item {
    public DataShardItem(Properties properties) { super(properties); }

    @Override
    public Component getName(ItemStack stack) {
        return LogCatalog.translationPrefix(stack.get(ModItems.LOG_ID.get()))
                .<Component>map(prefix -> Component.translatable("item.neroruins.data_shard.named",
                        Component.translatable(prefix + ".title")))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    //? if >=1.21.2 {
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //?} else {
    /*public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    *///?}
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            String id = stack.get(ModItems.LOG_ID.get());
            var prefix = LogCatalog.translationPrefix(id);
            if (prefix.isEmpty()) {
                message(serverPlayer, Component.translatable("message.neroruins.shard.invalid"));
            } else {
                PlayerArchive archive = PlayerArchive.get(level.getServer());
                var result = ArchiveReading.record(archive, serverPlayer.getUUID(), id, System.currentTimeMillis());
                if (result == ArchiveReading.Result.ARCHIVED || result == ArchiveReading.Result.CHAPTER_COMPLETED) {
                    showLog(serverPlayer, prefix.get());
                    message(serverPlayer, Component.translatable("message.neroruins.shard.saved"));
                    if (result == ArchiveReading.Result.CHAPTER_COMPLETED) {
                        message(serverPlayer, Component.translatable("message.neroruins.codex.unlocked",
                                Component.translatable("codex.neroruins." + LogCatalog.chapterOf(id).orElseThrow() + ".title")));
                    }
                    RuinMilestones.firstLog(serverPlayer);
                    if (!NeroRuinsConfig.KEEP_SHARDS.get() && !serverPlayer.getAbilities().instabuild) stack.shrink(1);
                } else {
                    showLog(serverPlayer, prefix.get());
                    message(serverPlayer, Component.translatable("message.neroruins.shard.duplicate"));
                }
            }
        }
        //? if >=1.21.2 {
        return InteractionResult.SUCCESS;
        //?} else {
        /*return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        *///?}
    }

    private static void showLog(ServerPlayer player, String prefix) {
        message(player, Component.translatable(prefix + ".title"));
        message(player, Component.translatable(prefix + ".speaker"));
        message(player, Component.translatable(prefix + ".body"));
    }

    private static void message(ServerPlayer player, Component text) {
        //? if >=26.1 {
        player.sendSystemMessage(text);
        //?} else {
        /*player.displayClientMessage(text, false);
        *///?}
    }
}
