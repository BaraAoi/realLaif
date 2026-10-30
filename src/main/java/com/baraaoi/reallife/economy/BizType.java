package com.baraaoi.reallife.economy;

import java.util.Locale;

/**
 * Легальные заведения. Порядок не менять — номер хранится в сохранении и в разрешении.
 * price — цена лицензии (здание игрок строит сам), base — базовая выручка одного работника за игровой день,
 * maxWorkers — сколько работников максимум (если хватает оборудования).
 */
public enum BizType {
    SHAWARMA("Шаурмичная «У Ашота»", "Шаурмист Ашот", 4000, 600, 2, "Ящик продуктов для шаурмы"),
    MINIMARKET("Мини-маркет «Копеечка»", "Продавщица Люда", 7000, 1000, 4, "Коробка товара для магазина"),
    CAR_WASH("Автомойка «Чистюля»", "Мойщик Руслан", 9000, 1200, 3, "Канистра автохимии"),
    QUARRY("Карьер «Гранит»", "Бригадир Степаныч", 12000, 1500, 4, "Бочка солярки для техники"),
    PAWNSHOP("Ломбард «Золотой телец»", "Оценщик Аркадий", 13000, 1600, 2, "Партия «ювелирки» на реализацию"),
    GAS_STATION("Заправка «Роснефть-Лайт»", "Заправщик Вася", 18000, 2200, 3, "Бочка бензина"),
    ELECTRONICS("Магазин техники «Эльдорадио»", "Консультант Денис", 24000, 3000, 4, "Коробка с техникой");

    public final String title;
    public final String workerName;
    public final int price;
    public final int base;
    public final int maxWorkers;
    public final String supplyName;

    BizType(String title, String workerName, int price, int base, int maxWorkers, String supplyName) {
        this.title = title;
        this.workerName = workerName;
        this.price = price;
        this.base = base;
        this.maxWorkers = maxWorkers;
        this.supplyName = supplyName;
    }

    /** Сколько единиц оборудования нужно на ОДНОГО работника (комплект). */
    public int need(BizEquip e) {
        switch (this) {
            case SHAWARMA: return e == BizEquip.COUNTER || e == BizEquip.GRILL || e == BizEquip.FRIDGE ? 1 : 0;
            case MINIMARKET: return e == BizEquip.COUNTER || e == BizEquip.FRIDGE || e == BizEquip.RACK ? 1 : e == BizEquip.SHELF ? 3 : 0;
            case CAR_WASH: return e == BizEquip.COUNTER || e == BizEquip.RACK ? 1 : e == BizEquip.WASHER ? 2 : 0;
            case QUARRY: return e == BizEquip.COUNTER || e == BizEquip.CRUSHER ? 1 : e == BizEquip.RACK ? 2 : 0;
            case PAWNSHOP: return e == BizEquip.COUNTER || e == BizEquip.SAFE ? 1 : e == BizEquip.DISPLAY ? 2 : 0;
            case GAS_STATION: return e == BizEquip.COUNTER || e == BizEquip.SHELF ? 1 : e == BizEquip.PUMP ? 2 : 0;
            case ELECTRONICS: return e == BizEquip.COUNTER || e == BizEquip.RACK ? 1 : e == BizEquip.DISPLAY ? 3 : e == BizEquip.SHELF ? 2 : 0;
            default: return 0;
        }
    }

    /** Цена одного комплекта оборудования. */
    public int kitPrice() {
        int s = 0;
        for (BizEquip e : BizEquip.values()) s += need(e) * e.price;
        return s;
    }

    /** Сырьё: сколько единиц в день расходует один работник. */
    public static final int SUPPLY_PER_WORKER = 4;

    /** Цена единицы сырья (≈30% выручки уходит на закупку). */
    public int supplyPrice() {
        return (int) (Math.round(base * 0.3 / SUPPLY_PER_WORKER / 5.0) * 5);
    }

    /** Зарплата работника в день (четверть базовой прибыли). */
    public int wage() {
        return base / 4;
    }

    /** Сколько стоит нанять работника (подъёмные: две зарплаты). */
    public int hireCost() {
        return wage() * 2;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BizType byId(int i) {
        BizType[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }
}
