package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.util.Nbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * v1.11: торговый центр игрока — атриум с фонтаном и помещения разного размера и цены.
 * Помещение покупают (деньги — владельцу ТЦ) и платят аренду каждый день. Внутри своего помещения можно строить
 * что угодно: жильё, склад или открыть любое заведение. Заведения в ТЦ: прибыль +50%, безопасный отмыв 25%.
 */
public class Mall {
    /** Цена разрешения на строительство ТЦ (стройка включена). */
    public static final int PRICE = 150_000;
    /** Прибыль заведений в ТЦ. */
    public static final double REVENUE_BONUS = 1.5;
    /** Безопасная доля отмыва в ТЦ (вне ТЦ — Config.LAUNDER_SAFE_SHARE, по умолчанию 15%). */
    public static final double SAFE_SHARE = 0.25;
    /** Сколько дней можно не платить аренду до выселения. */
    public static final int EVICT_DAYS = 3;

    /** Габариты зала в локальных координатах (x — влево от входа, z — вглубь). Стены до y=H−1, крыша на H. */
    public static final int X1 = -36, X2 = 36, Z1 = 0, Z2 = 56, H = 11;

    /** Сторона, где у помещения витрина и вход. */
    public static final int FRONT_XNEG = 0, FRONT_XPOS = 1, FRONT_ZNEG = 2;

    /** Планировка помещений: x1, z1, x2, z2 (внутренняя площадь), сторона входа. */
    public static final int[][] LAYOUT = {
            {21, 1, 35, 17, FRONT_XNEG},   // 15×17 большое
            {25, 19, 35, 31, FRONT_XNEG},  // 11×13
            {27, 33, 35, 41, FRONT_XNEG},  // 9×9
            {23, 43, 35, 55, FRONT_XNEG},  // 13×13
            {-35, 1, -25, 11, FRONT_XPOS}, // 11×11
            {-35, 13, -27, 21, FRONT_XPOS},// 9×9
            {-35, 23, -19, 39, FRONT_XPOS},// 17×17 самое большое
            {-35, 41, -27, 49, FRONT_XPOS},// 9×9
            {-35, 51, -29, 55, FRONT_XPOS},// 7×5 киоск
            {-17, 45, -5, 55, FRONT_ZNEG}, // 13×11
            {-3, 47, 5, 55, FRONT_ZNEG},   // 9×9
            {7, 45, 17, 55, FRONT_ZNEG}};  // 11×11

    public static int area(int[] l) {
        return (l[2] - l[0] + 1) * (l[3] - l[1] + 1);
    }

    /** Цена покупки помещения (деньги уходят владельцу ТЦ). */
    public static int unitPrice(int area) {
        return Math.max(1000, (int) Math.round(area * 80 / 500.0) * 500);
    }

    /** Аренда в день. */
    public static int unitRent(int area) {
        return Math.max(50, (int) Math.round(area * 2.0 / 10.0) * 10);
    }

    public static String sizeLabel(int area) {
        return area < 50 ? "Киоск" : area < 100 ? "Малое" : area < 170 ? "Среднее" : "Большое";
    }

    public static final class Unit {
        public int idx;
        /** Внутренняя площадь: пол (y) .. под крышей. */
        public BlockPos min = BlockPos.ZERO, max = BlockPos.ZERO;
        /** Проём входа (нижний блок) и направление внутрь помещения. */
        public BlockPos entrance = BlockPos.ZERO;
        public Direction facing = Direction.SOUTH;
        /** Табличка над входом. */
        public BlockPos sign = BlockPos.ZERO;
        public int width, depth;
        public int price, rent;
        public UUID tenant;
        public String tenantName = "";
        /** Помещение занял бот-конкурент (id), 0 — нет. */
        public int rival;
        public int unpaid;
        public long since;

        public int area() {
            return width * depth;
        }

        public String label() {
            return sizeLabel(area());
        }

        public boolean free() {
            return tenant == null && rival == 0;
        }

        public boolean contains(BlockPos p) {
            return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY()
                    && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
        }

        CompoundTag write() {
            CompoundTag t = new CompoundTag();
            t.putInt("i", idx);
            t.putLong("min", min.asLong());
            t.putLong("max", max.asLong());
            t.putLong("door", entrance.asLong());
            t.putInt("f", facing.get2DDataValue());
            t.putLong("sign", sign.asLong());
            t.putInt("w", width);
            t.putInt("d", depth);
            t.putInt("price", price);
            t.putInt("rent", rent);
            if (tenant != null) Nbt.putUUID(t, "tenant", tenant);
            t.putString("tenantName", tenantName);
            t.putInt("rival", rival);
            t.putInt("unpaid", unpaid);
            t.putLong("since", since);
            return t;
        }

