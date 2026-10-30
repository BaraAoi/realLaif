package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.registry.ModBlocks;
import net.minecraft.block.Block;

import java.util.function.Supplier;

/**
 * Оборудование заведений v6: покупается в телефоне (Бизнес → Оборудование) и расставляется игроком
 * в зоне заведения (вокруг главной кассы). Порядок не менять — номер уходит в пакеты.
 */
public enum BizEquip {
    COUNTER("Касса", 1500, () -> ModBlocks.BIZ_COUNTER.get()),
    SHELF("Торговая полка", 400, () -> ModBlocks.SHOP_SHELF.get()),
    RACK("Складской стеллаж", 500, () -> ModBlocks.STORAGE_RACK.get()),
    FRIDGE("Холодильник", 600, () -> ModBlocks.FRIDGE.get()),
    GRILL("Гриль для шаурмы", 1200, () -> ModBlocks.SHAWARMA_GRILL.get()),
    WASHER("Мойка высокого давления", 1500, () -> ModBlocks.PRESSURE_WASHER.get()),
    CRUSHER("Камнедробилка", 4000, () -> ModBlocks.STONE_CRUSHER.get()),
    SAFE("Сейф", 2500, () -> ModBlocks.SAFE.get()),
    DISPLAY("Витрина", 900, () -> ModBlocks.DISPLAY_CASE.get()),
    PUMP("Топливная колонка", 3000, () -> ModBlocks.FUEL_PUMP.get());

    public final String title;
    public final int price;
    public final Supplier<Block> block;

    BizEquip(String title, int price, Supplier<Block> block) {
        this.title = title;
        this.price = price;
        this.block = block;
    }

    public static BizEquip byId(int i) {
        BizEquip[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }

    public static BizEquip of(Block b) {
        for (BizEquip e : values()) if (e.block.get() == b) return e;
        return null;
    }
}
