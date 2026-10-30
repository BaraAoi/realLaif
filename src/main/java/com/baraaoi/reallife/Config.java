package com.baraaoi.reallife;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Общие настройки мода (config/reallife-common.toml).
 */
public class Config {
    public static final ForgeConfigSpec SPEC;

    // ===== Банк =====
    public static final ForgeConfigSpec.DoubleValue DEPOSIT_DAILY_RATE;
    public static final ForgeConfigSpec.DoubleValue LATE_PENALTY;
    public static final ForgeConfigSpec.IntValue ZHORA_DAYS;
    public static final ForgeConfigSpec.IntValue ALEXANDER_DAYS;
    public static final ForgeConfigSpec.IntValue POLICE_DAYS;
    public static final ForgeConfigSpec.IntValue POLICE_COUNT;
    public static final ForgeConfigSpec.IntValue JAIL_TICKS;
    public static final ForgeConfigSpec.DoubleValue MORTGAGE_DOWN_PAYMENT;

    // ===== МФО =====
    public static final ForgeConfigSpec.DoubleValue MFO_DAILY_RATE;
    public static final ForgeConfigSpec.DoubleValue MFO_OVERDUE_RATE;
    public static final ForgeConfigSpec.DoubleValue MFO_COMMISSION;
    public static final ForgeConfigSpec.IntValue MFO_TERM_DAYS;
    public static final ForgeConfigSpec.IntValue MFO_MAX_LOAN;
    public static final ForgeConfigSpec.IntValue VOVAN_DAYS;
    public static final ForgeConfigSpec.IntValue BRATKI_DAYS;
    public static final ForgeConfigSpec.IntValue LETHAL_DAYS;

    // ===== Взятки =====
    public static final ForgeConfigSpec.IntValue POLICE_BRIBE;
    public static final ForgeConfigSpec.IntValue BRATOK_BRIBE;

    // ===== Земля / самострой =====
    public static final ForgeConfigSpec.BooleanValue LAND_CONTROL;
    public static final ForgeConfigSpec.IntValue LAND_THRESHOLD;
    public static final ForgeConfigSpec.IntValue LAND_PRICE;
    public static final ForgeConfigSpec.IntValue LAND_FINE;
    public static final ForgeConfigSpec.IntValue LAND_DEADLINE;

    // ===== Подпольный бизнес =====
    public static final ForgeConfigSpec.IntValue BUSINESS_PRICE;
    public static final ForgeConfigSpec.IntValue BUSINESS_DAILY;
    public static final ForgeConfigSpec.IntValue LAUNDER_PRICE;
    public static final ForgeConfigSpec.DoubleValue LAUNDER_COMMISSION;
    public static final ForgeConfigSpec.DoubleValue DIRTY_CAUGHT_CHANCE;
    public static final ForgeConfigSpec.DoubleValue MFO_LAUNDER_COMMISSION;
    public static final ForgeConfigSpec.IntValue PRINTER_PRICE;
    public static final ForgeConfigSpec.IntValue PRINTER_DAILY;
    public static final ForgeConfigSpec.IntValue MAX_PRINTERS;
    public static final ForgeConfigSpec.IntValue PRINTER_COST;
    public static final ForgeConfigSpec.IntValue RAID_FINE;
    public static final ForgeConfigSpec.IntValue RAID_JAIL_TICKS;
    public static final ForgeConfigSpec.IntValue SPOT_RANGE;

    // ===== Легальный бизнес / крипта =====
    public static final ForgeConfigSpec.DoubleValue LAUNDER_SAFE_SHARE;
    public static final ForgeConfigSpec.DoubleValue LAUNDER_RISK_SLOPE;
    public static final ForgeConfigSpec.DoubleValue LAUNDER_TAX;
    public static final ForgeConfigSpec.DoubleValue TAX_FINE_MULT;
    public static final ForgeConfigSpec.IntValue TAX_JAIL_TICKS;
    public static final ForgeConfigSpec.DoubleValue BTC_PER_GPU_SECOND;
    public static final ForgeConfigSpec.IntValue BTC_BASE_RATE;

