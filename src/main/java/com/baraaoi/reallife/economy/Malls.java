package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.structure.BizBuildings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * v1.11: помещения торгового центра — покупка, аренда, выселение, права на стройку, таблички.
 */
public final class Malls {
    private Malls() {}

    public static final String TC = "§d[ТЦ] §f";

    // ------------------------------------------------------------------ таблички

    public static String[] signText(EconomyData data, Mall m, Mall.Unit u) {
        String head = "№" + (u.idx + 1) + " " + u.label();
        if (u.rival != 0) {
            Rival r = data.rivalById(u.rival);
            String[] nl = BizBuildings.signLines(r == null ? "Конкурент" : r.name);
            return new String[]{nl[0], nl[1], "(конкурент)", "ПКМ — выкупить"};
        }
        if (u.tenant != null) {
            LegalBusiness b = m.business(data, u);
            return new String[]{head, u.tenantName, b == null ? "" : BizBuild.shortTitle(b.type), u.tenant.equals(m.owner) ? "" : "аренда"};
        }
        return new String[]{head, u.width + "×" + u.depth + " продаётся", Finance.money(u.price), "аренда " + Finance.money(u.rent) + "/д"};
    }

    public static void updateSign(Level w, EconomyData data, Mall m, Mall.Unit u) {
        if (u.sign.equals(BlockPos.ZERO)) return;
        BizBuildings.writeSign(w, u.sign, signText(data, m, u));
    }

    public static void updateSign(MinecraftServer server, EconomyData data, Mall m, Mall.Unit u) {
        updateSign(EconomyData.overworld(server), data, m, u);
    }

    // ------------------------------------------------------------------ поиск

    
    public static Mall.Unit ownUnitAt(EconomyData data, UUID player, BlockPos pos) {
        Mall m = data.mallAt(pos, 0);
        if (m == null) return null;
        Mall.Unit u = m.unitAt(pos);
        return u != null && player.equals(u.tenant) ? u : null;
    }

    /** Почему игроку нельзя строить/ломать здесь (null — можно или это не ТЦ). */
    
    public static String editDenied(EconomyData data, UUID player, BlockPos pos) {
        Mall m = data.mallAt(pos, 0);
        if (m == null) return null;
        Mall.Unit u = m.unitAt(pos);
        if (u != null) {
            if (player.equals(u.tenant)) return null;
            if (u.rival != 0) return "помещение №" + (u.idx + 1) + " занял конкурент — выкупите его (ПКМ по табличке)";
            if (u.tenant == null) return player.equals(m.owner) ? null
                    : "помещение №" + (u.idx + 1) + " продаётся — купите его (ПКМ по табличке над входом)";
            return "помещение №" + (u.idx + 1) + " принадлежит " + u.tenantName;
        }
        if (player.equals(m.owner)) return null;
        return "это общая часть торгового центра (владелец " + m.ownerName + ")";
    }

    // ------------------------------------------------------------------ действия

    /** ПКМ по табличке: true — это была табличка помещения. */
    public static boolean onSignClick(ServerPlayer p, BlockPos pos) {
        if (p.level().getServer() == null) return false;
        EconomyData data = EconomyData.get(p.level().getServer());
        for (Mall m : data.malls) {
            if (!m.contains(pos, 2)) continue;
            for (Mall.Unit u : m.units) {
                if (!u.sign.equals(pos)) continue;
                info(p, data, m, u);
                return true;
            }
        }
        return false;
    }

