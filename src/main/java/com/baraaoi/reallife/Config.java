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
        SPEC = b.build();
    }
}
