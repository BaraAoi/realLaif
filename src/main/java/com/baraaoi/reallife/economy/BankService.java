package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.container.FinanceContainer;
import com.baraaoi.reallife.item.BanknoteItem;
import com.baraaoi.reallife.item.HouseOrderItem;
import com.baraaoi.reallife.network.Network;
import com.baraaoi.reallife.network.SyncPacket;
import com.baraaoi.reallife.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Серверная логика банка и МФО.
 */
public class BankService {
    public static final String BANK = "§6[Реал Банк] §f";
    public static final String MFO = "§2[Шайкин и Ко] §f";
    public static final int[] LOAN_TERMS = {5, 10, 20};
    public static final int[] NOTES = {5000, 1000, 100, 10};
    private static final Random RANDOM = new Random();

    // ---------------------------------------------------------------- utils

    public static void msg(PlayerEntity p, String text) {
        p.sendStatusMessage(new StringTextComponent(text), false);
    }

    public static void actionBar(PlayerEntity p, String text) {
        p.sendStatusMessage(new StringTextComponent(text), true);
    }

    public static long today(MinecraftServer server) {
        return EconomyData.overworld(server).getDayTime() / 24000L;
    }

    public static Item noteItem(int value) {
        switch (value) {
            case 5000: return ModItems.BANKNOTE_5000.get();
            case 1000: return ModItems.BANKNOTE_1000.get();
            case 100: return ModItems.BANKNOTE_100.get();
            default: return ModItems.BANKNOTE_10.get();
        }
    }

    /** Выдать наличные купюрами. Возвращает фактически выданную сумму (кратно 10). */
    public static double giveCash(PlayerEntity p, double amount) {
        long left = (long) Math.floor(amount / 10.0) * 10L;
        long given = left;
        for (int note : NOTES) {
            long n = left / note;
            left -= n * note;
            while (n > 0) {
                int c = (int) Math.min(64, n);
                n -= c;
                give(p, new ItemStack(noteItem(note), c));
            }
        }
        return given;
    }

    public static void give(PlayerEntity p, ItemStack stack) {
        if (!p.inventory.addItemStackToInventory(stack) && !stack.isEmpty()) {
            p.dropItem(stack, false);
        }
    }

    /**
     * Забрать у игрока ценности на сумму не менее needed (сначала купюры, потом прочее).
     * factor — по какой цене принимают предметы (купюры всегда по номиналу).
     * Возвращает зачтённую сумму.
     */
    public static double takeValuables(PlayerEntity p, double needed, double factor) {
        double got = 0;
        for (int pass = 0; pass < 2 && got < needed; pass++) {
            for (int i = 0; i < p.inventory.mainInventory.size() && got < needed; i++) {
                ItemStack st = p.inventory.mainInventory.get(i);
                if (st.isEmpty()) continue;
                boolean note = ItemValues.isCash(st);
                if ((pass == 0) != note) continue;
                double unit = ItemValues.valueOf(st) * (note ? 1.0 : factor);
                if (unit <= 0) continue;
                while (!st.isEmpty() && got < needed) {
                    st.shrink(1);
                    got += unit;
                }
            }
        }
        return got;
    }

    /** Сумма купюр в инвентаре. */
    public static long cashInInventory(PlayerEntity p) {
        long v = 0;
        for (ItemStack st : p.inventory.mainInventory) {
            if (st.getItem() instanceof BanknoteItem) v += (long) ((BanknoteItem) st.getItem()).getValue() * st.getCount();
        }
        return v;
    }

    /** Списать сумму: сначала со счёта, остаток — купюрами из инвентаря (со сдачей). */
    public static boolean charge(PlayerEntity p, Account a, double amount) {
        double fromBalance = Math.max(0, Math.min(a.balance, amount));
        double fromCash = amount - fromBalance;
        long cash = cashInInventory(p);
        if (fromCash > cash + 1e-6) return false;
        a.balance -= fromBalance;
        if (fromCash > 1e-6) {
            for (int i = 0; i < p.inventory.mainInventory.size(); i++) {
                if (p.inventory.mainInventory.get(i).getItem() instanceof BanknoteItem) p.inventory.mainInventory.set(i, ItemStack.EMPTY);
            }
            double change = cash - fromCash;
            if (change >= 10) giveCash(p, change);
        }
        return true;
    }

