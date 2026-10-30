package com.baraaoi.reallife.economy;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Легальное заведение игрока v6: главная касса + «зона» вокруг неё, где игрок сам расставляет оборудование.
 * Хранится в {@link EconomyData#businesses}.
 */
public class LegalBusiness {
    /** Размер зоны вокруг главной кассы: ±R_XZ по горизонтали, −R_DOWN..+R_UP по высоте. */
    public static final int R_XZ = 8, R_DOWN = 3, R_UP = 6;

    public int id;
    public UUID owner;
    public String ownerName = "";
    public BizType type = BizType.SHAWARMA;
    /** Главная касса (по ней зарегистрировано заведение). */
    public BlockPos counter = BlockPos.ZERO;
    public BlockPos min = BlockPos.ZERO;
    public BlockPos max = BlockPos.ZERO;
    /** Нанятые работники. */
    public final List<UUID> workers = new ArrayList<>();
    public double lastProfit;
    public String lastNote = "";
    public double totalProfit;
    public int days;
    public long launderDay = -1;
    public double launderedToday;
    /** Кэш последней проверки оборудования (если чанк не загружен). */
    public int cacheSets;
    public int cacheStock;
    /** Сколько покупателей-NPC дошло до кассы сегодня (косметика, не сохраняется). */
    public transient int customersToday;

    public void setCounter(BlockPos c) {
        counter = c.toImmutable();
        min = c.add(-R_XZ, -R_DOWN, -R_XZ);
        max = c.add(R_XZ, R_UP, R_XZ);
    }

    public boolean contains(BlockPos p, int grow) {
        return p.getX() >= min.getX() - grow && p.getX() <= max.getX() + grow
                && p.getY() >= min.getY() - grow && p.getY() <= max.getY() + grow
                && p.getZ() >= min.getZ() - grow && p.getZ() <= max.getZ() + grow;
    }

    /** Пересекаются ли зоны двух заведений. */
    public static boolean zonesOverlap(BlockPos c1, BlockPos c2) {
        return Math.abs(c1.getX() - c2.getX()) <= 2 * R_XZ && Math.abs(c1.getZ() - c2.getZ()) <= 2 * R_XZ
                && Math.abs(c1.getY() - c2.getY()) <= R_DOWN + R_UP;
    }

    /** Сколько уже отмыто сегодня (сбрасывается с новым днём). */
    public double launderedOn(long today) {
        return launderDay == today ? launderedToday : 0;
    }

    public CompoundNBT write() {
        CompoundNBT t = new CompoundNBT();
        t.putInt("id", id);
        if (owner != null) t.putUniqueId("owner", owner);
        t.putString("ownerName", ownerName);
        t.putInt("type", type.ordinal());
        t.putLong("counter", counter.toLong());
        ListNBT wl = new ListNBT();
        for (UUID u : workers) {
            CompoundNBT w = new CompoundNBT();
            w.putUniqueId("u", u);
            wl.add(w);
        }
        t.put("workers", wl);
        t.putDouble("lastProfit", lastProfit);
        t.putString("lastNote", lastNote);
        t.putDouble("totalProfit", totalProfit);
        t.putInt("days", days);
        t.putLong("launderDay", launderDay);
        t.putDouble("launderedToday", launderedToday);
        t.putInt("cacheSets", cacheSets);
        t.putInt("cacheStock", cacheStock);
        return t;
    }

    public static LegalBusiness read(CompoundNBT t) {
        LegalBusiness b = new LegalBusiness();
        b.id = t.getInt("id");
        b.owner = t.hasUniqueId("owner") ? t.getUniqueId("owner") : null;
        b.ownerName = t.getString("ownerName");
        BizType bt = BizType.byId(t.getInt("type"));
        b.type = bt == null ? BizType.SHAWARMA : bt;
        b.setCounter(BlockPos.fromLong(t.getLong("counter")));
        if (t.contains("workers")) {
            ListNBT wl = t.getList("workers", 10);
            for (int i = 0; i < wl.size(); i++) {
                CompoundNBT w = wl.getCompound(i);
                if (w.hasUniqueId("u")) b.workers.add(w.getUniqueId("u"));
            }
        } else if (t.getBoolean("worker") && t.hasUniqueId("workerId")) {
            b.workers.add(t.getUniqueId("workerId")); // сохранение v5
        }
        b.lastProfit = t.getDouble("lastProfit");
        b.lastNote = t.getString("lastNote");
        b.totalProfit = t.getDouble("totalProfit");
        b.days = t.getInt("days");
        b.launderDay = t.contains("launderDay") ? t.getLong("launderDay") : -1;
        b.launderedToday = t.getDouble("launderedToday");
        b.cacheSets = t.getInt("cacheSets");
        b.cacheStock = t.getInt("cacheStock");
        return b;
    }
}