    // ===== Казино =====
    public static final ForgeConfigSpec.BooleanValue SPAWN_CASINO;
    public static final ForgeConfigSpec.IntValue CASINO_MAX_BET;
    public static final ForgeConfigSpec.BooleanValue EVENTS_ENABLED;
    public static final ForgeConfigSpec.IntValue EVENTS_MIN_MINUTES;
    public static final ForgeConfigSpec.IntValue EVENTS_MAX_MINUTES;

    // ===== Ограбление банка =====
    public static final ForgeConfigSpec.IntValue ALARM_DELAY;
    public static final ForgeConfigSpec.IntValue CASHIER_LOOT_MIN;
    public static final ForgeConfigSpec.IntValue CASHIER_LOOT_MAX;

    // ===== Стройка домов =====
    public static final ForgeConfigSpec.IntValue HOUSE_FLATNESS;

    // ===== Мир =====
    public static final ForgeConfigSpec.BooleanValue SPAWN_BUILDINGS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Банк «Реал Банк»").push("bank");
        DEPOSIT_DAILY_RATE = b.comment("Процент на остаток по вкладу в игровой день (0.003 = 0.3%)")
                .defineInRange("depositDailyRate", 0.003, 0.0, 1.0);
        LATE_PENALTY = b.comment("Пени за просрочку: доля от суммы просрочки, начисляемая каждый день")
                .defineInRange("latePenalty", 0.02, 0.0, 1.0);
        ZHORA_DAYS = b.comment("Через сколько дней просрочки приходит вежливый Жора")
                .defineInRange("zhoraDays", 1, 1, 100);
        ALEXANDER_DAYS = b.comment("Через сколько дней просрочки приходит Александр и заставляет копать")
                .defineInRange("alexanderDays", 3, 1, 100);
        POLICE_DAYS = b.comment("Через сколько дней просрочки выезжает полиция")
                .defineInRange("policeDays", 5, 1, 100);
        POLICE_COUNT = b.comment("Сколько полицейских в первой волне")
                .defineInRange("policeCount", 5, 1, 30);
        JAIL_TICKS = b.comment("Срок в тюрьме в тиках (24000 = 1 игровой день = 20 минут)")
                .defineInRange("jailTicks", 12000, 200, 1000000);
        MORTGAGE_DOWN_PAYMENT = b.comment("Первоначальный взнос по ипотеке (0.2 = 20%)")
                .defineInRange("mortgageDownPayment", 0.2, 0.0, 1.0);
        b.pop();

        b.comment("МФО «Шайкин и Ко»").push("mfo");
        MFO_DAILY_RATE = b.comment("Сложный процент в день в срок займа (0.1 = 10% в день)")
                .defineInRange("dailyRate", 0.10, 0.0, 10.0);
        MFO_OVERDUE_RATE = b.comment("Сложный процент в день при просрочке")
                .defineInRange("overdueRate", 0.20, 0.0, 10.0);
        MFO_COMMISSION = b.comment("Комиссия/«страховка», удерживаемая при выдаче")
                .defineInRange("commission", 0.15, 0.0, 0.9);
        MFO_TERM_DAYS = b.comment("Срок микрозайма в игровых днях")
                .defineInRange("termDays", 3, 1, 100);
        MFO_MAX_LOAN = b.comment("Максимальная сумма микрозайма")
                .defineInRange("maxLoan", 3000, 10, 1000000);
        VOVAN_DAYS = b.comment("Через сколько дней просрочки приходит коллектор Вован")
                .defineInRange("vovanDays", 1, 1, 100);
        BRATKI_DAYS = b.comment("Через сколько дней просрочки приезжают братки (бьют, грабят, поджигают)")
                .defineInRange("bratkiDays", 2, 1, 100);
        LETHAL_DAYS = b.comment("Через сколько дней братки приходят «с паяльником» (убивают и увозят в подвал)")
                .defineInRange("lethalDays", 4, 1, 100);
        b.pop();

