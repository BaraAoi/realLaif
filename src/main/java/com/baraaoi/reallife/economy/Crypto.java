package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.container.CryptoContainer;
import com.baraaoi.reallife.tile.CryptoRigTileEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;

import java.util.Locale;
import java.util.Random;

/**
 * Криптобиржа (v1.8): четыре монеты с курсами, которые гуляют каждые 20 секунд (случайное блуждание вокруг
 * среднего + редкие «новости» с резкими скачками). Монеты можно покупать за рубли со счёта и продавать обратно.
 * BTC ещё и майнится на ферме.
 */
public final class Crypto {
    private Crypto() {}

    public static final String EX = "§e[КриптоБиржа] §f";
    /** Комиссия биржи при покупке и продаже. */
    public static final double FEE = 0.01;
    /** Курс меняется раз в столько тиков (400 = 20 секунд). */
    public static final int STEP_TICKS = 400;
    public static final int HIST = 40;
    private static final Random R = new Random();

    /** Монеты. Порядок не менять — номер хранится в счёте. */
    public enum Coin {
        BTC("Биткоин", "BTC", 0, 0.035, 6),
        ETH("Эфириум", "ETH", 80000, 0.05, 5),
        TON("Тонкоин", "TON", 400, 0.07, 4),
        DOGE("Догикоин", "DOGE", 20, 0.11, 2);

        public final String title;
        public final String ticker;
        private final double fixedBase;
        /** Волатильность за шаг (20 с). */
        public final double vol;
        /** Знаков после запятой при показе количества. */
        public final int digits;

        Coin(String title, String ticker, double fixedBase, double vol, int digits) {
            this.title = title;
            this.ticker = ticker;
            this.fixedBase = fixedBase;
            this.vol = vol;
            this.digits = digits;
        }

        /** Средний курс, вокруг которого гуляет цена. */
        public double base() {
            return this == BTC ? Config.BTC_BASE_RATE.get() : fixedBase;
        }

        public static Coin byId(int i) {
            Coin[] v = values();
            return i >= 0 && i < v.length ? v[i] : null;
        }
    }

    private static final String[][] NEWS_UP = {
            {"Илон Маск твитнул про %s!", "Крупный фонд закупил %s.", "Биржа «Бинанс» добавила %s в топ.", "Блогер-миллионник посоветовал %s."}};
    private static final String[][] NEWS_DOWN = {
            {"Регулятор пригрозил запретом %s.", "Взломали кошелёк с %s!", "Кит слил крупную партию %s.", "СМИ: «%s — это пирамида»."}};

    public static String fmt(Coin c, double v) {
        return String.format(Locale.ROOT, "%." + c.digits + "f %s", v, c.ticker);
    }

    /** Курс: дешёвые монеты — с копейками. */
    public static String price(double v) {
        return v < 100 ? String.format(Locale.ROOT, "%.2f₽", v) : Finance.money(v);
    }

    public static String btc(double v) {
        return fmt(Coin.BTC, v);
    }

    public static double rate(EconomyData data) {
        return rate(data, Coin.BTC);
    }

    public static double rate(EconomyData data, Coin c) {
        if (c == Coin.BTC) {
            if (data.btcRate <= 0) data.btcRate = c.base();
            return data.btcRate;
        }
        if (data.altRates[c.ordinal()] <= 0) data.altRates[c.ordinal()] = c.base();
        return data.altRates[c.ordinal()];
    }

    static void setRate(EconomyData data, Coin c, double v) {
        if (c == Coin.BTC) data.btcRate = v;
        else data.altRates[c.ordinal()] = v;
    }

    public static double held(Account a, Coin c) {
        return c == Coin.BTC ? a.crypto : a.coins[c.ordinal()];
    }

    static void setHeld(Account a, Coin c, double v) {
        if (v < 1e-12) v = 0;
        if (c == Coin.BTC) a.crypto = v;
        else a.coins[c.ordinal()] = v;
    }

    /** Стоимость всех монет игрока в рублях. */
    public static double portfolio(EconomyData data, Account a) {
        double s = 0;
        for (Coin c : Coin.values()) s += held(a, c) * rate(data, c);
        return s;
    }

    /** Старый шаг (раз в минуту, волатильность BTC 6%) — для самотеста. */
    public static double step(double rate, double base, Random r) {
        return step(rate, base, 0.06, 0.97, r);
    }

    /** Один шаг курса: логнормальный шум + возврат к среднему; диапазон 0.2–5 от среднего. */
    public static double step(double rate, double base, double vol, double revert, Random r) {
        double lr = Math.log(rate / base);
        lr = lr * revert + r.nextGaussian() * vol;
        lr = Math.max(Math.log(0.2), Math.min(Math.log(5.0), lr));
        return base * Math.exp(lr);
    }

