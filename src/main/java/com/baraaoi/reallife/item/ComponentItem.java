package com.baraaoi.reallife.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
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
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("§7Комплектующая для майнинг-фермы."));
        if (kind == GPU) tooltip.add(new StringTextComponent("§7Чем больше видеокарт (до 4), тем быстрее майнинг."));
        else tooltip.add(new StringTextComponent("§7Без неё ферма не запустится."));
    }
}