    static net.minecraft.network.chat.MutableComponent button(String label, String cmd) {
        net.minecraft.network.chat.MutableComponent c = Component.literal(label);
        c.setStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand(cmd)));
        return c;
    }

    public static void info(ServerPlayer p, EconomyData data, Mall m, Mall.Unit u) {
        UUID id = p.getUUID();
        boolean owner = id.equals(m.owner);
        BankService.msg(p, TC + "Помещение §e№" + (u.idx + 1) + "§f (" + u.label() + ", " + u.width + "×" + u.depth + " = " + u.area()
                + " м²) в ТЦ «" + m.ownerName + "».");
        if (u.rival != 0) {
            Rival r = data.rivalById(u.rival);
            if (r != null) Rivals.info(p, data, r);
            return;
        }
        String cmd = " " + m.id + " " + u.idx;
        if (u.tenant == null) {
            if (owner) {
                BankService.msg(p, TC + "Оно ваше и свободно. Займите сами (бесплатно, без аренды) или ждите покупателя: другой игрок заплатит §a"
                        + Finance.money(u.price) + "§f и будет платить вам §a" + Finance.money(u.rent) + "/день§f.");
                p.sendMessage(Component.literal("§d  ").append(button("§a§l[Занять себе]", "/reallife mall take" + cmd)), p.getUUID());
            } else {
                BankService.msg(p, TC + "Цена §e" + Finance.money(u.price) + "§f (владельцу ТЦ) + аренда §e" + Finance.money(u.rent)
                        + "/день§f. Внутри — что угодно: жильё, склад, любое заведение (прибыль +50%). Не платите аренду "
                        + Mall.EVICT_DAYS + " дня подряд — выселят.");
                p.sendMessage(Component.literal("§d  ").append(button("§a§l[Купить за " + Finance.money(u.price) + "]", "/reallife mall buy" + cmd)), p.getUUID());
            }
            return;
        }
        LegalBusiness b = m.business(data, u);
        if (id.equals(u.tenant)) {
            BankService.msg(p, TC + "Это ваше помещение" + (owner ? " (без аренды — ТЦ ваш)" : ", аренда §e" + Finance.money(u.rent) + "/день"
                    + (u.unpaid > 0 ? " §cдолг " + u.unpaid + " дн." : "")) + "§f. " + (b == null ? "Заведения нет — можно открыть любое лицензией." : "Здесь «" + b.type.title + "»."));
            if (b == null) p.sendMessage(Component.literal("§d  ").append(button("§c[Освободить помещение]", "/reallife mall leave" + cmd)), p.getUUID());
        } else {
            BankService.msg(p, TC + "Занято: §e" + u.tenantName + "§f" + (b == null ? "" : " — «" + b.type.title + "»") + ".");
        }
    }

    /** Покупка помещения другим игроком (деньги — владельцу ТЦ). null — успех, иначе причина. */
    
    public static String buyCore(EconomyData data, Mall m, Mall.Unit u, Account buyer, String buyerName, long today) {
        if (!u.free()) return "помещение уже занято";
        if (buyer.id.equals(m.owner)) return "это ваш ТЦ — займите помещение бесплатно";
        if (buyer.balance < u.price) return "не хватает денег на счёте: нужно " + Finance.money(u.price);
        buyer.balance -= u.price;
        Account owner = m.owner == null ? null : data.getOrCreate(m.owner);
        if (owner != null) {
            owner.balance += u.price;
            owner.peakBalance = Math.max(owner.peakBalance, owner.balance);
        }
        assign(m, u, buyer.id, buyerName, today);
        data.setDirty();
        return null;
    }

    public static void assign(Mall m, Mall.Unit u, UUID tenant, String name, long today) {
        u.tenant = tenant;
        u.tenantName = name == null ? "" : name;
        u.unpaid = 0;
        u.since = today;
    }

    public static void buy(ServerPlayer p, int mallId, int idx) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Mall m = data.mallById(mallId);
        Mall.Unit u = m == null ? null : m.unit(idx);
        if (u == null) return;
        Account a = data.get(p);
        if (a.captivity != Account.FREE) return;
        if (p.distanceToSqr(u.entrance.getX(), u.entrance.getY(), u.entrance.getZ()) > 64 * 64) {
            BankService.msg(p, TC + "§cПодойдите к помещению — покупку оформляют на месте.");
            return;
        }
        String why = buyCore(data, m, u, a, p.getGameProfile().name(), BankService.today(server));
        if (why != null) {
            BankService.msg(p, TC + "§cНе вышло: §f" + why + ".");
            return;
        }
        BankService.sync(p);
        updateSign(p.level(), data, m, u);
        BankService.msg(p, TC + "§a§lПомещение №" + (u.idx + 1) + " ваше! §r§fСписано §c" + Finance.money(u.price)
                + "§f. Аренда §e" + Finance.money(u.rent) + "/день§f спишется утром со счёта. Стройте внутри что угодно; заведение — ПКМ лицензией по полу помещения.");
        ServerPlayer op = m.owner == null ? null : server.getPlayerList().getPlayer(m.owner);
        if (op != null) BankService.msg(op, TC + "§a" + p.getGameProfile().name() + " купил помещение №" + (u.idx + 1) + " в вашем ТЦ: +"
                + Finance.money(u.price) + "§f, дальше аренда " + Finance.money(u.rent) + "/день.");
    }

    public static void take(ServerPlayer p, int mallId, int idx) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Mall m = data.mallById(mallId);
        Mall.Unit u = m == null ? null : m.unit(idx);
        if (u == null) return;
        if (!p.getUUID().equals(m.owner)) {
            BankService.msg(p, TC + "§cЗанять бесплатно может только владелец ТЦ.");
            return;
        }
        if (!u.free()) {
            BankService.msg(p, TC + "§cПомещение уже занято.");
            return;
        }
        assign(m, u, p.getUUID(), p.getGameProfile().name(), BankService.today(server));
        data.setDirty();
        updateSign(p.level(), data, m, u);
        BankService.msg(p, TC + "§aПомещение №" + (u.idx + 1) + " теперь ваше (без аренды). §fЖильё, склад или любое заведение — ПКМ лицензией по полу помещения.");
    }

    public static void leave(ServerPlayer p, int mallId, int idx) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Mall m = data.mallById(mallId);
        Mall.Unit u = m == null ? null : m.unit(idx);
        if (u == null || !p.getUUID().equals(u.tenant)) return;
        if (m.business(data, u) != null) {
            BankService.msg(p, TC + "§cСначала закройте заведение в помещении (сломайте главную кассу).");
            return;
        }
        u.tenant = null;
        u.tenantName = "";
        u.unpaid = 0;
        data.setDirty();
        updateSign(p.level(), data, m, u);
        BankService.msg(p, TC + "Помещение №" + (u.idx + 1) + " освобождено" + (p.getUUID().equals(m.owner) ? "." : ", аренда больше не списывается. Деньги за покупку не возвращаются."));
    }

    /** Открыть заведение в помещении (без проверок и денег). Используется и самотестом. */
    public static LegalBusiness openInUnit(EconomyData data, Level w, Mall m, Mall.Unit u, BlockPos counter, Direction facing, BizType type, UUID owner, String ownerName) {
        LegalBusiness b = LegalBiz.register(data, w, counter, facing, type, owner, ownerName);
        b.mallId = m.id;
        b.mallUnit = u.idx;
        b.setZone(u.min, u.max);
        data.setDirty();
        updateSign(w, data, m, u);
        return b;
    }

    // ------------------------------------------------------------------ день

    public static void processDay(MinecraftServer server, EconomyData data) {
        ServerLevel ow = EconomyData.overworld(server);
        long today = BankService.today(server);
        for (Mall m : data.malls) {
            Account owner = m.owner == null ? null : data.getOrCreate(m.owner);
            double income = 0;
            int bots = 0;
            for (Mall.Unit u : m.units) {
                if (u.rival != 0) {
                    income += u.rent;
                    bots++;
                    continue;
                }
                if (u.tenant == null || u.tenant.equals(m.owner)) continue;
                Account t = data.getOrCreate(u.tenant);
                ServerPlayer tp = server.getPlayerList().getPlayer(u.tenant);
                if (t.balance >= u.rent) {
                    t.balance -= u.rent;
                    income += u.rent;
                    u.unpaid = 0;
                    if (tp != null) BankService.msg(tp, TC + "Аренда помещения №" + (u.idx + 1) + " в ТЦ «" + m.ownerName + "»: §c−" + Finance.money(u.rent));
                    continue;
                }
                u.unpaid++;
                if (u.unpaid >= Mall.EVICT_DAYS) {
                    evict(server, data, m, u, tp);
                } else if (tp != null) {
                    BankService.msg(tp, TC + "§cНе хватило денег на аренду помещения №" + (u.idx + 1) + " (" + Finance.money(u.rent) + "). Долг "
                            + u.unpaid + " из " + Mall.EVICT_DAYS + " дн. — потом выселение и закрытие заведения!");
                }
            }
            if (owner != null && income > 0) {
                owner.balance += income;
                owner.peakBalance = Math.max(owner.peakBalance, owner.balance);
                ServerPlayer op = server.getPlayerList().getPlayer(m.owner);
                if (op != null) BankService.msg(op, TC + "Аренда за день в вашем ТЦ: §a+" + Finance.money(income)
                        + (bots > 0 ? " §7(в т.ч. конкуренты: " + bots + ")" : ""));
            }
        }
        data.setDirty();
    }

    static void evict(MinecraftServer server, EconomyData data, Mall m, Mall.Unit u, ServerPlayer tp) {
        LegalBusiness b = m.business(data, u);
        String name = u.tenantName;
        if (b != null) LegalBiz.close(server, data, b, tp);
        u.tenant = null;
        u.tenantName = "";
        u.unpaid = 0;
        updateSign(server, data, m, u);
        if (tp != null) BankService.msg(tp, TC + "§c§lВас выселили из помещения №" + (u.idx + 1) + " за долг по аренде."
                + (b != null ? " §r§cЗаведение закрыто, лицензия возвращена." : ""));
        ServerPlayer op = m.owner == null ? null : server.getPlayerList().getPlayer(m.owner);
        if (op != null) BankService.msg(op, TC + "Арендатор " + name + " выселен из помещения №" + (u.idx + 1) + " за неуплату. Помещение снова продаётся.");
    }

    // ------------------------------------------------------------------ телефон

    public static ListTag phoneList(EconomyData data, UUID player) {
        ListTag l = new ListTag();
        for (Mall m : data.malls) {
            boolean own = player.equals(m.owner);
            if (own) {
                CompoundTag t = new CompoundTag();
                t.putBoolean("own", true);
                t.putInt("id", m.id);
                BlockPos c = m.center();
                t.putInt("x", c.getX());
                t.putInt("y", c.getY());
                t.putInt("z", c.getZ());
                t.putInt("used", m.used());
                t.putInt("total", m.units.size());
                int rent = 0, bots = 0, free = 0;
                for (Mall.Unit u : m.units) {
                    if (u.rival != 0) { bots++; rent += u.rent; }
                    else if (u.tenant != null && !u.tenant.equals(m.owner)) rent += u.rent;
                    else if (u.tenant == null) free++;
                }
                t.putInt("rent", rent);
                t.putInt("bots", bots);
                t.putInt("free", free);
                l.add(t);
            }
            for (Mall.Unit u : m.units) {
                if (!player.equals(u.tenant) || own) continue;
                CompoundTag t = new CompoundTag();
                t.putBoolean("own", false);
                t.putInt("id", m.id);
                t.putString("mall", m.ownerName);
                t.putInt("unit", u.idx + 1);
                t.putString("label", u.label() + " " + u.width + "×" + u.depth);
                t.putInt("rent", u.rent);
                t.putInt("unpaid", u.unpaid);
                LegalBusiness b = m.business(data, u);
                t.putString("biz", b == null ? "" : BizBuild.shortTitle(b.type));
                l.add(t);
            }
        }
        return l;
    }
}
