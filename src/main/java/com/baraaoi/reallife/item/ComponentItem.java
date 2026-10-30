package com.baraaoi.reallife.item;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

/** Комплектующие для майнинг-фермы. */
public class ComponentItem extends Item {
    public static final int MOTHERBOARD = 0, CPU = 1, RAM = 2, PSU = 3, COOLER = 4, GPU = 5;
    public static final String[] NAMES = {"Материнская плата", "Процессор", "Оперативная память", "Блок питания", "Кулер", "Видеокарта"};

    private final int kind;

    public ComponentItem(int kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§7Комплектующая для майнинг-фермы."));
        if (kind == GPU) tooltip.accept(Component.literal("§7Чем больше видеокарт (до 4), тем быстрее майнинг."));
        else tooltip.accept(Component.literal("§7Без неё ферма не запустится."));
    }
}