        b.comment("Взятки: деньги, брошенные на землю, подбирают полицейские и братки").push("bribes");
        POLICE_BRIBE = b.comment("Сколько рублей в сумме должна подобрать полиция, чтобы уехать (на 1 игровой день)")
                .defineInRange("policeBribe", 5000, 1, 10000000);
        BRATOK_BRIBE = b.comment("Сколько рублей должен подобрать каждый браток, чтобы уйти")
                .defineInRange("bratokBribe", 1000, 1, 10000000);
        b.pop();

        b.comment("Земля и самострой").push("land");
        LAND_CONTROL = b.comment("Включить контроль самостроя (штрафы и выселение)")
                .define("enabled", true);
        LAND_THRESHOLD = b.comment("Сколько блоков игрок может поставить в чужом/ничейном чанке, прежде чем это сочтут самостроем")
                .defineInRange("blocksThreshold", 48, 1, 100000);
        LAND_PRICE = b.comment("Цена одного чанка 16x16 (участок покупается сразу 3x3 чанка: цена × число свободных чанков)")
                .defineInRange("chunkPrice", 300, 0, 10000000);
        LAND_FINE = b.comment("Штраф за самострой")
                .defineInRange("fine", 150, 0, 10000000);
        LAND_DEADLINE = b.comment("Сколько тиков даётся на снос или покупку земли до выселения (12000 = 10 минут)")
                .defineInRange("deadlineTicks", 12000, 200, 10000000);
        b.pop();

        b.comment("Подпольный бизнес и отмывание").push("business");
        BUSINESS_PRICE = b.comment("Устарело (версия 4): цеха больше нет, станки покупаются по одному (printerPrice)")
                .defineInRange("businessPrice", 5000, 0, 10000000);
        BUSINESS_DAILY = b.comment("Устарело (версия 2). Доход теперь считается по станкам: printerDaily")
                .defineInRange("dailyDirtyMoney", 500, 0, 10000000);
        PRINTER_DAILY = b.comment("Сколько грязных денег печатает один станок за игровой день (кратно 100)")
                .defineInRange("printerDaily", 300, 0, 10000000);
        PRINTER_PRICE = b.comment("Устарело (версия 5): цена станка теперь printerCost")
                .defineInRange("printerPrice", 3000, 0, 10000000);
        MAX_PRINTERS = b.comment("Устарело (версия 5): лимита станков больше нет")
                .defineInRange("maxPrinters", 8, 1, 1000);
        PRINTER_COST = b.comment("Цена печатного станка в МФО (первая покупка даёт ещё и «Сундук цеха»)")
                .defineInRange("printerCost", 1000, 0, 10000000);
        RAID_FINE = b.comment("Штраф ОБЭП за каждый найденный станок")
                .defineInRange("raidFine", 3000, 0, 10000000);
        RAID_JAIL_TICKS = b.comment("Сколько тиков СИЗО, если ОБЭП нашёл станок (3600 = 3 минуты)")
                .defineInRange("raidJailTicks", 3600, 0, 1000000);
        SPOT_RANGE = b.comment("С какого расстояния NPC замечают станок (если его видно)")
                .defineInRange("spotRange", 12, 2, 64);
        LAUNDER_PRICE = b.comment("Цена ООО «Отмыв»")
                .defineInRange("launderPrice", 8000, 0, 10000000);
        LAUNDER_COMMISSION = b.comment("Комиссия ООО «Отмыв» (аппарат у игрока дома)")
                .defineInRange("launderCommission", 0.3, 0.0, 1.0);
        MFO_LAUNDER_COMMISSION = b.comment("Комиссия, если отмывать прямо в конторе МФО")
                .defineInRange("mfoLaunderCommission", 0.5, 0.0, 1.0);
        DIRTY_CAUGHT_CHANCE = b.comment("Шанс, что банк «спалит» грязные деньги при внесении и изымет их")
                .defineInRange("dirtyCaughtChance", 0.8, 0.0, 1.0);
        b.pop();

