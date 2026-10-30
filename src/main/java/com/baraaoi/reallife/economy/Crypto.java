package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.container.CryptoContainer;
import com.baraaoi.reallife.tile.CryptoRigTileEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.server.ServerWorld;

import java.util.Locale;
import java.util.Random;

/** Криптовалюта: курс BTC (случайное блуждание вокруг базового), кошелёк в счёте, продажа в рубли. */
public final class Crypto {
    private Crypto() {}

    public static final String EX = "§e[КриптоБиржа] §f";
    /** Комиссия биржи при продаже. */
    public static final double FEE = 0.01;
    private static final Random R = new Random();

    public static String btc(double v) {
        return String.format(Locale.ROOT, "%.6f ₿", v);
    }

    public static double rate(EconomyData data) {
        if (data.btcRate <= 0) data.btcRate = Config.BTC_BASE_RATE.get();
        return data.btcRate;
    }

    /** Один шаг курса: логнормальный шум + возврат к среднему. */
    public static double step(double rate, double base, Random r) {
        double lr = Math.log(rate / base);
        lr = lr * 0.97 + r.nextGaussian() * 0.06;
        lr = Math.max(Math.log(0.3), Math.min(Math.log(4.0), lr));
        return base * Math.exp(lr);
    }

    /** Курс меняется раз в минуту. */
    public static void tick(MinecraftServer server) {
        ServerWorld ow = EconomyData.overworld(server);
        if (ow == null) return;
        long now = ow.getGameTime();
        if (now % 1200 != 0) return;
        EconomyData data = EconomyData.get(server);
        double old = rate(data);
        data.btcRate = step(old, Config.BTC_BASE_RATE.get(), R);
        data.btcUpdated = now;
        data.markDirty();
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

    /** Продать percent% кошелька по текущему курсу — деньги на счёт. */
    static void sell(ServerPlayerEntity p, Account a, int percent) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        if (percent <= 0 || percent > 100) return;
        if (a.crypto <= 1e-9) { BankService.msg(p, EX + "Кошелёк пуст. Намайни на ферме и переведи в кошелёк."); return; }
        double amount = a.crypto * percent / 100.0;
        double rub = amount * rate(EconomyData.get(server)) * (1 - FEE);
        a.crypto -= amount;
        if (a.crypto < 1e-9) a.crypto = 0;
        a.balance += rub;
        BankService.msg(p, EX + "Продано §e" + btc(amount) + "§f → §a+" + Finance.money(rub) + "§f на счёт (комиссия биржи "
                + Finance.percent(FEE) + "). Осталось: §e" + btc(a.crypto));
    }
}
