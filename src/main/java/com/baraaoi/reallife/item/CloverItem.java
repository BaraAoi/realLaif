package com.baraaoi.reallife.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** Клевер абсолютной удачи: пока лежит в инвентаре, каждая игра в казино — лучший исход. */
public class CloverItem extends Item {
    public static final int USES = 8;

    public CloverItem(Properties props) {
        super(props.maxDamage(USES));
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("§aАбсолютная удача: в казино всегда выпадает лучший исход"));
        tooltip.add(new StringTextComponent("§7Осталось игр: §f" + (stack.getMaxDamage() - stack.getDamage())));
        tooltip.add(new StringTextComponent("§8Охрана казино не любит слишком везучих..."));
    }
}
