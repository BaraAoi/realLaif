package com.baraaoi.reallife.economy;

/**
 * Дома, доступные по ипотеке.
 */
public enum HouseType {
    DIRT_HUT("Землянка из грязи", 300, 6, 450, "5x5, грязь, верстак и сено вместо кровати"),
    WOODEN_HUT("Деревянная изба", 1500, 10, 500, "7x7, кровать, печь, сундук, крыша"),
    STONE_HOUSE("Каменный дом", 4000, 15, 560, "9x8, кухня, стол, книги, 2 сундука"),
    BRICK_COTTAGE("Кирпичный коттедж", 10000, 20, 620, "11x10, 2 этажа, 2 спальни, камин"),
    MANSION("Особняк", 30000, 30, 690, "15x13, сад, балкон, библиотека, 4 кровати"),
    PENTHOUSE("Пентхаус с бассейном", 80000, 40, 760, "башня 4 этажа, терраса и бассейн");

    public final String title;
    public final int price;
    public final int termDays;
    public final int minScore;
    public final String desc;

    HouseType(String title, int price, int termDays, int minScore, String desc) {
        this.desc = desc;
        this.title = title;
        this.price = price;
        this.termDays = termDays;
        this.minScore = minScore;
    }

    public static HouseType byId(int id) {
        HouseType[] v = values();
        return id >= 0 && id < v.length ? v[id] : null;
    }
}
