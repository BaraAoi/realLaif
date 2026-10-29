package com.baraaoi.reallife.economy;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;

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
    public void clearMortgage() { mortPrincipal = 0; mortDays = 0; mortRate = 0; mortHouse = -1; houseOrderId = 0; houseBuilt = false; }
    public void clearMfo() { mfoDebt = 0; mfoPrincipal = 0; mfoDays = 0; mfoOverdueDays = 0; mfoLethal = false; mfoBurned = false; nextBratkiTick = 0; }
    public void clearArrears() { arrears = 0; overdueDays = 0; laborBlocksLeft = 0; laborPerBlock = 0; }

    public CompoundNBT write() {
        CompoundNBT t = new CompoundNBT();
        t.putUniqueId("id", id);
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
        t.putLong("houseMin", houseMin.toLong());
        t.putLong("houseMax", houseMax.toLong());
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
        return t;
    }

    public static Account read(CompoundNBT t) {
        Account a = new Account(t.getUniqueId("id"));
        a.name = t.getString("name");
        a.balance = t.getDouble("balance");
        a.lifetimeDeposited = t.getDouble("lifetimeDeposited");
        a.createdDay = t.getLong("createdDay");
        a.onTimePayments = t.getInt("onTime");
        a.lateDaysTotal = t.getInt("lateDays");
        a.defaults = t.getInt("defaults");
        a.loanPrincipal = t.getDouble("loanPrincipal");
        a.loanRate = t.getDouble("loanRate");
        a.loanDays = t.getInt("loanDays");
        a.mortPrincipal = t.getDouble("mortPrincipal");
        a.mortRate = t.getDouble("mortRate");
        a.mortDays = t.getInt("mortDays");
        a.mortHouse = t.getInt("mortHouse");
        a.houseOrderId = t.getLong("houseOrderId");
        a.houseBuilt = t.getBoolean("houseBuilt");
        a.hasHouse = t.getBoolean("hasHouse");
        a.houseType = t.getInt("houseType");
        a.houseMin = BlockPos.fromLong(t.getLong("houseMin"));
        a.houseMax = BlockPos.fromLong(t.getLong("houseMax"));
        a.arrears = t.getDouble("arrears");
        a.overdueDays = t.getInt("overdueDays");
        a.wanted = t.getBoolean("wanted");
        a.policeWave = t.getInt("policeWave");
        a.nextPoliceTick = t.getLong("nextPoliceTick");
        a.laborBlocksLeft = t.getInt("laborBlocksLeft");
        a.laborPerBlock = t.getDouble("laborPerBlock");
        a.lastLaborTick = t.getLong("lastLaborTick");
        a.mfoDebt = t.getDouble("mfoDebt");
        a.mfoPrincipal = t.getDouble("mfoPrincipal");
        a.mfoDays = t.getInt("mfoDays");
        a.mfoOverdueDays = t.getInt("mfoOverdueDays");
        a.mfoLethal = t.getBoolean("mfoLethal");
        a.mfoBurned = t.getBoolean("mfoBurned");
        a.nextBratkiTick = t.getLong("nextBratkiTick");
        a.lastSms = t.getLong("lastSms");
        a.mfoLoansTaken = t.getInt("mfoLoansTaken");
        a.captivity = t.getInt("captivity");
        a.jailUntil = t.getLong("jailUntil");
        a.pitBlocksLeft = t.getInt("pitBlocksLeft");
        return a;
    }
}