    /** Курсы меняются раз в 20 секунд. */
    public static void tick(MinecraftServer server) {
        ServerWorld ow = EconomyData.overworld(server);
        if (ow == null) return;
        long now = ow.getGameTime();
        EconomyData data = EconomyData.get(server);
        if (data.cryptoHist.isEmpty()) snapshot(data);
        if (now % STEP_TICKS != 0) return;
        for (Coin c : Coin.values()) {
            double old = rate(data, c);
            double nv = step(old, c.base(), c.vol, 0.97, R);
            // редкие новости: резкий скачок
            if (R.nextDouble() < 0.012) {
                boolean up = R.nextBoolean();
                double jump = 0.15 + R.nextDouble() * 0.25;
                nv = Math.max(c.base() * 0.2, Math.min(c.base() * 5, nv * (up ? 1 + jump : 1 - jump * 0.7)));
                String[] pool = (up ? NEWS_UP : NEWS_DOWN)[0];
                String text = EX + (up ? "§a" : "§c") + String.format(pool[R.nextInt(pool.length)], c.title) + " §f" + c.ticker + " "
                        + (up ? "§a▲" : "§c▼") + String.format(Locale.ROOT, " %+.0f%%", (nv / old - 1) * 100) + "§f → " + price(nv);
                for (ServerPlayerEntity p : server.getPlayerList().getPlayers()) p.sendStatusMessage(new StringTextComponent(text), false);
            }
            setRate(data, c, nv);
        }
        data.btcUpdated = now;
        snapshot(data);
        data.markDirty();
    }

    private static void snapshot(EconomyData data) {
        double[] s = new double[Coin.values().length];
        for (Coin c : Coin.values()) s[c.ordinal()] = rate(data, c);
        data.cryptoHist.addLast(s);
        while (data.cryptoHist.size() > HIST) data.cryptoHist.removeFirst();
    }

    /** Данные биржи для телефона. */
    public static ListNBT sync(EconomyData data, Account a) {
        ListNBT l = new ListNBT();
        for (Coin c : Coin.values()) {
            CompoundNBT t = new CompoundNBT();
            t.putDouble("rate", rate(data, c));
            t.putDouble("base", c.base());
            t.putDouble("held", held(a, c));
            ListNBT h = new ListNBT();
            for (double[] s : data.cryptoHist) h.add(DoubleNBT.valueOf(s[c.ordinal()]));
            t.put("hist", h);
            l.add(t);
        }
        return l;
    }

    /** Перевести добытое фермой в кошелёк игрока. */
    static void withdrawRig(ServerPlayerEntity p, Account a, CryptoContainer c) {
        TileEntity te = p.world.getTileEntity(c.getPos());
        if (!(te instanceof CryptoRigTileEntity)) return;
        double m = ((CryptoRigTileEntity) te).takeMined();
        if (m <= 0) { BankService.msg(p, EX + "На ферме пока ничего не намайнено."); return; }
        a.crypto += m;
        BankService.msg(p, EX + "Переведено в кошелёк: §e" + btc(m) + "§f. В кошельке: §e" + btc(a.crypto));
    }

    /** Продать percent% монеты по текущему курсу — деньги на счёт. */
    static void sell(ServerPlayerEntity p, Account a, int percent, int coinIdx) {
        MinecraftServer server = p.getServer();
        Coin c = Coin.byId(coinIdx);
        if (server == null || c == null || percent <= 0 || percent > 100) return;
        double have = held(a, c);
        if (have <= 1e-12) { BankService.msg(p, EX + "В кошельке нет " + c.ticker + ". Купите на бирже" + (c == Coin.BTC ? " или намайните на ферме." : ".")); return; }
        double amount = have * percent / 100.0;
        double rub = amount * rate(EconomyData.get(server), c) * (1 - FEE);
        setHeld(a, c, have - amount);
        a.balance += rub;
        BankService.msg(p, EX + "Продано §e" + fmt(c, amount) + "§f → §a+" + Finance.money(rub) + "§f на счёт (комиссия "
                + Finance.percent(FEE) + "). Осталось: §e" + fmt(c, held(a, c)));
    }

    /** Купить монету на rub рублей со счёта (rub ≤ 0 — на весь положительный остаток). */
    static void buy(ServerPlayerEntity p, Account a, int coinIdx, int rub) {
        MinecraftServer server = p.getServer();
        Coin c = Coin.byId(coinIdx);
        if (server == null || c == null) return;
        if (a.captivity != Account.FREE) { BankService.msg(p, EX + "§cИз-под стражи биржа недоступна."); return; }
        double avail = Math.floor(Math.max(0, a.balance));
        double spend = rub <= 0 ? avail : rub;
        if (spend < 10) { BankService.msg(p, EX + "§cНа счёте нет денег для покупки (минимум 10₽)."); return; }
        if (spend > avail + 1e-6) {
            BankService.msg(p, EX + "§cНа счёте только " + Finance.money(avail) + ". Пополните счёт или купите на меньшую сумму.");
            return;
        }
        double r = rate(EconomyData.get(server), c);
        double amount = spend * (1 - FEE) / r;
        a.balance -= spend;
        setHeld(a, c, held(a, c) + amount);
        BankService.msg(p, EX + "Куплено §e" + fmt(c, amount) + "§f за §c" + Finance.money(spend) + "§f по курсу " + price(r)
                + " (комиссия " + Finance.percent(FEE) + "). В кошельке: §e" + fmt(c, held(a, c)));
    }
}
