package com.baraaoi.reallife.item;

import com.baraaoi.reallife.registry.ModItems;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

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
        CompoundTag t = com.baraaoi.reallife.util.Nbt.itemTag(stack);
        if (t != null && t.contains("Denom")) {
            int d = t.getIntOr("Denom", 0);
            if (d > 0) return d;
        }
        return VALUE;
    }

    public static ItemStack of(int denom, int count) {
        ItemStack st = new ItemStack(ModItems.DIRTY_MONEY.get(), count);
        if (denom != VALUE) com.baraaoi.reallife.util.Nbt.editItem(st, _t -> _t.putInt("Denom", denom));
        return st;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Грязные деньги " + denom(stack) + "₽");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        int v = denom(stack);
        tooltip.accept(Component.literal("§7Пачка: §2" + v + "₽ §8(происхождение неясно)"));
        if (stack.getCount() > 1) tooltip.accept(Component.literal("§7Всего: §2" + ((long) v * stack.getCount()) + "₽"));
        tooltip.accept(Component.literal("§8Банк чаще всего изымет. Отмыть: касса своего"));
        tooltip.accept(Component.literal("§8заведения (дёшево, но налоговая не дремлет)"));
        tooltip.accept(Component.literal("§8или контора МФО (−50%). Братки берут по номиналу."));
    }
}
