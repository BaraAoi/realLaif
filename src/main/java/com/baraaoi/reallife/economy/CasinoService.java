package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.container.CasinoContainer;
import com.baraaoi.reallife.item.CloverItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Казино «Три семёрки». Шансы подняты (v4): возврат игроку 96–99.5%, клевер даёт лучший исход.
 * Охрана больше никого не выгоняет — выигрывай сколько хочешь.
 */
public class CasinoService {
    public static final String CASINO = "§5[Казино «Три семёрки»] §f";
    public static final int COIN = 0, SLOTS = 1, ANARCHY = 2, ROULETTE = 3;
    public static final String[] TITLES = {"Орёл и решка", "Однорукий бандит", "Мама Анархия", "Рулетка"};

    /**
     * Однорукий бандит: 5 символов, 777 — x25, три одинаковых — x8, два одинаковых — x1 (возврат ставки).
     * Исход выбирается заранее (в промилле): 777 — 18‰, другая тройка — 55‰, пара — 70‰, иначе пусто.
     * Троек стало 7.3% (было 4%), пар — 7% (было 48%). Возврат 96%.
     */
    public static final int SLOT_777_PM = 18, SLOT_TRIPLE_PM = 55, SLOT_PAIR_PM = 70;
    /** Шанс угадать в «Орёл и решка». */
    public static final double COIN_WIN = 0.49;
    public static final String[] SYMBOLS = {"7", "BAR", "Вишня", "Лимон", "Колокол"};
    public static final int[] SYMBOL_COLORS = {0xFF3030, 0xFFFFFF, 0xFF5577, 0xFFF04A, 0xFFB000};

    /** Рулетка: 100 секторов. x0 — 64, x1 — 10, x2 — 20, x3 — 3, x5 — 1, x10 — 1, x25 — 1 (возврат 99%, выигрыш 26%). */
    public static final int[] WHEEL_COUNTS = {64, 10, 20, 3, 1, 1, 1};
    public static final int[] WHEEL_MULTS = {0, 1, 2, 3, 5, 10, 25};
    public static final int[] WHEEL = buildWheel();

    /** Мама Анархия: хаос решает. Множители и веса (возврат 99.5%). */
    public static final double[] ANARCHY_MULT = {0, 1, 1.5, 2, 5, 8};
    public static final int[] ANARCHY_WEIGHT = {46, 22, 15, 11, 5, 1};
    public static final String[] ANARCHY_TEXT = {"Анархия забрала всё!", "Хаос вернул своё.", "Мама довольна: x1.5",
            "Бунт удался: x2", "Революция! x5", "МАМА — АНАРХИЯ! x8"};

    private static final Random RANDOM = new Random();
    private static final Map<UUID, CompoundTag> LAST = new HashMap<>();

    private static int[] buildWheel() {
        int[] counts = WHEEL_COUNTS;
        int[] mult = WHEEL_MULTS;
        int[] w = new int[100];
        int i = 0;
        for (int k = 0; k < counts.length; k++) for (int c = 0; c < counts[k]; c++) w[i++] = mult[k];
        Random r = new Random(777L);
        for (int k = w.length - 1; k > 0; k--) {
            int j = r.nextInt(k + 1);
            int t = w[k]; w[k] = w[j]; w[j] = t;
        }
        return w;
    }

    public static double slotMult(int[] reels) {
        if (reels[0] == reels[1] && reels[1] == reels[2]) return reels[0] == 0 ? 25 : 8;
        if (reels[0] == reels[1] || reels[1] == reels[2] || reels[0] == reels[2]) return 1;
        return 0;
    }

    /** Крутануть барабаны: сначала решаем исход, потом подбираем символы под него. */
    public static int[] spinSlots(Random r) {
        int roll = r.nextInt(1000);
        if (roll < SLOT_777_PM) return new int[]{0, 0, 0};
        roll -= SLOT_777_PM;
        int n = SYMBOLS.length;
        if (roll < SLOT_TRIPLE_PM) {
            int s = 1 + r.nextInt(n - 1);
            return new int[]{s, s, s};
        }
        roll -= SLOT_TRIPLE_PM;
        if (roll < SLOT_PAIR_PM) {
            int s = r.nextInt(n);
            int o = (s + 1 + r.nextInt(n - 1)) % n;
            int[] res = {s, s, s};
            res[r.nextInt(3)] = o;
            return res;
        }
        // все разные
        int a = r.nextInt(n);
        int b = (a + 1 + r.nextInt(n - 1)) % n;
        int c;
        do { c = r.nextInt(n); } while (c == a || c == b);
        return new int[]{a, b, c};
    }

    /** Теоретический возврат однорукого бандита. */
    public static double slotsRtp() {
        return (SLOT_777_PM * 25 + SLOT_TRIPLE_PM * 8 + SLOT_PAIR_PM) / 1000.0;
    }

    public static double wheelRtp() {
        double s = 0;
        for (int m : WHEEL) s += m;
        return s / WHEEL.length;
    }

    public static double anarchyRtp() {
        double s = 0, w = 0;
        for (int i = 0; i < ANARCHY_MULT.length; i++) { s += ANARCHY_MULT[i] * ANARCHY_WEIGHT[i]; w += ANARCHY_WEIGHT[i]; }
        return s / w;
    }

