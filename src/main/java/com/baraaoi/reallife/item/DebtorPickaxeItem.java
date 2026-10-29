package com.baraaoi.reallife.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTier;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Кирка, которую выдаёт Александр. Копай, должник.
 */
public class DebtorPickaxeItem extends PickaxeItem {
    public DebtorPickaxeItem(Properties props) {
        super(ItemTier.IRON, 1, -2.8F, props.maxDamage(0));
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("§7Выдана Александром под роспись."));
        tooltip.add(new StringTextComponent("§7Каждый выкопанный блок идёт в счёт долга."));
        tooltip.add(new StringTextComponent("§cРуда тоже идёт банку."));
    }
}
