package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.util.Nbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;

import java.util.UUID;

/**
 * Всё, что банк и МФО знают об игроке.
 */
public class Account {
    public static final int FREE = 0, JAIL = 1, PIT = 2;

    public final UUID id;
    public String name = "";

    // --- счёт ---
    public double balance;
    public double lifetimeDeposited;
    public long createdDay = -1;

    // --- кредитная история ---
    public int onTimePayments;
    public int lateDaysTotal;
    public int defaults;

    // --- потребительский кредит ---
    public double loanPrincipal;
    public double loanRate;
    public int loanDays;

    // --- ипотека ---
    public double mortPrincipal;
    public double mortRate;
    public int mortDays;
    public int mortHouse = -1;
    public long houseOrderId;
    public boolean houseBuilt;

    // --- дом игрока (построенный по ипотеке) ---
    public boolean hasHouse;
    public int houseType = -1;
    public BlockPos houseMin = BlockPos.ZERO;
    public BlockPos houseMax = BlockPos.ZERO;

    // --- просрочка в банке ---
    public double arrears;
    public int overdueDays;
    public boolean wanted;
    public int policeWave;
    public long nextPoliceTick;
    public int laborBlocksLeft;
    public double laborPerBlock;
    public long lastLaborTick;

    // --- МФО ---
    public double mfoDebt;
    public double mfoPrincipal;
    public int mfoDays;
    public int mfoOverdueDays;
    public boolean mfoLethal;
    public boolean mfoBurned;
    public long nextBratkiTick;
    public long lastSms;
    public int mfoLoansTaken;

    // --- неволя ---
    public int captivity = FREE;
    public long jailUntil;
    public int pitBlocksLeft;
    /** Временная камера (СИЗО в небе / подвал под МФО) и снимок блоков, которые она заменила. */
    public boolean cellActive;
    public BlockPos cellCenter = BlockPos.ZERO;
    public ListTag cellSnapshot = new ListTag();

    // --- рейтинг (ручная поправка админом / финмониторинг) ---
    public int ratingBonus;

    // --- ордер на дом, который ещё не построен (не зависит от того, погашена ли ипотека) ---
    public int pendingHouse = -1;

    // --- подвал Шайкина: сколько ещё рублей добычи сдать в приёмник ---
    public double pitDebtLeft;

    // --- цех: куплено станков, где стоит ООО ---
    public int printers;
    /** Не сохраняется: после обновления мира перенести станки старого подвала в список. */
    public transient boolean needPrinters;
    /** v4: подключённые станки (стоят в доме / на участке / в старом подвале). */
    public final java.util.List<BlockPos> printerList = new java.util.ArrayList<>();
    /** «Сундук цеха» уже выдан (даётся один раз). */
    public boolean chestGiven;
    public BlockPos launderPos = BlockPos.ZERO;

    // --- ограбление банка ---
    public boolean robber;
    public long robberyUntil;

    // --- казино ---
    public long casinoBanUntil;
    public long casinoDay = -1;
    public double casinoProfit;
    public boolean casinoKick;

    // --- взятки ---
    public double policeBribe;

    // --- подпольный бизнес ---
    public boolean hasBusiness;
    public BlockPos businessChest = BlockPos.ZERO;
    public BlockPos businessMin = BlockPos.ZERO;
    public BlockPos businessMax = BlockPos.ZERO;
    public boolean hasLaundering;
    /** Криптокошелёк (BTC). */
    public double crypto;
    /** v1.8: кошельки остальных монет (индекс = Crypto.Coin.ordinal(), [0] не используется — BTC лежит в crypto). */
    public final double[] coins = new double[8];
    /** v1.8: самый большой остаток на счёте — рейтинг не падает, когда тратишь накопленное. */
    public double peakBalance;
    /** v1.8: сколько дней у игрока работал прибыльный бизнес — рейтинг растёт. */
    public int bizDays;

    // --- самострой ---
    public boolean landCase;
    public long landChunk;
    public long landDeadline;
    public long landNextWarn;

    public Account(UUID id) {
        this.id = id;
    }

    public boolean hasLoan() { return loanPrincipal > 0.009 && loanDays > 0; }
    public boolean hasMortgage() { return mortHouse >= 0 && mortDays > 0; }
    public boolean hasMfo() { return mfoDebt > 0.009; }

    public double loanPayment() { return hasLoan() ? Finance.annuity(loanPrincipal, loanRate, loanDays) : 0; }
    public double mortPayment() { return hasMortgage() ? Finance.annuity(mortPrincipal, mortRate, mortDays) : 0; }
    public double dailyDue() { return loanPayment() + mortPayment(); }

    public void clearLoan() { loanPrincipal = 0; loanDays = 0; loanRate = 0; }
    public void clearMortgage() { mortPrincipal = 0; mortDays = 0; mortRate = 0; mortHouse = -1; }
    public void clearMfo() { mfoDebt = 0; mfoPrincipal = 0; mfoDays = 0; mfoOverdueDays = 0; mfoLethal = false; mfoBurned = false; nextBratkiTick = 0; }
    public void clearArrears() { arrears = 0; overdueDays = 0; laborBlocksLeft = 0; laborPerBlock = 0; }

