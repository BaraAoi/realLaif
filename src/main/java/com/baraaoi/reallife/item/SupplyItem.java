package com.baraaoi.reallife.item;

import com.baraaoi.reallife.economy.BizType;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

/** Сырьё для заведения (свой вид для каждого типа бизнеса). Лежит на полках/в холодильниках зоны и расходуется работниками. */
public class SupplyItem extends Item {
    public final BizType type;

    public SupplyItem(BizType type, Properties props) {
        super(props);
        this.type = type;
    }

    
    public static BizType typeOf(ItemStack st) {
        return !st.isEmpty() && st.getItem() instanceof SupplyItem ? ((SupplyItem) st.getItem()).type : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(type.supplyName);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§7Сырьё для: §a" + type.title));
        tooltip.accept(Component.literal("§7Каждый работник расходует §e" + BizType.SUPPLY_PER_WORKER + "§7 коробку в день."));
        tooltip.accept(Component.literal("§8Не стакается: одна коробка — один слот. Храни на складских стеллажах,"));
        tooltip.accept(Component.literal("§8полках, в холодильниках или сундуках в зоне заведения."));
    }
}
