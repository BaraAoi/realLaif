package com.baraaoi.reallife.item;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Кирка, которую выдаёт Александр. Копай, должник.
 */
public class DebtorPickaxeItem extends Item {
    public DebtorPickaxeItem(Properties props) {
        super(props.pickaxe(ToolMaterial.IRON, 1, -2.8F).component(net.minecraft.core.component.DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§7Выдана Александром под роспись."));
        tooltip.accept(Component.literal("§7Каждый выкопанный блок идёт в счёт долга."));
        tooltip.accept(Component.literal("§cРуда тоже идёт банку."));
    }
}
