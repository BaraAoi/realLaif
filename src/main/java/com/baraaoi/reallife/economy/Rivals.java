package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.structure.BizBuildings;
import com.baraaoi.reallife.structure.Blueprint;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * v1.11: боты-конкуренты. Время от времени рядом с вашим заведением открывается ларёк того же профиля, а свободные
 * помещения ТЦ занимают арендаторы-боты. Каждый конкурент того же вида рядом −15% выручки (не ниже −45%).
 * Конкуренты сами разоряются через несколько дней, или их можно выкупить (ПКМ по вывеске).
 */
public final class Rivals {
    private Rivals() {}

    public static final String RIV = "§c[Конкуренты] §f";
    public static final double PER_RIVAL = 0.15, MIN_FACTOR = 0.55;
    /** Радиус, в котором уличный конкурент отбивает покупателей. */
    public static final int RADIUS = 64;
    public static final int MAX_PER_MALL = 3, MAX_NEAR = 2;
    static final Random R = new Random();
    static int ticker;

    static final String[][] NAMES = {
            {"Шаурма «У Гиви»", "Донер «Султан»", "Шаверма 24/7", "Лаваш-Хаус", "Шаурма «Бро»"},
            {"Продукты «Берёзка»", "Магазин у дома", "Минимаркет 24 часа", "Продукты «Ассорти»"},
            {"Мойка «Блеск»", "Автомойка у Армена", "Мойка «Пена»"},
            {"Карьер «Щебень+»", "ООО «Камнедроб»", "Карьер «Гранит»"},
            {"Ломбард «Злато»", "Скупка 24", "Ломбард «Фортуна»"},
            {"АЗС «Октан»", "Заправка «Нефтяник»", "АЗС «Дизель+»"},
            {"Техно-Экспресс", "Цифровой Мир", "Гаджет-Маркет"}};

    public static String randomName(BizType t) {
        String[] n = NAMES[Math.min(t.ordinal(), NAMES.length - 1)];
        return n[R.nextInt(n.length)];
    }

    // ------------------------------------------------------------------ влияние на прибыль

    /** Сколько конкурентов того же вида отбивают покупателей у заведения. */
    public static int count(EconomyData data, LegalBusiness b) {
        if (data == null) return 0;
        int n = 0;
        for (Rival r : data.rivals) {
            if (r.type != b.type) continue;
            if (b.inMall()) {
                if (r.mallId == b.mallId) n++;
            } else if (!r.inMall() && r.center().distSqr(b.counter) <= (double) RADIUS * RADIUS) n++;
        }
        return n;
    }

    public static double factor(EconomyData data, LegalBusiness b) {
        int n = count(data, b);
        return n <= 0 ? 1.0 : Math.max(MIN_FACTOR, 1.0 - PER_RIVAL * n);
    }

    public static int buyoutPrice(Rival r) {
        return Math.max(3000, r.type.price);
    }

    // ------------------------------------------------------------------ появление