    public CompoundTag write() {
        CompoundTag t = new CompoundTag();
        Nbt.putUUID(t, "id", id);
        t.putString("name", name);
        t.putDouble("balance", balance);
        t.putDouble("lifetimeDeposited", lifetimeDeposited);
        t.putLong("createdDay", createdDay);
        t.putInt("onTime", onTimePayments);
        t.putInt("lateDays", lateDaysTotal);
        t.putInt("defaults", defaults);
        t.putDouble("loanPrincipal", loanPrincipal);
        t.putDouble("loanRate", loanRate);
        t.putInt("loanDays", loanDays);
        t.putDouble("mortPrincipal", mortPrincipal);
        t.putDouble("mortRate", mortRate);
        t.putInt("mortDays", mortDays);
        t.putInt("mortHouse", mortHouse);
        t.putLong("houseOrderId", houseOrderId);
        t.putBoolean("houseBuilt", houseBuilt);
        t.putBoolean("hasHouse", hasHouse);
        t.putInt("houseType", houseType);
        t.putLong("houseMin", houseMin.asLong());
        t.putLong("houseMax", houseMax.asLong());
        t.putDouble("arrears", arrears);
        t.putInt("overdueDays", overdueDays);
        t.putBoolean("wanted", wanted);
        t.putInt("policeWave", policeWave);
        t.putLong("nextPoliceTick", nextPoliceTick);
        t.putInt("laborBlocksLeft", laborBlocksLeft);
        t.putDouble("laborPerBlock", laborPerBlock);
        t.putLong("lastLaborTick", lastLaborTick);
        t.putDouble("mfoDebt", mfoDebt);
        t.putDouble("mfoPrincipal", mfoPrincipal);
        t.putInt("mfoDays", mfoDays);
        t.putInt("mfoOverdueDays", mfoOverdueDays);
        t.putBoolean("mfoLethal", mfoLethal);
        t.putBoolean("mfoBurned", mfoBurned);
        t.putLong("nextBratkiTick", nextBratkiTick);
        t.putLong("lastSms", lastSms);
        t.putInt("mfoLoansTaken", mfoLoansTaken);
        t.putInt("captivity", captivity);
        t.putLong("jailUntil", jailUntil);
        t.putInt("pitBlocksLeft", pitBlocksLeft);
        t.putBoolean("cellActive", cellActive);
        t.putLong("cellCenter", cellCenter.asLong());
        t.put("cellSnapshot", cellSnapshot);
        t.putInt("ratingBonus", ratingBonus);
        t.putInt("pendingHouse", pendingHouse);
        t.putDouble("pitDebtLeft", pitDebtLeft);
        t.putInt("printers", printers);
        long[] pl = new long[printerList.size()];
        for (int i = 0; i < pl.length; i++) pl[i] = printerList.get(i).asLong();
        t.putLongArray("printerList", pl);
        t.putBoolean("chestGiven", chestGiven);
        t.putInt("bizVersion", 4);
        t.putLong("launderPos", launderPos.asLong());
        t.putBoolean("robber", robber);
        t.putLong("robberyUntil", robberyUntil);
        t.putLong("casinoBanUntil", casinoBanUntil);
        t.putLong("casinoDay", casinoDay);
        t.putDouble("casinoProfit", casinoProfit);
        t.putBoolean("casinoKick", casinoKick);
        t.putDouble("policeBribe", policeBribe);
        t.putBoolean("hasBusiness", hasBusiness);
        t.putLong("businessChest", businessChest.asLong());
        t.putLong("businessMin", businessMin.asLong());
        t.putLong("businessMax", businessMax.asLong());
        t.putBoolean("hasLaundering", hasLaundering);
        t.putDouble("crypto", crypto);
        for (int i = 1; i < coins.length; i++) if (coins[i] != 0) t.putDouble("coin" + i, coins[i]);
        t.putDouble("peakBalance", peakBalance);
        t.putInt("bizDays", bizDays);
        t.putBoolean("landCase", landCase);
        t.putLong("landChunk", landChunk);
        t.putLong("landDeadline", landDeadline);
        return t;
    }