        static Unit read(CompoundTag t) {
            Unit u = new Unit();
            u.idx = t.getIntOr("i", 0);
            u.min = BlockPos.of(t.getLongOr("min", 0L));
            u.max = BlockPos.of(t.getLongOr("max", 0L));
            u.entrance = BlockPos.of(t.getLongOr("door", 0L));
            u.facing = Direction.from2DDataValue(t.getIntOr("f", 0));
            u.sign = BlockPos.of(t.getLongOr("sign", 0L));
            u.width = t.getIntOr("w", 0);
            u.depth = t.getIntOr("d", 0);
            u.price = t.getIntOr("price", 0);
            u.rent = t.getIntOr("rent", 0);
            u.tenant = Nbt.hasUUID(t, "tenant") ? Nbt.getUUID(t, "tenant") : null;
            u.tenantName = t.getStringOr("tenantName", "");
            u.rival = t.getIntOr("rival", 0);
            u.unpaid = t.getIntOr("unpaid", 0);
            u.since = t.getLongOr("since", 0L);
            return u;
        }
    }

    public int id;
    public UUID owner;
    public String ownerName = "";
    public BlockPos min = BlockPos.ZERO, max = BlockPos.ZERO;
    public final List<Unit> units = new ArrayList<>();

    public boolean contains(BlockPos p, int grow) {
        return p.getX() >= min.getX() - grow && p.getX() <= max.getX() + grow
                && p.getY() >= min.getY() - grow && p.getY() <= max.getY() + grow
                && p.getZ() >= min.getZ() - grow && p.getZ() <= max.getZ() + grow;
    }

    public BlockPos center() {
        return new BlockPos((min.getX() + max.getX()) / 2, min.getY(), (min.getZ() + max.getZ()) / 2);
    }

    public Unit unitAt(BlockPos p) {
        for (Unit u : units) if (u.contains(p)) return u;
        return null;
    }

    public Unit unit(int idx) {
        return idx >= 0 && idx < units.size() ? units.get(idx) : null;
    }

    /** Заведение внутри помещения (или null). */
    public LegalBusiness business(EconomyData data, Unit u) {
        for (LegalBusiness b : data.businesses) if (b.mallId == id && u.contains(b.counter)) return b;
        return null;
    }

    public int used() {
        int n = 0;
        for (Unit u : units) if (!u.free()) n++;
        return n;
    }

    public CompoundTag write() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        if (owner != null) Nbt.putUUID(t, "owner", owner);
        t.putString("ownerName", ownerName);
        t.putLong("min", min.asLong());
        t.putLong("max", max.asLong());
        ListTag l = new ListTag();
        for (Unit u : units) l.add(u.write());
        t.put("units", l);
        return t;
    }

    public static Mall read(CompoundTag t) {
        Mall m = new Mall();
        m.id = t.getIntOr("id", 0);
        m.owner = Nbt.hasUUID(t, "owner") ? Nbt.getUUID(t, "owner") : null;
        m.ownerName = t.getStringOr("ownerName", "");
        m.min = BlockPos.of(t.getLongOr("min", 0L));
        m.max = BlockPos.of(t.getLongOr("max", 0L));
        if (t.contains("units")) {
            ListTag l = t.getListOrEmpty("units");
            for (int i = 0; i < l.size(); i++) m.units.add(Unit.read(l.getCompoundOrEmpty(i)));
        } else {
            // ТЦ из v1.10: площадки 15×15 вокруг размеченных мест — становятся помещениями владельца
            ListTag l = t.getListOrEmpty("slots");
            for (int i = 0; i < l.size(); i++) {
                BlockPos c = BlockPos.of(l.getCompoundOrEmpty(i).getLongOr("p", 0L));
                Unit u = new Unit();
                u.idx = i;
                u.min = c.offset(-7, -1, -7);
                u.max = c.offset(7, 8, 7);
                u.entrance = c;
                u.width = 15;
                u.depth = 15;
                u.price = unitPrice(u.area());
                u.rent = unitRent(u.area());
                u.tenant = m.owner;
                u.tenantName = m.ownerName;
                m.units.add(u);
            }
        }
        return m;
    }
}