        b.comment("Легальный бизнес (заведения) и отмывание через кассу").push("legal");
        LAUNDER_SAFE_SHARE = b.comment("Сколько грязных в день можно провести через кассу без риска (доля базовой прибыли заведения)")
                .defineInRange("launderSafeShare", 0.15, 0.0, 10.0);
        LAUNDER_RISK_SLOPE = b.comment("Как быстро растёт риск сверх безопасной доли: шанс = (сумма/база - безопасная доля) * slope")
                .defineInRange("launderRiskSlope", 1.5, 0.0, 100.0);
        LAUNDER_TAX = b.comment("Налог с отмытой выручки (НДФЛ)")
                .defineInRange("launderTax", 0.13, 0.0, 1.0);
        TAX_FINE_MULT = b.comment("Штраф налоговой при поимке: доля от (базовой прибыли заведения + суммы грязных)")
                .defineInRange("taxFineMult", 2.0, 0.0, 100.0);
        TAX_JAIL_TICKS = b.comment("Сколько тиков СИЗО при поимке налоговой (6000 = 5 минут)")
                .defineInRange("taxJailTicks", 6000, 0, 1000000);
        b.pop();

        b.comment("Криптовалюта").push("crypto");
        BTC_PER_GPU_SECOND = b.comment("Сколько BTC добывает одна видеокарта за секунду")
                .defineInRange("btcPerGpuSecond", 0.0000005, 0.0, 1.0);
        BTC_BASE_RATE = b.comment("Средний курс BTC в рублях (курс гуляет вокруг него)")
                .defineInRange("btcBaseRate", 1000000, 1000, 1000000000);
        b.pop();

        b.comment("Мир").push("world");
        SPAWN_BUILDINGS = b.comment("Построить банк и МФО на точке спавна при первом запуске мира")
                .define("spawnBuildings", true);
        SPAWN_CASINO = b.comment("Построить казино недалеко от спавна (в пределах 12 чанков)")
                .define("spawnCasino", true);
        HOUSE_FLATNESS = b.comment("Допустимый перепад высот под ипотечным домом (блоков)")
                .defineInRange("houseFlatness", 4, 0, 64);
        b.pop();

        b.comment("Казино").push("casino");
        CASINO_MAX_BET = b.comment("Максимальная ставка").defineInRange("maxBet", 1000, 10, 10000000);
        b.pop();

        b.comment("Ограбление банка").push("robbery");
        ALARM_DELAY = b.comment("Через сколько тиков после удара по кассиру приезжает полиция (600 = 30 сек)")
                .defineInRange("alarmDelay", 600, 20, 100000);
        CASHIER_LOOT_MIN = b.comment("Сколько денег минимум выпадает из кассы убитого кассира").defineInRange("cashierLootMin", 800, 0, 10000000);
        CASHIER_LOOT_MAX = b.comment("Сколько денег максимум").defineInRange("cashierLootMax", 2500, 0, 10000000);
        b.pop();

        b.comment("Случайные гости: налоговая, сантехник, цыганка, гопник, конокрад и другие").push("events");
        EVENTS_ENABLED = b.comment("Включить случайных гостей").define("enabled", true);
        EVENTS_MIN_MINUTES = b.comment("Минимальный интервал между гостями (реальные минуты). Бездомным и путешественникам гости приходят ещё в 1.4 раза чаще").defineInRange("intervalMinMinutes", 3, 1, 1000);
        EVENTS_MAX_MINUTES = b.comment("Максимальный интервал между гостями (реальные минуты)").defineInRange("intervalMaxMinutes", 7, 1, 1000);
        b.pop();
        SPEC = b.build();
    }
}
