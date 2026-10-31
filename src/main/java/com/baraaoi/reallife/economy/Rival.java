package com.baraaoi.reallife.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;

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
    public ListTag snapshot = new ListTag();

    public boolean inMall() {
        return mallId >= 0;
    }

    public BlockPos center() {
        return new BlockPos((min.getX() + max.getX()) / 2, min.getY(), (min.getZ() + max.getZ()) / 2);
    }

    public CompoundTag write() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putInt("type", type.ordinal());
        t.putString("name", name);
        t.putLong("sign", sign.asLong());
        t.putLong("min", min.asLong());
        t.putLong("max", max.asLong());
        t.putInt("mall", mallId);
        t.putInt("unit", unit);
        t.putLong("until", until);
        t.put("snap", snapshot);
        return t;
    }

    public static Rival read(CompoundTag t) {
        Rival r = new Rival();
        r.id = t.getIntOr("id", 0);
        BizType bt = BizType.byId(t.getIntOr("type", 0));
        r.type = bt == null ? BizType.SHAWARMA : bt;
        r.name = t.getStringOr("name", "");
        r.sign = BlockPos.of(t.getLongOr("sign", 0L));
        r.min = BlockPos.of(t.getLongOr("min", 0L));
        r.max = BlockPos.of(t.getLongOr("max", 0L));
        r.mallId = t.getIntOr("mall", 0);
        r.unit = t.getIntOr("unit", 0);
        r.until = t.getLongOr("until", 0L);
        r.snapshot = t.getListOrEmpty("snap");
        return r;
    }
}
