package com.baraaoi.reallife.item;

import com.baraaoi.reallife.registry.ModItems;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Грязные деньги из печатного станка. Номинал пачки хранится в NBT "Denom" (10, 100, 500, 1000, 5000);
 * без NBT — старая пачка на 100₽. Пачки разных номиналов не складываются в одну стопку.
 */
public class DirtyMoneyItem extends Item {
    /** Номинал по умолчанию (старые сохранения). */
    public static final int VALUE = 100;
    public static final int[] DENOMS = {10, 100, 500, 1000, 5000};

    public DirtyMoneyItem(Properties props) {
        super(props);
    }

    public static int denom(ItemStack stack) {
        CompoundNBT t = stack.getTag();
        if (t != null && t.contains("Denom")) {
            int d = t.getInt("Denom");
            if (d > 0) return d;
        }
        return VALUE;
    }

    public static ItemStack of(int denom, int count) {
        ItemStack st = new ItemStack(ModItems.DIRTY_MONEY.get(), count);
        if (denom != VALUE) st.getOrCreateTag().putInt("Denom", denom);
        return st;
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        return new StringTextComponent("Грязные деньги " + denom(stack) + "₽");
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        int v = denom(stack);
        tooltip.add(new StringTextComponent("§7Пачка: §2" + v + "₽ §8(происхождение неясно)"));
        if (stack.getCount() > 1) tooltip.add(new StringTextComponent("§7Всего: §2" + ((long) v * stack.getCount()) + "₽"));
        tooltip.add(new StringTextComponent("§8Банк чаще всего изымет. Отмыть: касса своего"));
        tooltip.add(new StringTextComponent("§8заведения (дёшево, но налоговая не дремлет)"));
        tooltip.add(new StringTextComponent("§8или контора МФО (−50%). Братки берут по номиналу."));
    }
}
