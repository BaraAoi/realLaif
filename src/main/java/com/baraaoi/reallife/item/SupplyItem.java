package com.baraaoi.reallife.item;

import com.baraaoi.reallife.economy.BizType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** Сырьё для заведения (свой вид для каждого типа бизнеса). Лежит на полках/в холодильниках зоны и расходуется работниками. */
public class SupplyItem extends Item {
    public final BizType type;

    public SupplyItem(BizType type, Properties props) {
        super(props);
        this.type = type;
    }

    @Nullable
    public static BizType typeOf(ItemStack st) {
        return !st.isEmpty() && st.getItem() instanceof SupplyItem ? ((SupplyItem) st.getItem()).type : null;
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        return new StringTextComponent(type.supplyName);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("§7Сырьё для: §a" + type.title));
        tooltip.add(new StringTextComponent("§7Каждый работник расходует §e" + BizType.SUPPLY_PER_WORKER + "§7 шт. в день."));
        tooltip.add(new StringTextComponent("§8Держи на полках, стеллажах, в холодильниках или сундуках в зоне заведения."));
    }
}
