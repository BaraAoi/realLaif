package com.baraaoi.reallife.item;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

/** Клевер абсолютной удачи: пока лежит в инвентаре, каждая игра в казино — лучший исход. */
public class CloverItem extends Item {
    public static final int USES = 8;

    public CloverItem(Properties props) {
        super(props.durability(USES));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§aАбсолютная удача: в казино всегда выпадает лучший исход"));
        tooltip.accept(Component.literal("§7Осталось игр: §f" + (stack.getMaxDamage() - stack.getDamageValue())));
        tooltip.accept(Component.literal("§8Охрана казино не любит слишком везучих..."));
    }
}