    public static void tick(MinecraftServer server) {
        if (++ticker % 6000 != 0) return;
        if (!Config.RIVALS_ENABLED.get()) return;
        ServerLevel ow = EconomyData.overworld(server);
        EconomyData data = EconomyData.get(server);
        long today = BankService.today(server);
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (p.level() != ow || p.isSpectator()) continue;
            Account a = data.get(p);
            if (a.captivity != Account.FREE) continue;
            for (Mall m : data.malls) {
                BlockPos c = m.center();
                if (p.distanceToSqr(c.getX(), c.getY(), c.getZ()) > 120 * 120 || !ow.isLoaded(c)) continue;
                if (R.nextDouble() < 0.2) tryMall(ow, data, m, today);
            }
            for (LegalBusiness b : new ArrayList<>(data.businesses)) {
                if (!p.getUUID().equals(b.owner) || b.inMall() || b.days < 2) continue;
                if (p.distanceToSqr(b.counter.getX(), b.counter.getY(), b.counter.getZ()) > 96 * 96) continue;
                if (count(data, b) >= MAX_NEAR || R.nextDouble() >= Config.RIVAL_CHANCE.get()) continue;
                Rival r = spawnNear(ow, data, b, today);
                if (r != null) {
                    int dist = (int) Math.sqrt(r.center().distSqr(b.counter));
                    BankService.msg(p, RIV + "§cВ " + dist + " блоках от «" + b.type.title + "» открылся «" + r.name + "»!§f Пока он работает, "
                            + "выручка −" + Math.round(PER_RIVAL * 100) + "%. Разорится сам через несколько дней — или выкупите его за "
                            + Finance.money(buyoutPrice(r)) + " (ПКМ по вывеске).");
                }
            }
        }
    }

    /** Админ-команда: конкурент прямо сейчас (в ТЦ, где стоит игрок, или у его ближайшего заведения). */
    public static void forceSpawn(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        ServerLevel ow = EconomyData.overworld(server);
        EconomyData data = EconomyData.get(server);
        long today = BankService.today(server);
        Mall m = data.mallAt(p.blockPosition(), 4);
        if (m != null) {
            Rival r = tryMall(ow, data, m, today);
            BankService.msg(p, RIV + (r != null ? "В ТЦ заехал «" + r.name + "»." : "Свободных помещений нет или ботов уже " + MAX_PER_MALL + "."));
            return;
        }
        LegalBusiness best = null;
        for (LegalBusiness b : data.businesses)
            if (!b.inMall() && (best == null || b.counter.distSqr(p.blockPosition()) < best.counter.distSqr(p.blockPosition()))) best = b;
        if (best == null) {
            BankService.msg(p, RIV + "Рядом нет уличных заведений.");
            return;
        }
        Rival r = spawnNear(ow, data, best, today);
        BankService.msg(p, RIV + (r != null ? "У «" + best.type.title + "» открылся «" + r.name + "» на " + r.center().getX() + ", " + r.center().getY() + ", " + r.center().getZ() + "."
                : "Не нашлось ровного свободного места в 20–40 блоках (чанки должны быть загружены)."));
    }

    /** Бот занимает свободное помещение ТЦ. */
    
    static Rival tryMall(ServerLevel ow, EconomyData data, Mall m, long today) {
        int bots = 0;
        List<Mall.Unit> free = new ArrayList<>();
        for (Mall.Unit u : m.units) {
            if (u.rival != 0) bots++;
            else if (u.free() && !u.sign.equals(BlockPos.ZERO) && ow.isLoaded(u.entrance)) free.add(u);
        }
        if (bots >= MAX_PER_MALL || free.isEmpty()) return null;
        Mall.Unit u = free.get(R.nextInt(free.size()));
        // чаще всего бот выбирает профиль, который уже есть в ТЦ — так интереснее конкурировать
        List<BizType> present = new ArrayList<>();
        for (LegalBusiness b : data.businesses) if (b.mallId == m.id) present.add(b.type);
        BizType[] mallTypes = {BizType.SHAWARMA, BizType.MINIMARKET, BizType.PAWNSHOP, BizType.ELECTRONICS};
        BizType t = !present.isEmpty() && R.nextDouble() < 0.6 ? present.get(R.nextInt(present.size())) : mallTypes[R.nextInt(mallTypes.length)];
        Rival r = openInMall(ow, data, m, u, t, today);
        notifyMall(ow.getServer(), data, m, u, r);
        return r;
    }

    /** Открыть бота в помещении (без проверок). Используется и самотестом. */
    public static Rival openInMall(Level w, EconomyData data, Mall m, Mall.Unit u, BizType t, long today) {
        Rival r = new Rival();
        r.id = data.nextRivalId++;
        r.type = t;
        r.name = randomName(t);
        r.mallId = m.id;
        r.unit = u.idx;
        r.sign = u.sign;
        r.min = u.min;
        r.max = u.max;
        r.until = today + 5 + R.nextInt(6);
        Blueprint b = BizBuildings.decorateUnit(w, u, t);
        r.snapshot = b.snapshot();
        u.rival = r.id;
        data.rivals.add(r);
        Malls.updateSign(w, data, m, u);
        data.setDirty();
        return r;
    }

    static void notifyMall(MinecraftServer server, EconomyData data, Mall m, Mall.Unit u, Rival r) {
        if (server == null) return;
        ServerPlayer op = m.owner == null ? null : server.getPlayerList().getPlayer(m.owner);
        if (op != null) BankService.msg(op, Malls.TC + "В помещение №" + (u.idx + 1) + " вашего ТЦ заехал арендатор-бот «" + r.name
                + "». Аренду §a" + Finance.money(u.rent) + "/день§f он платит вам, но отбивает покупателей у «" + BizBuild.shortTitle(r.type) + "».");
        for (LegalBusiness b : data.businesses) {
            if (b.mallId != m.id || b.type != r.type) continue;
            ServerPlayer bp = server.getPlayerList().getPlayer(b.owner);
            if (bp != null && bp != op) BankService.msg(bp, RIV + "§cВ ТЦ открылся конкурент «" + r.name + "» (помещение №" + (u.idx + 1)
                    + ")§f: выручка «" + b.type.title + "» −" + Math.round(PER_RIVAL * 100) + "%. Можно выкупить: ПКМ по его табличке.");
        }
    }

    /** Ларёк конкурента на улице в 20–40 блоках от заведения. */
    
    static Rival spawnNear(ServerLevel w, EconomyData data, LegalBusiness b, long today) {
        UUID nobody = UUID.randomUUID();
        for (int attempt = 0; attempt < 24; attempt++) {
            double ang = R.nextDouble() * Math.PI * 2;
            int dist = 20 + R.nextInt(21);
            int x = b.counter.getX() + (int) Math.round(Math.cos(ang) * dist);
            int z = b.counter.getZ() + (int) Math.round(Math.sin(ang) * dist);
            BlockPos probe = new BlockPos(x, 64, z);
            if (!w.isLoaded(probe) || !w.isLoaded(probe.offset(8, 0, 8)) || !w.isLoaded(probe.offset(-8, 0, -8))) continue;
            int h = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(h - b.counter.getY()) > 12) continue;
            BlockPos origin = new BlockPos(x, h, z);
            BlockState ground = w.getBlockState(origin.below());
            if (ground.liquid() || !ground.isSolid()) continue;
            // лицом к заведению игрока
            Direction fwd = Direction.getApproximateNearest(x - b.counter.getX(), 0, z - b.counter.getZ());
            if (fwd.getAxis().isVertical()) fwd = Direction.NORTH;
            Blueprint probeBp = new Blueprint(w, origin, fwd);
            if (!flat(w, probeBp, h)) continue;
            if (BizBuild.boxProblem(w, probeBp, -4, -1, -2, 4, 6, 7, data, nobody) != null) continue;
            if (tooCloseToBusiness(data, origin)) continue;
            return build(w, data, origin, fwd, b.type, today);
        }
        return null;
    }

    static boolean flat(Level w, Blueprint b, int h) {
        for (int[] c : new int[][]{{-3, 0}, {3, 0}, {-3, 6}, {3, 6}, {0, 3}}) {
            BlockPos p = b.pos(c[0], 0, c[1]);
            int y = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ());
            if (Math.abs(y - h) > 2) return false;
            if (w.getBlockState(new BlockPos(p.getX(), y - 1, p.getZ())).liquid()) return false;
        }
        return true;
    }

    static boolean tooCloseToBusiness(EconomyData data, BlockPos origin) {
        for (LegalBusiness lb : data.businesses)
            if (Math.abs(lb.counter.getX() - origin.getX()) <= LegalBusiness.R_XZ + 11 && Math.abs(lb.counter.getZ() - origin.getZ()) <= LegalBusiness.R_XZ + 11) return true;
        for (Rival r : data.rivals)
            if (!r.inMall() && r.center().distSqr(origin) < 16 * 16) return true;
        return false;
    }

    /** Построить уличный ларёк (без проверок). Используется и самотестом. */
    public static Rival build(Level w, EconomyData data, BlockPos origin, Direction fwd, BizType t, long today) {
        Rival r = new Rival();
        r.id = data.nextRivalId++;
        r.type = t;
        r.name = randomName(t);
        Blueprint bp = BizBuildings.buildKiosk(w, origin, fwd, t, r.name);
        r.snapshot = bp.snapshot();
        r.min = bp.min();
        r.max = bp.max();
        r.sign = bp.pos(0, 3, -1).immutable();
        r.until = today + 4 + R.nextInt(6);
        data.rivals.add(r);
        data.setDirty();
        return r;
    }

    // ------------------------------------------------------------------ закрытие и выкуп

    public static void processDay(MinecraftServer server, EconomyData data) {
        long today = BankService.today(server);
        for (Rival r : new ArrayList<>(data.rivals)) {
            if (r.until > today) continue;
            close(server, data, r);
            notifyClosed(server, data, r, "разорился и закрылся");
        }
    }

    public static void close(MinecraftServer server, EconomyData data, Rival r) {
        close(EconomyData.overworld(server), data, r);
    }

    public static void close(Level w, EconomyData data, Rival r) {
        Blueprint.restore(w, r.snapshot);
        data.rivals.remove(r);
        if (r.inMall()) {
            Mall m = data.mallById(r.mallId);
            Mall.Unit u = m == null ? null : m.unit(r.unit);
            if (u != null && u.rival == r.id) {
                u.rival = 0;
                Malls.updateSign(w, data, m, u);
            }
        }
        data.setDirty();
    }

    static void notifyClosed(MinecraftServer server, EconomyData data, Rival r, String how) {
        for (LegalBusiness b : data.businesses) {
            if (b.type != r.type) continue;
            boolean affected = r.inMall() ? b.mallId == r.mallId : !b.inMall() && r.center().distSqr(b.counter) <= (double) RADIUS * RADIUS;
            if (!affected) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(b.owner);
            if (p != null) BankService.msg(p, RIV + "§aКонкурент «" + r.name + "» " + how + ".§f Покупатели возвращаются в «" + b.type.title + "».");
        }
    }

    public static boolean onSignClick(ServerPlayer p, BlockPos pos) {
        if (p.level().getServer() == null) return false;
        EconomyData data = EconomyData.get(p.level().getServer());
        for (Rival r : data.rivals) {
            if (!r.sign.equals(pos)) continue;
            info(p, data, r);
            return true;
        }
        return false;
    }

    public static void info(ServerPlayer p, EconomyData data, Rival r) {
        long left = Math.max(0, r.until - BankService.today(p.level().getServer()));
        BankService.msg(p, RIV + "«" + r.name + "» — " + BizBuild.shortTitle(r.type) + ", бот-конкурент. Отбивает "
                + Math.round(PER_RIVAL * 100) + "% выручки у заведений того же вида " + (r.inMall() ? "в этом ТЦ" : "в радиусе " + RADIUS + " блоков")
                + ". Разорится примерно через " + left + " дн.");
        net.minecraft.network.chat.MutableComponent b = Component.literal("§c§l[Выкупить и закрыть за " + Finance.money(buyoutPrice(r)) + "]");
        b.setStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/reallife rival buyout " + r.id)));
        p.sendSystemMessage(Component.literal("§c  ").append(b));
    }

    public static void buyout(ServerPlayer p, int id) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Rival r = data.rivalById(id);
        if (r == null) {
            BankService.msg(p, RIV + "Этого конкурента уже нет.");
            return;
        }
        Account a = data.get(p);
        if (a.captivity != Account.FREE) return;
        if (p.distanceToSqr(r.sign.getX(), r.sign.getY(), r.sign.getZ()) > 48 * 48) {
            BankService.msg(p, RIV + "§cПодойдите к конкуренту — сделку оформляют на месте.");
            return;
        }
        int price = buyoutPrice(r);
        if (!BankService.charge(p, a, price)) {
            BankService.msg(p, RIV + "§cВыкуп стоит " + Finance.money(price) + ". Не хватает.");
            return;
        }
        close(server, data, r);
        BankService.sync(p);
        BankService.msg(p, RIV + "§aВы выкупили «" + r.name + "» за " + Finance.money(price) + " и закрыли его." + (r.inMall() ? " Помещение снова свободно." : " Ларёк снесён."));
        notifyClosed(server, data, r, "выкуплен и закрыт");
    }
}
