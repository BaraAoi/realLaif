package com.baraaoi.reallife.item;

import com.baraaoi.reallife.economy.Finance;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
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
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("§7Номинал: §a" + Finance.money(value)));
        if (stack.getCount() > 1) {
            tooltip.add(new StringTextComponent("§7В пачке: §a" + Finance.money((double) value * stack.getCount())));
        }
        tooltip.add(new StringTextComponent("§8Билет Банка «Реал Банк»"));
    }
}
