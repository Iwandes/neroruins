package za.co.neroland.neroruins.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * One of the nine named chapter relics. In this release a relic is a collectable with a lore
 * tooltip; its in-world uses (relic sockets and fixtures) arrive with the puzzle blocks.
 * Tooltip keys: {@code item.neroruins.<id>.tooltip.0} (description) and {@code .tooltip.1} (lore).
 */
public final class RelicItem extends Item {
    public RelicItem(Properties properties) { super(properties); }

    //? if >=1.21.5 {
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            net.minecraft.world.item.component.TooltipDisplay display,
            java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(line(0));
        tooltip.accept(line(1));
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(line(0));
        tooltip.add(line(1));
    }
    *///?}

    private Component line(int index) {
        MutableComponent text = Component.translatable(getDescriptionId() + ".tooltip." + index);
        return index == 0
                ? text.withStyle(ChatFormatting.GRAY)
                : text.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
    }
}
