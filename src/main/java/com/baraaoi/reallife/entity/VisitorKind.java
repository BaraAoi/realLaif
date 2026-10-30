package com.baraaoi.reallife.entity;

import java.util.Locale;

/** Виды случайных гостей. Порядок не менять — номер хранится в сущности. */
public enum VisitorKind {
    TAX("§6Налоговый инспектор", false),
    TAX_POLICE("§9Полицейский", true),
    BAILIFF("§8Судебный пристав", false),
    PLUMBER("§3Сантехник Михалыч", false),
    ELECTRIC("§eЭлектрик Энергосбыта", false),
    GYPSY("§dЦыганка Роза", false),
    GOPNIK("§7Гопник Колян", false),
    GOPNIK_FRIEND("§7Кореш Коляна", true),
    DEALER("§5Барыга", false),
    HORSE_THIEF("§cКонокрад", false),
    COURIER("§eКурьер Яндекс.Еды", false),
    REALTOR("§bРиелтор Эдуард", false),
    GRANNY("§dБабушка Зина", false),
    FIRE_INSPECTOR("§cПожарный инспектор", false),
    FIREMAN("§cПожарный", true),
    NEIGHBOR("§2Сосед Петрович", false),
    PROMOTER("§aПромоутер «ДеньгиСразу»", false),
    GRANDPA("§6Дед Митрич", false),
    PHILOSOPHER("§8Коллектор-философ", false),
    BANK_SCAMMER("§9«Служба безопасности банка»", false),
    POSTMAN("§9Почтальон Почты России", false),
    SOCIOLOGIST("§fСоциолог ВЦИОМ", false),
    /** Ходит вокруг станков (приходит чаще, чем больше станков) — управляется {@link com.baraaoi.reallife.event.Raids}. */
    SNOOP("§8Подозрительный тип", true),
    /** Опер ОБЭП — приходит по наводке и копает к станку. */
    OBEP("§9Оперативник ОБЭП", true),
    // v1.7: гости для бездомных, шахтёров и путешественников
    UCHASTKOVY("§9Участковый Анискин", false),
    BLACK_DIGGER("§8Чёрный копатель Сиплый", false),
    LOST_CAVER("§eЗаблудившийся спелеолог Вася", false),
    ECO_INSPECTOR("§2Инспектор Росприроднадзора", false),
    HITCHHIKER("§bАвтостопщик Гоша", false),
    PEDDLER("§6Коробейник Ашот", false),
    HOBO("§7Бомж Валера", false),
    TREASURE_HUNTER("§6Кладоискатель Семёныч", false),
    DPS("§9Инспектор ДПС Петренко", false),
    // v1.9: страшные события «здесь и сейчас»
    SANEPID("§2Инспектор СЭС Зловредова", false),
    MIGRATION("§9Майор миграционной службы", false),
    FIRE("§4Пожар", false),
    CAVE_IN("§6Обвал", false),
    BURGLAR("§8Домушник", false);

    public final String display;
    /** Помощник — сам по себе не приходит, только вместе с главным гостем. */
    public final boolean helper;

    VisitorKind(String display, boolean helper) {
        this.display = display;
        this.helper = helper;
    }

    /** Имя текстуры: textures/entity/visitor/&lt;id&gt;.png */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static VisitorKind byId(int i) {
        VisitorKind[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }

    public static VisitorKind byName(String s) {
        for (VisitorKind k : values()) if (k.id().equalsIgnoreCase(s)) return k;
        return null;
    }
}