    public static Account read(CompoundTag t) {
        Account a = new Account(Nbt.getUUID(t, "id"));
        a.name = t.getStringOr("name", "");
        a.balance = t.getDoubleOr("balance", 0.0);
        a.lifetimeDeposited = t.getDoubleOr("lifetimeDeposited", 0.0);
        a.createdDay = t.getLongOr("createdDay", 0L);
        a.onTimePayments = t.getIntOr("onTime", 0);
        a.lateDaysTotal = t.getIntOr("lateDays", 0);
        a.defaults = t.getIntOr("defaults", 0);
        a.loanPrincipal = t.getDoubleOr("loanPrincipal", 0.0);
        a.loanRate = t.getDoubleOr("loanRate", 0.0);
        a.loanDays = t.getIntOr("loanDays", 0);
        a.mortPrincipal = t.getDoubleOr("mortPrincipal", 0.0);
        a.mortRate = t.getDoubleOr("mortRate", 0.0);
        a.mortDays = t.getIntOr("mortDays", 0);
        a.mortHouse = t.getIntOr("mortHouse", 0);
        a.houseOrderId = t.getLongOr("houseOrderId", 0L);
        a.houseBuilt = t.getBooleanOr("houseBuilt", false);
        a.hasHouse = t.getBooleanOr("hasHouse", false);
        a.houseType = t.getIntOr("houseType", 0);
        a.houseMin = BlockPos.of(t.getLongOr("houseMin", 0L));
        a.houseMax = BlockPos.of(t.getLongOr("houseMax", 0L));
        a.arrears = t.getDoubleOr("arrears", 0.0);
        a.overdueDays = t.getIntOr("overdueDays", 0);
        a.wanted = t.getBooleanOr("wanted", false);
        a.policeWave = t.getIntOr("policeWave", 0);
        a.nextPoliceTick = t.getLongOr("nextPoliceTick", 0L);
        a.laborBlocksLeft = t.getIntOr("laborBlocksLeft", 0);
        a.laborPerBlock = t.getDoubleOr("laborPerBlock", 0.0);
        a.lastLaborTick = t.getLongOr("lastLaborTick", 0L);
        a.mfoDebt = t.getDoubleOr("mfoDebt", 0.0);
        a.mfoPrincipal = t.getDoubleOr("mfoPrincipal", 0.0);
        a.mfoDays = t.getIntOr("mfoDays", 0);
        a.mfoOverdueDays = t.getIntOr("mfoOverdueDays", 0);
        a.mfoLethal = t.getBooleanOr("mfoLethal", false);
        a.mfoBurned = t.getBooleanOr("mfoBurned", false);
        a.nextBratkiTick = t.getLongOr("nextBratkiTick", 0L);
        a.lastSms = t.getLongOr("lastSms", 0L);
        a.mfoLoansTaken = t.getIntOr("mfoLoansTaken", 0);
        a.captivity = t.getIntOr("captivity", 0);
        a.jailUntil = t.getLongOr("jailUntil", 0L);
        a.pitBlocksLeft = t.getIntOr("pitBlocksLeft", 0);
        a.cellActive = t.getBooleanOr("cellActive", false);
        a.cellCenter = BlockPos.of(t.getLongOr("cellCenter", 0L));
        a.cellSnapshot = t.getListOrEmpty("cellSnapshot");
        a.ratingBonus = t.getIntOr("ratingBonus", 0);
        if (t.contains("pendingHouse")) {
            a.pendingHouse = t.getIntOr("pendingHouse", 0);
        } else if (a.mortHouse >= 0 && !a.houseBuilt) {
            a.pendingHouse = a.mortHouse; // версия 2
        }
        a.pitDebtLeft = t.getDoubleOr("pitDebtLeft", 0.0);
        if (a.pitDebtLeft <= 0 && a.pitBlocksLeft > 0) a.pitDebtLeft = a.pitBlocksLeft * 8.0; // версия 2
        a.printers = t.getIntOr("printers", 0);
        a.launderPos = BlockPos.of(t.getLongOr("launderPos", 0L));
        a.robber = t.getBooleanOr("robber", false);
        a.robberyUntil = t.getLongOr("robberyUntil", 0L);
        a.casinoBanUntil = t.getLongOr("casinoBanUntil", 0L);
        a.casinoDay = t.contains("casinoDay") ? t.getLongOr("casinoDay", 0L) : -1;
        a.casinoProfit = t.getDoubleOr("casinoProfit", 0.0);
        a.casinoKick = false; // наказание за везение отменено (v4)
        a.casinoBanUntil = 0;
        a.policeBribe = t.getDoubleOr("policeBribe", 0.0);
        a.hasBusiness = t.getBooleanOr("hasBusiness", false);
        if (a.hasBusiness && !t.contains("printers")) a.printers = 2; // версия 2
        for (long l : t.getLongArray("printerList").orElse(new long[0])) a.printerList.add(BlockPos.of(l));
        a.chestGiven = t.getBooleanOr("chestGiven", false);
        if (a.hasBusiness && t.getIntOr("bizVersion", 0) < 4) { a.chestGiven = true; a.needPrinters = true; } // старый подвал: сундук уже есть
        a.businessChest = BlockPos.of(t.getLongOr("businessChest", 0L));
        a.businessMin = BlockPos.of(t.getLongOr("businessMin", 0L));
        a.businessMax = BlockPos.of(t.getLongOr("businessMax", 0L));
        a.hasLaundering = t.getBooleanOr("hasLaundering", false);
        a.crypto = t.getDoubleOr("crypto", 0.0);
        for (int i = 1; i < a.coins.length; i++) a.coins[i] = t.getDoubleOr("coin" + i, 0.0);
        a.peakBalance = t.contains("peakBalance") ? t.getDoubleOr("peakBalance", 0.0) : Math.max(0, a.balance);
        a.bizDays = t.getIntOr("bizDays", 0);
        a.landCase = t.getBooleanOr("landCase", false);
        a.landChunk = t.getLongOr("landChunk", 0L);
        a.landDeadline = t.getLongOr("landDeadline", 0L);
        return a;
    }
}