    /** Сумма всех ценностей в инвентаре. */
    public static double inventoryValue(PlayerEntity p) {
        double v = 0;
        for (ItemStack st : p.inventory.mainInventory) v += ItemValues.stackValue(st);
        return v;
    }

    // ---------------------------------------------------------------- sync

    public static void sync(ServerPlayerEntity p) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(p);
        long today = today(server);
        int score = CreditRating.score(a, today);
        CompoundNBT t = new CompoundNBT();
        t.putDouble("balance", a.balance);
        t.putInt("score", score);
        t.putString("category", CreditRating.category(score));
        t.putDouble("income", CreditRating.avgDailyIncome(a, today));
        t.putDouble("burdenLimit", burdenLimit(a, today, a.balance, 0));
        t.putDouble("maxLoan", CreditRating.maxLoan(a, today));
        t.putDouble("loanRate", CreditRating.loanRate(score));
        t.putDouble("depositRate", Config.DEPOSIT_DAILY_RATE.get());
        t.putInt("onTime", a.onTimePayments);
        t.putInt("lateDays", a.lateDaysTotal);
        t.putInt("defaults", a.defaults);
        t.putDouble("loanPrincipal", a.hasLoan() ? a.loanPrincipal : 0);
        t.putDouble("loanPayment", a.loanPayment());
        t.putInt("loanDays", a.hasLoan() ? a.loanDays : 0);
        t.putDouble("loanActiveRate", a.loanRate);
        t.putDouble("mortPrincipal", a.hasMortgage() ? a.mortPrincipal : 0);
        t.putDouble("mortPayment", a.mortPayment());
        t.putInt("mortDays", a.hasMortgage() ? a.mortDays : 0);
        t.putInt("mortHouse", a.hasMortgage() ? a.mortHouse : -1);
        t.putBoolean("houseBuilt", a.houseBuilt);
        t.putDouble("arrears", a.arrears);
        t.putInt("overdue", a.overdueDays);
        t.putBoolean("wanted", a.wanted);
        t.putInt("laborLeft", a.laborBlocksLeft);
        t.putDouble("downPct", Config.MORTGAGE_DOWN_PAYMENT.get());
        ListNBT houses = new ListNBT();
        for (HouseType h : HouseType.values()) {
            CompoundNBT ht = new CompoundNBT();
            double down = h.price * Config.MORTGAGE_DOWN_PAYMENT.get();
            double rate = CreditRating.mortgageRate(score);
            double pay = Finance.annuity(h.price - down, rate, h.termDays);
            ht.putDouble("down", down);
            ht.putDouble("rate", rate);
            ht.putDouble("pay", pay);
            ht.putString("reason", mortgageRefusal(a, h, today));
            houses.add(ht);
        }
        t.put("houses", houses);
        // МФО
        t.putDouble("mfoDebt", a.mfoDebt);
        t.putInt("mfoDays", a.mfoDays);
        t.putInt("mfoOverdue", a.mfoOverdueDays);
        t.putInt("mfoMax", Config.MFO_MAX_LOAN.get());
        t.putDouble("mfoRate", Config.MFO_DAILY_RATE.get());
        t.putDouble("mfoOverdueRate", Config.MFO_OVERDUE_RATE.get());
        t.putDouble("mfoCommission", Config.MFO_COMMISSION.get());
        t.putInt("mfoTerm", Config.MFO_TERM_DAYS.get());
        t.putBoolean("hasHouse", a.hasHouse);
        t.putBoolean("hasBusiness", a.hasBusiness);
        t.putBoolean("hasLaundering", a.hasLaundering);
        t.putInt("businessPrice", Config.BUSINESS_PRICE.get());
        t.putInt("businessDaily", Config.BUSINESS_DAILY.get());
        t.putInt("launderPrice", Config.LAUNDER_PRICE.get());
        t.putDouble("launderCommission", Config.LAUNDER_COMMISSION.get());
        t.putDouble("dirtyChance", Config.DIRTY_ACCEPT_CHANCE.get());
        t.putInt("ownedChunks", data.ownedCount(a.id));
        Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new SyncPacket(t));
    }

    // ---------------------------------------------------------------- проверки

    /** Сколько в день клиент может платить по всем кредитам (с учётом накоплений). */
    public static double burdenLimit(Account a, long today, double cashAfter, int termDays) {
        double lim = CreditRating.maxDailyBurden(a, today);
        if (termDays > 0) lim += Math.max(0, cashAfter) / termDays;
        return lim;
    }

    /** Пустая строка = одобрено, иначе причина отказа. */
    public static String mortgageRefusal(Account a, HouseType h, long today) {
        int score = CreditRating.score(a, today);
        if (a.captivity != Account.FREE) return "Вы под арестом";
        if (a.hasMortgage()) return "Уже есть ипотека";
        if (a.arrears > 0.01) return "Есть просрочка";
        if (score < h.minScore) return "Нужен рейтинг " + h.minScore;
        double down = h.price * Config.MORTGAGE_DOWN_PAYMENT.get();
        if (a.balance < down) return "Нет взноса " + Finance.money(down);
        double pay = Finance.annuity(h.price - down, CreditRating.mortgageRate(score), h.termDays);
        double limit = burdenLimit(a, today, a.balance - down, h.termDays);
        if (pay + a.loanPayment() > limit) return "Мал доход (ПДН)";
        return "";
    }

    private static boolean checkFree(ServerPlayerEntity p, Account a) {
        if (a.captivity != Account.FREE) {
            msg(p, "§cВы сейчас не можете пользоваться услугами.");
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- действия банка

    public static void handle(ServerPlayerEntity p, int action, int arg1, int arg2) {
        if (!(p.openContainer instanceof FinanceContainer)) return;
        FinanceContainer c = (FinanceContainer) p.openContainer;
        if (!c.canInteractWith(p)) return;
        MinecraftServer server = p.getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(p);
        long today = today(server);
        if (c.isMfo()) {
            switch (action) {
                case Actions.MFO_TAKE: mfoTake(p, a, arg1); break;
                case Actions.MFO_PAY_SLOT: mfoPaySlot(p, a, c); break;
                case Actions.MFO_PAY_BANK: mfoPayBank(p, a, arg1); break;
                case Actions.MFO_BUY_BUSINESS: buyBusiness(p, a); break;
                case Actions.MFO_BUY_LAUNDER: buyLaunder(p, a); break;
                case Actions.MFO_LAUNDER: launder(p, a, c); break;
                default: break;
            }
        } else {
            switch (action) {
                case Actions.DEPOSIT: deposit(p, a, c); break;
                case Actions.WITHDRAW: withdraw(p, a, arg1); break;
                case Actions.TAKE_LOAN: takeLoan(p, a, today, arg1, arg2); break;
                case Actions.REPAY_LOAN: repayLoan(p, a, arg1); break;
                case Actions.MORTGAGE: takeMortgage(p, a, today, arg1); break;
                case Actions.REPAY_MORTGAGE: repayMortgage(p, a, arg1); break;
                case Actions.REISSUE_ORDER: reissueOrder(p, a); break;
                default: break;
            }
        }
        data.markDirty();
        sync(p);
    }

    static void deposit(ServerPlayerEntity p, Account a, FinanceContainer c) {
        ItemStack st = c.getInput().getStackInSlot(0);
        if (st.isEmpty()) { msg(p, BANK + "Положите ресурсы или купюры в слот."); return; }
        double v = ItemValues.stackValue(st);
        if (v <= 0) { msg(p, BANK + "§cБанк не принимает этот предмет."); return; }
        if (ItemValues.isDirty(st)) {
            c.getInput().setInventorySlotContents(0, ItemStack.EMPTY);
            if (RANDOM.nextDouble() < Config.DIRTY_ACCEPT_CHANCE.get()) {
                a.balance += v;
                a.lifetimeDeposited += v;
                msg(p, BANK + "Зачислено §a+" + Finance.money(v) + "§f («выручка от предпринимательской деятельности»). Баланс: §e" + Finance.money(a.balance));
                autoPayArrears(p, a);
            } else {
                a.ratingBonus -= 15;
                msg(p, BANK + "§4Финмониторинг: происхождение средств не подтверждено. §c" + Finance.money(v)
                        + " изъято по 115-ФЗ§4, сведения переданы куда следует. Рейтинг −15.");
            }
            return;
        }
        boolean notes = st.getItem() instanceof BanknoteItem;
        c.getInput().setInventorySlotContents(0, ItemStack.EMPTY);
        a.balance += v;
        if (!notes) a.lifetimeDeposited += v;
        msg(p, BANK + "Зачислено §a+" + Finance.money(v) + "§f. Баланс: §e" + Finance.money(a.balance));
        autoPayArrears(p, a);
    }

    public static void autoPayArrears(PlayerEntity p, Account a) {
        if (a.arrears > 0.01 && a.balance > 0) {
            double pay = Math.min(a.balance, a.arrears);
            a.balance -= pay;
            a.arrears -= pay;
            if (p != null) msg(p, BANK + "Списано в погашение просрочки: §c" + Finance.money(pay));
            if (a.arrears <= 0.01) {
                a.clearArrears();
                if (p != null) msg(p, BANK + "§aПросрочка погашена. Спасибо!");
            }
        }
    }

    static void withdraw(ServerPlayerEntity p, Account a, int note) {
        boolean ok = false;
        for (int n : NOTES) if (n == note) ok = true;
        if (!ok) return;
        if (a.balance + 1e-6 < note) { msg(p, BANK + "§cНедостаточно средств на счёте."); return; }
        a.balance -= note;
        give(p, new ItemStack(noteItem(note)));
    }

    static void takeLoan(ServerPlayerEntity p, Account a, long today, int amount, int termIdx) {
        if (!checkFree(p, a)) return;
        if (a.hasLoan()) { msg(p, BANK + "§cУ вас уже есть кредит. Сначала погасите его."); return; }
        if (termIdx < 0 || termIdx >= LOAN_TERMS.length) return;
        int term = LOAN_TERMS[termIdx];
        double max = CreditRating.maxLoan(a, today);
        int score = CreditRating.score(a, today);
        if (max < 50) {
            msg(p, BANK + "§cОтказ. Кредитный рейтинг " + score + " — недостаточно. Копите, вносите ресурсы, платите вовремя.");
            return;
        }
        if (amount < 50 || amount > max) { msg(p, BANK + "§cСумма должна быть от 50₽ до " + Finance.money(max)); return; }
        double rate = CreditRating.loanRate(score);
        double pay = Finance.annuity(amount, rate, term);
        double limit = burdenLimit(a, today, a.balance, term);
        if (pay + a.mortPayment() > limit) {
            msg(p, BANK + "§cОтказ: слишком высокая долговая нагрузка. Платёж " + Finance.money(pay)
                    + "/день, а вы можете платить не более " + Finance.money(limit - a.mortPayment()) + "/день.");
            return;
        }
        a.loanPrincipal = amount;
        a.loanRate = rate;
        a.loanDays = term;
        a.balance += amount;
        msg(p, BANK + "§aКредит одобрен! §f" + Finance.money(amount) + " зачислено на счёт. Ставка "
                + Finance.percent(rate) + "/день, платёж §e" + Finance.money(pay) + "§f/день, " + term + " дн.");
        msg(p, BANK + "§7Платёж списывается со счёта автоматически каждое утро. Держите деньги на счёте!");
    }

    static void repayLoan(ServerPlayerEntity p, Account a, int amount) {
        if (!a.hasLoan()) { msg(p, BANK + "У вас нет кредита."); return; }
        double pay = amount <= 0 ? a.loanPrincipal : Math.min(amount, a.loanPrincipal);
        if (a.balance + 1e-6 < pay) { msg(p, BANK + "§cНедостаточно средств на счёте (" + Finance.money(pay) + ")."); return; }
        a.balance -= pay;
        a.loanPrincipal -= pay;
        if (a.loanPrincipal < 0.01) {
            a.clearLoan();
            a.onTimePayments += 3;
            msg(p, BANK + "§aКредит полностью погашен досрочно! Кредитная история улучшена.");
        } else {
            msg(p, BANK + "Досрочно погашено " + Finance.money(pay) + ". Остаток долга: " + Finance.money(a.loanPrincipal)
                    + ", новый платёж " + Finance.money(a.loanPayment()) + "/день.");
        }
    }

    static void takeMortgage(ServerPlayerEntity p, Account a, long today, int houseIdx) {
        if (!checkFree(p, a)) return;
        HouseType h = HouseType.byId(houseIdx);
        if (h == null) return;
        String refusal = mortgageRefusal(a, h, today);
        if (!refusal.isEmpty()) { msg(p, BANK + "§cОтказ по ипотеке: " + refusal); return; }
        int score = CreditRating.score(a, today);
        double down = h.price * Config.MORTGAGE_DOWN_PAYMENT.get();
        a.balance -= down;
        a.mortPrincipal = h.price - down;
        a.mortRate = CreditRating.mortgageRate(score);
        a.mortDays = h.termDays;
        a.mortHouse = h.ordinal();
        a.houseOrderId = RANDOM.nextLong();
        a.houseBuilt = false;
        give(p, HouseOrderItem.create(ModItems.HOUSE_ORDER.get(), p.getUniqueID(), h.ordinal(), a.houseOrderId));
        msg(p, BANK + "§aИпотека одобрена: §e" + h.title + "§f! Взнос " + Finance.money(down) + " списан.");
        msg(p, BANK + "Платёж §e" + Finance.money(a.mortPayment()) + "§f/день, " + h.termDays + " дн. "
                + "Используйте §dОрдер на строительство§f — бригада построит дом бесплатно.");
    }

    static void repayMortgage(ServerPlayerEntity p, Account a, int amount) {
        if (!a.hasMortgage()) { msg(p, BANK + "У вас нет ипотеки."); return; }
        double pay = amount <= 0 ? a.mortPrincipal : Math.min(amount, a.mortPrincipal);
        if (a.balance + 1e-6 < pay) { msg(p, BANK + "§cНедостаточно средств на счёте (" + Finance.money(pay) + ")."); return; }
        a.balance -= pay;
        a.mortPrincipal -= pay;
        if (a.mortPrincipal < 0.01) {
            finishMortgage(p, a);
        } else {
            msg(p, BANK + "Досрочно погашено " + Finance.money(pay) + ". Остаток: " + Finance.money(a.mortPrincipal)
                    + ", платёж " + Finance.money(a.mortPayment()) + "/день.");
        }
    }

    static void finishMortgage(PlayerEntity p, Account a) {
        HouseType h = HouseType.byId(a.mortHouse);
        boolean built = a.houseBuilt;
        a.clearMortgage();
        a.onTimePayments += 5;
        if (p != null) {
            msg(p, BANK + "§a§lИпотека погашена! §r§f" + (h != null ? h.title : "Дом") + " теперь полностью ваш.");
            if (!built) msg(p, BANK + "§7(Дом так и не был построен — ордер аннулирован.)");
        }
    }

    static void reissueOrder(ServerPlayerEntity p, Account a) {
        if (!a.hasMortgage() || a.houseBuilt) { msg(p, BANK + "Нет ипотеки без построенного дома."); return; }
        a.houseOrderId = RANDOM.nextLong();
        give(p, HouseOrderItem.create(ModItems.HOUSE_ORDER.get(), p.getUniqueID(), a.mortHouse, a.houseOrderId));
        msg(p, BANK + "Выдан новый ордер. Старые ордера аннулированы.");
    }

    // ---------------------------------------------------------------- действия МФО

    static void mfoTake(ServerPlayerEntity p, Account a, int amount) {
        if (!checkFree(p, a)) return;
        if (a.hasMfo()) { msg(p, MFO + "§cСначала верни старый должок, дорогой."); return; }
        int max = Config.MFO_MAX_LOAN.get();
        if (amount < 100 || amount > max) { msg(p, MFO + "Даём от 100₽ до " + Finance.money(max) + "."); return; }
        double commission = Config.MFO_COMMISSION.get();
        double cash = Math.floor(amount * (1 - commission) / 10.0) * 10.0;
        a.mfoDebt = amount;
        a.mfoPrincipal = amount;
        a.mfoDays = Config.MFO_TERM_DAYS.get();
        a.mfoOverdueDays = 0;
        a.mfoLethal = false;
        a.mfoBurned = false;
        a.mfoLoansTaken++;
        giveCash(p, cash);
        double rate = Config.MFO_DAILY_RATE.get();
        msg(p, MFO + "Держи §a" + Finance.money(cash) + "§f наличкой! Должен §c" + Finance.money(amount)
                + "§f (страховка и комиссия " + Finance.percent(commission) + " уже удержаны).");
        msg(p, MFO + "§7Мелким шрифтом: " + Finance.percent(rate) + " в день сложными процентами + СМС-информирование 5₽/день, "
                + "срок " + a.mfoDays + " дн., просрочка — " + Finance.percent(Config.MFO_OVERDUE_RATE.get())
                + "/день + штраф. ПСК: " + Math.round((Math.pow(1 + rate, 365) - 1) * 100) + "% годовых.");
        msg(p, MFO + "§8Не вернёшь — к тебе приедут. Мы знаем, где ты живёшь.");
    }

    static void mfoPaySlot(ServerPlayerEntity p, Account a, FinanceContainer c) {
        if (!a.hasMfo()) { msg(p, MFO + "Ты нам ничего не должен. Пока."); return; }
        ItemStack st = c.getInput().getStackInSlot(0);
        if (st.isEmpty()) { msg(p, MFO + "Клади деньги или ценности в слот."); return; }
        double unit = ItemValues.valueOf(st);
        if (unit <= 0) { msg(p, MFO + "§cЭто барахло нам не нужно."); return; }
        boolean note = ItemValues.isCash(st);
        double factor = note ? 1.0 : 0.6;
        double per = unit * factor;
        int need = (int) Math.min(st.getCount(), Math.ceil(a.mfoDebt / per - 1e-9));
        need = Math.max(1, need);
        double paid = need * per;
        st.shrink(need);
        c.getInput().setInventorySlotContents(0, st);
        payMfo(p, a, paid, note ? "" : " (оценщик принял по 60% стоимости)");
    }

    static void mfoPayBank(ServerPlayerEntity p, Account a, int amount) {
        if (!a.hasMfo()) { msg(p, MFO + "Ты нам ничего не должен. Пока."); return; }
        double want = amount <= 0 ? a.mfoDebt * 1.05 : amount;
        double take = Math.min(want, a.balance);
        if (take <= 0) { msg(p, MFO + "§cНа банковском счёте пусто."); return; }
        a.balance -= take;
        payMfo(p, a, take / 1.05, " (перевод из банка, комиссия 5%)");
    }

    // ---------------------------------------------------------------- подпольный бизнес

    static void buyBusiness(ServerPlayerEntity p, Account a) {
        if (!checkFree(p, a)) return;
        if (a.hasBusiness) { msg(p, MFO + "У тебя уже есть цех, коммерсант."); return; }
        if (!a.hasHouse) { msg(p, MFO + "§cЦех ставим только в подвал твоего дома. Сначала возьми ипотеку и построй дом."); return; }
        int price = Config.BUSINESS_PRICE.get();
        if (!charge(p, a, price)) { msg(p, MFO + "§cЦех стоит " + Finance.money(price) + ". Счёт + наличка — не хватает."); return; }
        net.minecraft.world.server.ServerWorld ow = EconomyData.overworld(p.getServer());
        com.baraaoi.reallife.structure.Buildings.Basement bm = com.baraaoi.reallife.structure.Buildings.buildBasement(ow, a.houseMin, a.houseMax);
        if (bm == null) {
            a.balance += price;
            msg(p, MFO + "§cВ доме нет свободного места для люка (нужна клетка пола рядом с центром дома). Деньги вернули на счёт.");
            return;
        }
        a.hasBusiness = true;
        a.businessChest = bm.chest;
        a.businessMin = bm.bp.min();
        a.businessMax = bm.bp.max();
        EconomyData.get(p.getServer()).markDirty();
        msg(p, MFO + "§aЦех открыт! §fЛюк в полу дома (" + bm.hatch.getX() + ", " + bm.hatch.getY() + ", " + bm.hatch.getZ()
                + "). Станки печатают §2" + Finance.money(Config.BUSINESS_DAILY.get()) + "§f грязными в день — забирай из сундука в подвале.");
        msg(p, MFO + "§7Грязные деньги банк примет только с шансом " + Finance.percent(Config.DIRTY_ACCEPT_CHANCE.get())
                + ". Надёжнее — наше ООО «Отмыв». Мы и братки берём по номиналу.");
    }

    static void buyLaunder(ServerPlayerEntity p, Account a) {
        if (!checkFree(p, a)) return;
        if (a.hasLaundering) { msg(p, MFO + "ООО уже на тебя оформлено."); return; }
        int price = Config.LAUNDER_PRICE.get();
        if (!charge(p, a, price)) { msg(p, MFO + "§cООО стоит " + Finance.money(price) + ". Не хватает."); return; }
        a.hasLaundering = true;
        EconomyData.get(p.getServer()).markDirty();
        msg(p, MFO + "§aООО «Отмыв» зарегистрировано! §fДиректор — твоя бабушка. Клади грязные в слот и жми «Отмыть». Комиссия "
                + Finance.percent(Config.LAUNDER_COMMISSION.get()) + ".");
    }

    static void launder(ServerPlayerEntity p, Account a, FinanceContainer c) {
        if (!a.hasLaundering) { msg(p, MFO + "§cСначала купи ООО «Отмыв»."); return; }
        ItemStack st = c.getInput().getStackInSlot(0);
        if (st.isEmpty() || !ItemValues.isDirty(st)) { msg(p, MFO + "Клади в слот грязные деньги."); return; }
        double v = ItemValues.stackValue(st);
        double clean = Math.floor(v * (1 - Config.LAUNDER_COMMISSION.get()) / 10.0) * 10.0;
        c.getInput().setInventorySlotContents(0, ItemStack.EMPTY);
        giveCash(p, clean);
        msg(p, MFO + "Отмыто: §2" + Finance.money(v) + "§f → §a" + Finance.money(clean) + "§f чистыми. Консалтинговые услуги, всё по закону.");
    }

    /** Положить грязные деньги в сундук цеха. Возвращает, сколько пачек влезло. */
    public static int produceDirty(MinecraftServer server, Account a) {
        int packs = Config.BUSINESS_DAILY.get() / com.baraaoi.reallife.item.DirtyMoneyItem.VALUE;
        if (!a.hasBusiness || packs <= 0) return 0;
        net.minecraft.tileentity.TileEntity te = EconomyData.overworld(server).getTileEntity(a.businessChest);
        if (!(te instanceof net.minecraft.inventory.IInventory)) return 0;
        net.minecraft.inventory.IInventory inv = (net.minecraft.inventory.IInventory) te;
        int left = packs;
        for (int i = 0; i < inv.getSizeInventory() && left > 0; i++) {
            ItemStack cur = inv.getStackInSlot(i);
            if (cur.isEmpty()) {
                int n = Math.min(64, left);
                inv.setInventorySlotContents(i, new ItemStack(ModItems.DIRTY_MONEY.get(), n));
                left -= n;
            } else if (cur.getItem() == ModItems.DIRTY_MONEY.get() && cur.getCount() < 64) {
                int n = Math.min(64 - cur.getCount(), left);
                cur.grow(n);
                left -= n;
            }
        }
        te.markDirty();
        return packs - left;
    }

    public static void payMfo(PlayerEntity p, Account a, double amount, String note) {
        double before = a.mfoDebt;
        a.mfoDebt -= amount;
        if (a.mfoDebt <= 0.5) {
            boolean overpaid = a.mfoDebt < -0.5;
            a.clearMfo();
            if (p != null) {
                msg(p, MFO + "§aДолг закрыт" + note + ". " + (overpaid ? "Сдачи нет. " : "") + "Приходи ещё, мы всегда рады!");
            }
        } else if (p != null) {
            msg(p, MFO + "Принято " + Finance.money(Math.min(amount, before)) + note + ". Осталось: §c" + Finance.money(a.mfoDebt));
        }
    }

    // ---------------------------------------------------------------- ежедневное списание

    public static void processDay(MinecraftServer server, EconomyData data) {
        double dep = Config.DEPOSIT_DAILY_RATE.get();
        double pen = Config.LATE_PENALTY.get();
        List<Account> list = new ArrayList<>(data.all());
        for (Account a : list) {
            if (a.captivity != Account.FREE) continue;
            ServerPlayerEntity p = server.getPlayerList().getPlayerByUUID(a.id);

            // проценты по вкладу
            if (a.balance > 0 && a.arrears <= 0.01) {
                double inc = a.balance * dep;
                a.balance += inc;
                if (p != null && inc >= 1) msg(p, BANK + "Начислены проценты по вкладу: §a+" + Finance.money(inc));
            }

            // платежи по кредиту и ипотеке
            double due = 0;
            if (a.hasLoan()) {
                double pay = a.loanPayment();
                double interest = a.loanPrincipal * a.loanRate;
                a.loanPrincipal = Math.max(0, a.loanPrincipal - (pay - interest));
                a.loanDays--;
                due += pay;
                if (a.loanDays <= 0 || a.loanPrincipal < 0.01) {
                    a.clearLoan();
                    if (p != null) msg(p, BANK + "Последний платёж по кредиту выставлен.");
                }
            }
            if (a.hasMortgage()) {
                double pay = a.mortPayment();
                double interest = a.mortPrincipal * a.mortRate;
                a.mortPrincipal = Math.max(0, a.mortPrincipal - (pay - interest));
                a.mortDays--;
                due += pay;
                if (a.mortDays <= 0 || a.mortPrincipal < 0.01) {
                    finishMortgage(p, a);
                }
            }
            if (due > 0 || a.arrears > 0.01) {
                boolean hadArrears = a.arrears > 0.01;
                a.arrears += due;
                double paid = Math.min(Math.max(0, a.balance), a.arrears);
                a.balance -= paid;
                a.arrears -= paid;
                if (a.arrears > 0.01 && a.laborBlocksLeft > 0) {
                    // идёт отработка у Александра: новые платежи добавляют блоков, дни просрочки не растут
                    int extra = (int) Math.min(64, Math.ceil(due / Math.max(1.0, a.laborPerBlock)));
                    a.laborBlocksLeft += extra;
                    a.lateDaysTotal++;
                    if (p != null && extra > 0) msg(p, BANK + "§6Платёж не внесён — Александр добавил вам ещё §c" + extra + "§6 блоков отработки.");
                } else if (a.arrears > 0.01) {
                    a.arrears += a.arrears * pen;
                    a.overdueDays++;
                    a.lateDaysTotal++;
                    if (p != null) {
                        msg(p, BANK + "§cНедостаточно средств для платежа! §fПросрочка: §c" + Finance.money(a.arrears)
                                + "§f, дней просрочки: §c" + a.overdueDays + "§f. Начислены пени.");
                        if (a.overdueDays == 1) msg(p, BANK + "§7Пожалуйста, пополните счёт. К вам может зайти наш сотрудник.");
                    }
                } else {
                    if (!hadArrears) a.onTimePayments++;
                    a.clearArrears();
                    if (p != null && due > 0) msg(p, BANK + "Списан ежедневный платёж: " + Finance.money(due) + ". Баланс: " + Finance.money(a.balance));
                }
            }

            // подпольный цех
            if (a.hasBusiness) {
                int made = produceDirty(server, a);
                if (p != null) {
                    if (made > 0) msg(p, MFO + "§8СМС: «Станки отработали смену. В сундуке +" + Finance.money(made * 100.0) + " грязными.»");
                    else msg(p, MFO + "§cСМС: «Цех стоит — сундук в подвале полон или сломан.»");
                }
            }

            // МФО — сложные проценты
            if (a.hasMfo()) {
                if (a.mfoDays > 0) {
                    a.mfoDebt = a.mfoDebt * (1 + Config.MFO_DAILY_RATE.get()) + 5;
                    a.mfoDays--;
                    if (p != null) {
                        msg(p, MFO + "СМС: Ваш долг вырос до §c" + Finance.money(a.mfoDebt) + "§f. "
                                + (a.mfoDays > 0 ? "До возврата " + a.mfoDays + " дн." : "§cСегодня последний день!"));
                    }
                } else {
                    a.mfoDebt = a.mfoDebt * (1 + Config.MFO_OVERDUE_RATE.get()) + 50;
                    a.mfoOverdueDays++;
                    if (p != null) {
                        msg(p, MFO + "§cСМС: ПРОСРОЧКА " + a.mfoOverdueDays + " дн.! Долг: " + Finance.money(a.mfoDebt)
                                + ". Штраф 50₽. Мы знаем, где ты живёшь.");
                    }
                }
            }
        }
        data.markDirty();
    }
}
