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

    /** Сколько единиц оборудования нужно на ОДНОГО работника (комплект). Склад сюда не входит — см. {@link #baseNeed}. */
    public int need(BizEquip e) {
        switch (this) {
            case SHAWARMA: return e == BizEquip.COUNTER || e == BizEquip.GRILL || e == BizEquip.FRIDGE ? 1 : 0;
            case MINIMARKET: return e == BizEquip.COUNTER || e == BizEquip.FRIDGE ? 1 : e == BizEquip.SHELF ? 3 : 0;
            case CAR_WASH: return e == BizEquip.COUNTER ? 1 : e == BizEquip.WASHER ? 2 : 0;
            case QUARRY: return e == BizEquip.COUNTER || e == BizEquip.CRUSHER ? 1 : 0;
            case PAWNSHOP: return e == BizEquip.COUNTER || e == BizEquip.SAFE ? 1 : e == BizEquip.DISPLAY ? 2 : 0;
            case GAS_STATION: return e == BizEquip.COUNTER || e == BizEquip.SHELF ? 1 : e == BizEquip.PUMP ? 2 : 0;
            case ELECTRONICS: return e == BizEquip.COUNTER ? 1 : e == BizEquip.DISPLAY ? 3 : e == BizEquip.SHELF ? 2 : 0;
            default: return 0;
        }
    }

    /** Склад: один складской стеллаж на всё заведение (сколько ставить сверх — решает игрок, это запас сырья). */
    public static final int BASE_RACKS = 1;

    /** Сколько оборудования нужно на заведение целиком, независимо от числа работников. */
    public int baseNeed(BizEquip e) {
        return e == BizEquip.RACK ? BASE_RACKS : 0;
    }

    /** Сколько всего оборудования нужно для workers работников. */
    public int total(BizEquip e, int workers) {
        return baseNeed(e) + need(e) * workers;
    }

    /** Цена одного комплекта оборудования. */
    public int kitPrice() {
        int s = 0;
        for (BizEquip e : BizEquip.values()) s += need(e) * e.price;
        return s;
    }

    /** v1.10: прибыль бизнесов поднята — выручка работника = base × REVENUE_BOOST (зарплаты и сырьё остались прежними). */
    public static final double REVENUE_BOOST = 1.75;

    /** Выручка одного работника в обычный день (без удачи и бонуса ТЦ). */
    public int revenue() {
        return (int) Math.round(base * REVENUE_BOOST);
    }

    /** v1.10: «под ключ» — работа бригады строителей (здание с освещением и вывеской). */
    public static final int BUILD_FEE = 8000;

    /** Оборудование на максимум работников (главная касса идёт с лицензией, поэтому одна касса не считается). */
    public int fullEquipPrice() {
        int s = 0;
        for (BizEquip e : BizEquip.values()) s += total(e, maxWorkers) * e.price;
        return s - BizEquip.COUNTER.price;
    }

    /** Лицензия + стройка + всё оборудование на максимум работников. */
    public int turnkeyPrice() {
        return price + fullEquipPrice() + BUILD_FEE;
    }

    /** Сырьё: сколько коробок в день расходует один работник (v1.7: коробки не стакаются — одна коробка на слот). */
    public static final int SUPPLY_PER_WORKER = 1;

    /** Цена одной коробки сырья (≈30% выручки уходит на закупку). */
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