    public static CompoundTag last(UUID id) {
        CompoundTag t = LAST.get(id);
        return t == null ? new CompoundTag() : t;
    }

    // ------------------------------------------------------------------ открыть

    public static void open(ServerPlayer p, BlockPos pos, int kind) {
        EconomyData data = EconomyData.get(p.level().getServer());
        Account a = data.get(p);
        if (a.captivity != Account.FREE) return;
        p.openMenu(new SimpleMenuProvider(
                (id, inv, pl) -> new CasinoContainer(id, inv, pos, kind),
                Component.literal(TITLES[Math.max(0, Math.min(3, kind))])), buf -> {
            buf.writeBlockPos(pos);
            buf.writeVarInt(kind);
        });
        BankService.sync(p);
    }

    // ------------------------------------------------------------------ игра

    /** Найти клевер в инвентаре и потратить одну игру. */
    private static boolean useClover(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.getItem() instanceof CloverItem) {
                st.setDamage(st.getDamage() + 1);
                if (st.getDamage() >= st.getMaxDamage()) {
                    p.getInventory().setItem(i, ItemStack.EMPTY);
                    BankService.msg(p, CASINO + "§7Клевер завял. Удача кончилась.");
                }
                return true;
            }
        }
        return false;
    }

    public static void play(ServerPlayer p, CasinoContainer c, int bet, int choice) {
        EconomyData data = EconomyData.get(p.level().getServer());
        Account a = data.get(p);
        long now = EconomyData.overworld(p.level().getServer()).getGameTime();
        if (a.captivity != Account.FREE) return;
        int max = Config.CASINO_MAX_BET.get();
        bet = bet / 10 * 10;
        if (bet < 10 || bet > max) {
            BankService.msg(p, CASINO + "Ставка от 10₽ до " + Finance.money(max) + ".");
            return;
        }
        if (!BankService.charge(p, a, bet)) {
            BankService.msg(p, CASINO + "§cНе хватает денег на ставку " + Finance.money(bet) + " (счёт + наличные).");
            return;
        }
        boolean lucky = useClover(p);
        int kind = c.getKind();
        CompoundTag r = new CompoundTag();
        r.putInt("kind", kind);
        r.putLong("id", now * 31 + RANDOM.nextInt(1000));
        r.putInt("bet", bet);
        r.putBoolean("clover", lucky);
        double mult;
        String text;
        switch (kind) {
            case COIN: {
                choice = choice == 1 ? 1 : 0;
                boolean win = lucky || RANDOM.nextDouble() < COIN_WIN;
                int side = win ? choice : 1 - choice;
                mult = win ? 2 : 0;
                r.putInt("side", side);
                r.putInt("choice", choice);
                text = "Выпал" + (side == 0 ? " орёл" : "а решка");
                break;
            }
            case SLOTS: {
                int[] reels = lucky ? new int[]{0, 0, 0} : spinSlots(RANDOM);
                mult = slotMult(reels);
                r.putIntArray("reels", reels);
                text = SYMBOLS[reels[0]] + " | " + SYMBOLS[reels[1]] + " | " + SYMBOLS[reels[2]];
                break;
            }
            case ANARCHY: {
                int idx = ANARCHY_MULT.length - 1;
                if (!lucky) {
                    int roll = RANDOM.nextInt(100);
                    for (int i = 0; i < ANARCHY_WEIGHT.length; i++) {
                        if (roll < ANARCHY_WEIGHT[i]) { idx = i; break; }
                        roll -= ANARCHY_WEIGHT[i];
                    }
                }
                mult = ANARCHY_MULT[idx];
                r.putInt("sector", idx);
                text = ANARCHY_TEXT[idx];
                break;
            }
            default: {
                int sector = RANDOM.nextInt(WHEEL.length);
                if (lucky) for (int i = 0; i < WHEEL.length; i++) if (WHEEL[i] == 25) sector = i;
                mult = WHEEL[sector];
                r.putInt("sector", sector);
                text = "Шарик остановился на x" + WHEEL[sector];
                break;
            }
        }
        double win = bet * mult;
        double cash = Math.floor(win / 10.0) * 10.0;
        if (cash > 0) BankService.giveCash(p, cash);
        a.balance += win - cash;
        long day = BankService.today(p.level().getServer());
        if (a.casinoDay != day) {
            a.casinoDay = day;
            a.casinoProfit = 0;
        }
        a.casinoProfit += win - bet;
        r.putDouble("mult", mult);
        r.putDouble("win", win);
        r.putString("text", text);
        LAST.put(p.getUUID(), r);
        if (mult > 1) {
            BankService.actionBar(p, "§a" + text + " — выигрыш " + Finance.money(win) + (lucky ? " §2(клевер)" : ""));
        } else if (mult == 1) {
            BankService.actionBar(p, "§e" + text + " — ставка возвращена");
        } else {
            BankService.actionBar(p, "§c" + text + " — ставка " + Finance.money(bet) + " сгорела");
        }
        if (mult >= 8) {
            for (ServerPlayer o : p.level().getServer().getPlayerList().getPlayers()) {
                if (o != p && o.distanceToSqr(p) < 48 * 48) BankService.msg(o, CASINO + "§e" + p.getGameProfile().getName()
                        + " сорвал куш: " + Finance.money(win) + "!");
            }
        }
        data.setDirty();
        BankService.sync(p);
    }
}
