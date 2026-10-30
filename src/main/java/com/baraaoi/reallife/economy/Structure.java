package com.baraaoi.reallife.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

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

    public CompoundTag write() {
        CompoundTag t = new CompoundTag();
        t.putString("type", type);
        t.putLong("min", min.asLong());
        t.putLong("max", max.asLong());
        t.putBoolean("protect", protect);
        t.putLong("exit", exit.asLong());
        long[] c = new long[cashiers.size()];
        for (int i = 0; i < c.length; i++) c[i] = cashiers.get(i).asLong();
        t.putLongArray("cashiers", c);
        t.putLong("cashierRespawn", cashierRespawn);
        return t;
    }

    public static Structure read(CompoundTag t) {
        Structure s = new Structure();
        s.type = t.getStringOr("type", "");
        s.min = BlockPos.of(t.getLongOr("min", 0L));
        s.max = BlockPos.of(t.getLongOr("max", 0L));
        s.protect = t.getBooleanOr("protect", false);
        s.exit = BlockPos.of(t.getLongOr("exit", 0L));
        for (long l : t.getLongArray("cashiers").orElse(new long[0])) s.cashiers.add(BlockPos.of(l));
        s.cashierRespawn = t.getLongOr("cashierRespawn", 0L);
        return s;
    }
}
