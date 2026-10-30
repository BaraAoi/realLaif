package com.baraaoi.reallife.economy;

import java.util.Locale;

/**
 * Легальные заведения. Порядок не менять — номер хранится в сохранении и в разрешении на стройку.
 * price — цена разрешения (стройка бесплатно), base — базовая прибыль за игровой день (до зарплаты),
 * wage — зарплата работника в день.
 */
public enum BizType {
    SHAWARMA("Шаурмичная «У Ашота»", "Шаурмист Ашот", 8000, 600),
    MINIMARKET("Мини-маркет «Копеечка»", "Продавщица Люда", 14000, 1000),
    CAR_WASH("Автомойка «Чистюля»", "Мойщик Руслан", 18000, 1200),
    QUARRY("Карьер «Гранит»", "Бригадир Степаныч", 24000, 1500),
    PAWNSHOP("Ломбард «Золотой телец»", "Оценщик Аркадий", 26000, 1600),
    GAS_STATION("Заправка «Роснефть-Лайт»", "Заправщик Вася", 36000, 2200),
    ELECTRONICS("Магазин техники «Эльдорадио»", "Консультант Денис", 48000, 3000);

    public final String title;
    public final String workerName;
    public final int price;
    public final int base;

    BizType(String title, String workerName, int price, int base) {
        this.title = title;
        this.workerName = workerName;
        this.price = price;
        this.base = base;
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
