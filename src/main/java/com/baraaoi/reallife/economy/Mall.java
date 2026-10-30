package com.baraaoi.reallife.economy;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * v1.10: торговый центр игрока — огромный пустой зал с размеченными местами под определённые виды бизнеса.
 * Заведения на этих местах приносят на 50% больше и могут отмывать до 25% прибыли без риска.
 */
public class Mall {
    /** Цена разрешения на строительство ТЦ (стройка включена). */
    public static final int PRICE = 150_000;
    /** Прибыль заведений в ТЦ. */
    public static final double REVENUE_BONUS = 1.5;
    /** Безопасная доля отмыва в ТЦ (вне ТЦ — Config.LAUNDER_SAFE_SHARE, по умолчанию 15%). */
    public static final double SAFE_SHARE = 0.25;
    /** На каком расстоянии от центра места можно кликнуть лицензией (касса встанет точно в центр). */
    public static final int SNAP = 3;
    /** Разметка мест: виды бизнеса (по 2 каждого). */
    public static final BizType[] LAYOUT = {
            BizType.SHAWARMA, BizType.MINIMARKET, BizType.PAWNSHOP, BizType.ELECTRONICS,
            BizType.ELECTRONICS, BizType.PAWNSHOP, BizType.MINIMARKET, BizType.SHAWARMA};
    /** Локальные координаты центров мест (x — влево от входа, z — вглубь). */
    public static final int[] SLOT_X = {-26, -9, 8, 25, -26, -9, 8, 25};
    public static final int[] SLOT_Z = {10, 10, 10, 10, 34, 34, 34, 34};
    /** Габариты зала в локальных координатах. */
    public static final int X1 = -35, X2 = 35, Z1 = 0, Z2 = 44, H = 10;

    public int id;
    public UUID owner;
    public String ownerName = "";
    public BlockPos min = BlockPos.ZERO, max = BlockPos.ZERO;
    /** Центры мест (там встаёт главная касса) и их виды. */
    public final List<BlockPos> slots = new ArrayList<>();
    public final List<BizType> types = new ArrayList<>();

    public boolean contains(BlockPos p, int grow) {
        return p.getX() >= min.getX() - grow && p.getX() <= max.getX() + grow
                && p.getY() >= min.getY() - grow && p.getY() <= max.getY() + grow
                && p.getZ() >= min.getZ() - grow && p.getZ() <= max.getZ() + grow;
    }

    /** Ближайшее место в пределах snap блоков по горизонтали (и ±2 по высоте), -1 — нет. */
    public int slotNear(BlockPos p, int snap) {
        int best = -1;
        int bestD = Integer.MAX_VALUE;
        for (int i = 0; i < slots.size(); i++) {
            BlockPos s = slots.get(i);
            int dx = Math.abs(s.getX() - p.getX()), dz = Math.abs(s.getZ() - p.getZ()), dy = Math.abs(s.getY() - p.getY());
            if (dx > snap || dz > snap || dy > 2) continue;
            int d = dx + dz + dy;
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    /** Заведение, занявшее место idx (или null). */
    public LegalBusiness occupant(EconomyData data, int idx) {
        BlockPos s = slots.get(idx);
        for (LegalBusiness b : data.businesses) if (b.counter.equals(s)) return b;
        return null;
    }

    public int used(EconomyData data) {
        int n = 0;
        for (int i = 0; i < slots.size(); i++) if (occupant(data, i) != null) n++;
        return n;
    }

    public CompoundNBT write() {
        CompoundNBT t = new CompoundNBT();
        t.putInt("id", id);
        if (owner != null) t.putUniqueId("owner", owner);
        t.putString("ownerName", ownerName);
        t.putLong("min", min.toLong());
        t.putLong("max", max.toLong());
        ListNBT l = new ListNBT();
        for (int i = 0; i < slots.size(); i++) {
            CompoundNBT s = new CompoundNBT();
            s.putLong("p", slots.get(i).toLong());
            s.putInt("t", types.get(i).ordinal());
            l.add(s);
        }
        t.put("slots", l);
        return t;
    }

    public static Mall read(CompoundNBT t) {
        Mall m = new Mall();
        m.id = t.getInt("id");
        m.owner = t.hasUniqueId("owner") ? t.getUniqueId("owner") : null;
        m.ownerName = t.getString("ownerName");
        m.min = BlockPos.fromLong(t.getLong("min"));
        m.max = BlockPos.fromLong(t.getLong("max"));
        ListNBT l = t.getList("slots", 10);
        for (int i = 0; i < l.size(); i++) {
            CompoundNBT s = l.getCompound(i);
            BizType bt = BizType.byId(s.getInt("t"));
            m.slots.add(BlockPos.fromLong(s.getLong("p")));
            m.types.add(bt == null ? BizType.SHAWARMA : bt);
        }
        return m;
    }
}
