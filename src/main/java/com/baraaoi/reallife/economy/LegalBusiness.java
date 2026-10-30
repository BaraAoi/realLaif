package com.baraaoi.reallife.economy;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.UUID;

/** Построенное легальное заведение игрока. Хранится в {@link EconomyData#businesses}. */
public class LegalBusiness {
    public int id;
    public UUID owner;
    public String ownerName = "";
    public BizType type = BizType.SHAWARMA;
    /** Касса заведения (блок). */
    public BlockPos counter = BlockPos.ZERO;
    /** Где стоит работник. */
    public BlockPos workerPos = BlockPos.ZERO;
    public BlockPos min = BlockPos.ZERO;
    public BlockPos max = BlockPos.ZERO;
    public boolean worker;
    @Nullable
    public UUID workerId;
    public double lastProfit;
    public String lastNote = "";
    public double totalProfit;
    public int days;
    public long launderDay = -1;
    public double launderedToday;

    public boolean contains(BlockPos p, int grow) {
        return p.getX() >= min.getX() - grow && p.getX() <= max.getX() + grow
                && p.getY() >= min.getY() - grow && p.getY() <= max.getY() + grow
                && p.getZ() >= min.getZ() - grow && p.getZ() <= max.getZ() + grow;
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
        t.putLong("workerPos", workerPos.toLong());
        t.putLong("min", min.toLong());
        t.putLong("max", max.toLong());
        t.putBoolean("worker", worker);
        if (workerId != null) t.putUniqueId("workerId", workerId);
        t.putDouble("lastProfit", lastProfit);
        t.putString("lastNote", lastNote);
        t.putDouble("totalProfit", totalProfit);
        t.putInt("days", days);
        t.putLong("launderDay", launderDay);
        t.putDouble("launderedToday", launderedToday);
        return t;
    }

    public static LegalBusiness read(CompoundNBT t) {
        LegalBusiness b = new LegalBusiness();
        b.id = t.getInt("id");
        b.owner = t.hasUniqueId("owner") ? t.getUniqueId("owner") : null;
        b.ownerName = t.getString("ownerName");
        BizType bt = BizType.byId(t.getInt("type"));
        b.type = bt == null ? BizType.SHAWARMA : bt;
        b.counter = BlockPos.fromLong(t.getLong("counter"));
        b.workerPos = BlockPos.fromLong(t.getLong("workerPos"));
        b.min = BlockPos.fromLong(t.getLong("min"));
        b.max = BlockPos.fromLong(t.getLong("max"));
        b.worker = t.getBoolean("worker");
        b.workerId = t.hasUniqueId("workerId") ? t.getUniqueId("workerId") : null;
        b.lastProfit = t.getDouble("lastProfit");
        b.lastNote = t.getString("lastNote");
        b.totalProfit = t.getDouble("totalProfit");
        b.days = t.getInt("days");
        b.launderDay = t.contains("launderDay") ? t.getLong("launderDay") : -1;
        b.launderedToday = t.getDouble("launderedToday");
        return b;
    }
}
