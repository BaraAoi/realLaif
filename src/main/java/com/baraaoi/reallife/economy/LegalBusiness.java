package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.util.Nbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;

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
    /** v1.9: опечатано (СЭС, миграционная служба) — не работает в дни &lt; sealedUntil. */
    public long sealedUntil;
    public String sealReason = "";
    /** v1.9: документы работников (медкнижки + патенты) действуют до этого дня и покрывают docsWorkers человек. */
    public long docsUntil;
    public int docsWorkers;
    /** v1.10: заведение стоит на месте в торговом центре (id ТЦ, -1 — нет). */
    public int mallId = -1;
    /** v1.11: помещение ТЦ; у заведений в помещении зона = само помещение (а не ±8 от кассы). */
    public int mallUnit = -1;
    public boolean customZone;

    public void setZone(BlockPos zmin, BlockPos zmax) {
        min = zmin.immutable();
        max = zmax.immutable();
        customZone = true;
    }

    public boolean inMall() {
        return mallId >= 0;
    }

    public void setCounter(BlockPos c) {
        counter = c.immutable();
        min = c.offset(-R_XZ, -R_DOWN, -R_XZ);
        max = c.offset(R_XZ, R_UP, R_XZ);
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

    public CompoundTag write() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        if (owner != null) Nbt.putUUID(t, "owner", owner);
        t.putString("ownerName", ownerName);
        t.putInt("type", type.ordinal());
        t.putLong("counter", counter.asLong());
        ListTag wl = new ListTag();
        for (UUID u : workers) {
            CompoundTag w = new CompoundTag();
            Nbt.putUUID(w, "u", u);
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
        t.putLong("sealedUntil", sealedUntil);
        t.putString("sealReason", sealReason);
        t.putLong("docsUntil", docsUntil);
        t.putInt("docsWorkers", docsWorkers);
        t.putInt("mallId", mallId);
        t.putInt("mallUnit", mallUnit);
        if (customZone) {
            t.putLong("zmin", min.asLong());
            t.putLong("zmax", max.asLong());
        }
        return t;
    }

    public static LegalBusiness read(CompoundTag t) {
        LegalBusiness b = new LegalBusiness();
        b.id = t.getIntOr("id", 0);
        b.owner = Nbt.hasUUID(t, "owner") ? Nbt.getUUID(t, "owner") : null;
        b.ownerName = t.getStringOr("ownerName", "");
        BizType bt = BizType.byId(t.getIntOr("type", 0));
        b.type = bt == null ? BizType.SHAWARMA : bt;
        b.setCounter(BlockPos.of(t.getLongOr("counter", 0L)));
        if (t.contains("workers")) {
            ListTag wl = t.getListOrEmpty("workers");
            for (int i = 0; i < wl.size(); i++) {
                CompoundTag w = wl.getCompoundOrEmpty(i);
                if (Nbt.hasUUID(w, "u")) b.workers.add(Nbt.getUUID(w, "u"));
            }
        } else if (t.getBooleanOr("worker", false) && Nbt.hasUUID(t, "workerId")) {
            b.workers.add(Nbt.getUUID(t, "workerId")); // сохранение v5
        }
        b.lastProfit = t.getDoubleOr("lastProfit", 0.0);
        b.lastNote = t.getStringOr("lastNote", "");
        b.totalProfit = t.getDoubleOr("totalProfit", 0.0);
        b.days = t.getIntOr("days", 0);
        b.launderDay = t.contains("launderDay") ? t.getLongOr("launderDay", 0L) : -1;
        b.launderedToday = t.getDoubleOr("launderedToday", 0.0);
        b.cacheSets = t.getIntOr("cacheSets", 0);
        b.cacheStock = t.getIntOr("cacheStock", 0);
        b.sealedUntil = t.getLongOr("sealedUntil", 0L);
        b.sealReason = t.getStringOr("sealReason", "");
        b.docsUntil = t.getLongOr("docsUntil", 0L);
        b.docsWorkers = t.getIntOr("docsWorkers", 0);
        b.mallId = t.contains("mallId") ? t.getIntOr("mallId", 0) : -1;
        b.mallUnit = t.contains("mallUnit") ? t.getIntOr("mallUnit", 0) : -1;
        if (t.contains("zmin")) b.setZone(BlockPos.of(t.getLongOr("zmin", 0L)), BlockPos.of(t.getLongOr("zmax", 0L)));
        return b;
    }
}
