package com.baraaoi.reallife.item;

import com.baraaoi.reallife.economy.Finance;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

public class BanknoteItem extends Item {
    private final int value;

    public BanknoteItem(int value, Properties props) {
        super(props);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§7Номинал: §a" + Finance.money(value)));
        if (stack.getCount() > 1) {
            tooltip.accept(Component.literal("§7В пачке: §a" + Finance.money((double) value * stack.getCount())));
        }
        tooltip.accept(Component.literal("§8Билет Банка «Реал Банк»"));
    }
}
