package com.baraaoi.reallife.economy;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Зарегистрированная постройка мода (банк, МФО, казино): габарит, защита, выход, места кассиров.
 */
public class Structure {
    public static final String BANK = "bank";
    public static final String MFO = "mfo";
    public static final String CASINO = "casino";

    public String type = BANK;
    public BlockPos min = BlockPos.ZERO;
    public BlockPos max = BlockPos.ZERO;
    /** Неразрушимая (городская) постройка. */
    public boolean protect;
    /** Куда выводят (казино) / где стоять снаружи. */
    public BlockPos exit = BlockPos.ZERO;
    public final List<BlockPos> cashiers = new ArrayList<>();
    /** Когда можно снова посадить убитых кассиров. */
    public long cashierRespawn;

    public boolean contains(BlockPos p, int grow) {
        return p.getX() >= min.getX() - grow && p.getX() <= max.getX() + grow
                && p.getY() >= min.getY() - grow && p.getY() <= max.getY() + grow
                && p.getZ() >= min.getZ() - grow && p.getZ() <= max.getZ() + grow;
    }

    public boolean contains(double x, double y, double z, int grow) {
        return contains(new BlockPos(x, y, z), grow);
    }

    /** Пересекается ли с коробкой (по X/Z, с запасом grow; по Y — с запасом 2). */
    public boolean intersects(BlockPos a, BlockPos b, int grow) {
        return a.getX() <= max.getX() + grow && b.getX() >= min.getX() - grow
                && a.getZ() <= max.getZ() + grow && b.getZ() >= min.getZ() - grow
                && a.getY() <= max.getY() + 2 && b.getY() >= min.getY() - 2;
    }

    public CompoundNBT write() {
        CompoundNBT t = new CompoundNBT();
        t.putString("type", type);
        t.putLong("min", min.toLong());
        t.putLong("max", max.toLong());
        t.putBoolean("protect", protect);
        t.putLong("exit", exit.toLong());
        long[] c = new long[cashiers.size()];
        for (int i = 0; i < c.length; i++) c[i] = cashiers.get(i).toLong();
        t.putLongArray("cashiers", c);
        t.putLong("cashierRespawn", cashierRespawn);
        return t;
    }

    public static Structure read(CompoundNBT t) {
        Structure s = new Structure();
        s.type = t.getString("type");
        s.min = BlockPos.fromLong(t.getLong("min"));
        s.max = BlockPos.fromLong(t.getLong("max"));
        s.protect = t.getBoolean("protect");
        s.exit = BlockPos.fromLong(t.getLong("exit"));
        for (long l : t.getLongArray("cashiers")) s.cashiers.add(BlockPos.fromLong(l));
        s.cashierRespawn = t.getLong("cashierRespawn");
        return s;
    }
}
