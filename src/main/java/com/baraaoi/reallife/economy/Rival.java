package com.baraaoi.reallife.economy;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;

/** v1.11: бот-конкурент — ларёк рядом с заведением игрока или арендатор свободного помещения в ТЦ. */
public class Rival {
    public int id;
    public BizType type = BizType.SHAWARMA;
    public String name = "";
    /** Вывеска (ПКМ — выкупить) и габариты постройки. */
    public BlockPos sign = BlockPos.ZERO;
    public BlockPos min = BlockPos.ZERO, max = BlockPos.ZERO;
    /** В каком ТЦ и помещении (-1 — ларёк на улице). */
    public int mallId = -1, unit = -1;
    /** День, когда «разорится» и закроется сам. */
    public long until;
    /** Что стояло на месте постройки — вернётся, когда конкурент закроется. */
    public ListNBT snapshot = new ListNBT();

    public boolean inMall() {
        return mallId >= 0;
    }

    public BlockPos center() {
        return new BlockPos((min.getX() + max.getX()) / 2, min.getY(), (min.getZ() + max.getZ()) / 2);
    }

    public CompoundNBT write() {
        CompoundNBT t = new CompoundNBT();
        t.putInt("id", id);
        t.putInt("type", type.ordinal());
        t.putString("name", name);
        t.putLong("sign", sign.toLong());
        t.putLong("min", min.toLong());
        t.putLong("max", max.toLong());
        t.putInt("mall", mallId);
        t.putInt("unit", unit);
        t.putLong("until", until);
        t.put("snap", snapshot);
        return t;
    }

    public static Rival read(CompoundNBT t) {
        Rival r = new Rival();
        r.id = t.getInt("id");
        BizType bt = BizType.byId(t.getInt("type"));
        r.type = bt == null ? BizType.SHAWARMA : bt;
        r.name = t.getString("name");
        r.sign = BlockPos.fromLong(t.getLong("sign"));
        r.min = BlockPos.fromLong(t.getLong("min"));
        r.max = BlockPos.fromLong(t.getLong("max"));
        r.mallId = t.getInt("mall");
        r.unit = t.getInt("unit");
        r.until = t.getLong("until");
        r.snapshot = t.getList("snap", 10);
        return r;
    }
}
