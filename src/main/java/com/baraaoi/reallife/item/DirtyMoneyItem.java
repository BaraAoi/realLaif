package com.baraaoi.reallife.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** Грязные деньги из подпольного цеха: пачка 100₽ «неясного происхождения». */
public class DirtyMoneyItem extends Item {
    public static final int VALUE = 100;

    public DirtyMoneyItem(Properties props) {
        super(props);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("§7Пачка: §2" + VALUE + "₽ §8(происхождение неясно)"));
        if (stack.getCount() > 1) tooltip.add(new StringTextComponent("§7Всего: §2" + (VALUE * stack.getCount()) + "₽"));
        tooltip.add(new StringTextComponent("§8Банк примет с шансом 50% — или изымет."));
        tooltip.add(new StringTextComponent("§8Надёжнее отмыть через ООО в МФО."));
        tooltip.add(new StringTextComponent("§8Братки и МФО берут по номиналу."));
    }
}
