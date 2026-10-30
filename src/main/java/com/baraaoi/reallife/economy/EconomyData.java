package com.baraaoi.reallife.economy;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Хранилище всех счетов, земли и построек мода (сохраняется вместе с миром, в overworld).
 */
public class EconomyData extends WorldSavedData {
    public static final String NAME = "reallife_economy";

    private final Map<UUID, Account> accounts = new HashMap<>();
    public long lastDay = -1;

    /** Старые постоянные тюрьма/подвал (версия 1.0) — будут убраны при запуске. */
    public boolean jailBuilt;
    public BlockPos jailPos = BlockPos.ZERO;
    public boolean pitBuilt;
    public BlockPos pitPos = BlockPos.ZERO;

    /** Банк и МФО на спавне уже построены. */
    public boolean spawnBuilt;
    /** Центры (уровень пола) всех контор МФО — под ними делается подвал. */
    public final List<BlockPos> mfoCenters = new ArrayList<>();

    /** Владельцы участков (чанков) в обычном мире. */
    public final Map<Long, UUID> landOwners = new HashMap<>();
    /** Кто сколько блоков поставил в каждом чанке: чанк -> игрок -> позиции. */
    public final Map<Long, Map<UUID, Set<Long>>> placed = new HashMap<>();

    public EconomyData() {
        super(NAME);
    }

    public static ServerWorld overworld(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD);
    }

    public static EconomyData get(MinecraftServer server) {
        return overworld(server).getSavedData().getOrCreate(EconomyData::new, NAME);
    }

    public Account get(PlayerEntity player) {
        Account a = accounts.computeIfAbsent(player.getUniqueID(), Account::new);
        a.name = player.getGameProfile().getName();
        if (a.createdDay < 0 && player.world != null) {
            a.createdDay = player.world.getDayTime() / 24000L;
        }
        return a;
    }

    public Account get(UUID id) {
        return accounts.get(id);
    }

    public Account getOrCreate(UUID id) {
        return accounts.computeIfAbsent(id, Account::new);
    }

    public Collection<Account> all() {
        return accounts.values();
    }

    public void reset(UUID id) {
        accounts.remove(id);
        markDirty();
    }

    // ------------------------------------------------------------ земля

    public static long chunkKey(BlockPos pos) {
        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public UUID owner(long chunk) {
        return landOwners.get(chunk);
    }

    public void claim(UUID player, long chunk) {
        landOwners.put(chunk, player);
        placed.remove(chunk);
        markDirty();
    }

    public int ownedCount(UUID player) {
        int n = 0;
        for (UUID u : landOwners.values()) if (u.equals(player)) n++;
        return n;
    }

    public int placedCount(long chunk, UUID player) {
        Map<UUID, Set<Long>> m = placed.get(chunk);
        if (m == null) return 0;
        Set<Long> s = m.get(player);
        return s == null ? 0 : s.size();
    }

    public int addPlaced(long chunk, UUID player, BlockPos pos) {
        Set<Long> s = placed.computeIfAbsent(chunk, k -> new HashMap<>()).computeIfAbsent(player, k -> new HashSet<>());
        s.add(pos.toLong());
        markDirty();
        return s.size();
    }

    /** @return true если блок был поставлен этим игроком. */
    public boolean removePlaced(long chunk, UUID player, BlockPos pos) {
        Map<UUID, Set<Long>> m = placed.get(chunk);
        if (m == null) return false;
        Set<Long> s = m.get(player);
        if (s == null) return false;
        boolean r = s.remove(pos.toLong());
        if (r) markDirty();
        return r;
    }

    public void clearPlaced(long chunk) {
        placed.remove(chunk);
        markDirty();
    }

    // ------------------------------------------------------------ NBT

    @Override
    public void read(CompoundNBT nbt) {
        accounts.clear();
        ListNBT list = nbt.getList("accounts", 10);
        for (INBT n : list) {
            Account a = Account.read((CompoundNBT) n);
            accounts.put(a.id, a);
        }
        lastDay = nbt.getLong("lastDay");
        jailBuilt = nbt.getBoolean("jailBuilt");
        jailPos = BlockPos.fromLong(nbt.getLong("jailPos"));
        pitBuilt = nbt.getBoolean("pitBuilt");
        pitPos = BlockPos.fromLong(nbt.getLong("pitPos"));
        spawnBuilt = nbt.getBoolean("spawnBuilt");
        mfoCenters.clear();
        for (long l : nbt.getLongArray("mfoCenters")) mfoCenters.add(BlockPos.fromLong(l));
        landOwners.clear();
        for (INBT n : nbt.getList("land", 10)) {
            CompoundNBT t = (CompoundNBT) n;
            landOwners.put(t.getLong("c"), t.getUniqueId("o"));
        }
        placed.clear();
        for (INBT n : nbt.getList("placed", 10)) {
            CompoundNBT t = (CompoundNBT) n;
            Set<Long> s = new HashSet<>();
            for (long l : t.getLongArray("p")) s.add(l);
            placed.computeIfAbsent(t.getLong("c"), k -> new HashMap<>()).put(t.getUniqueId("u"), s);
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        ListNBT list = new ListNBT();
        for (Account a : accounts.values()) list.add(a.write());
        nbt.put("accounts", list);
        nbt.putLong("lastDay", lastDay);
        nbt.putBoolean("jailBuilt", jailBuilt);
        nbt.putLong("jailPos", jailPos.toLong());
        nbt.putBoolean("pitBuilt", pitBuilt);
        nbt.putLong("pitPos", pitPos.toLong());
        nbt.putBoolean("spawnBuilt", spawnBuilt);
        long[] mc = new long[mfoCenters.size()];
        for (int i = 0; i < mc.length; i++) mc[i] = mfoCenters.get(i).toLong();
        nbt.putLongArray("mfoCenters", mc);
        ListNBT land = new ListNBT();
        for (Map.Entry<Long, UUID> e : landOwners.entrySet()) {
            CompoundNBT t = new CompoundNBT();
            t.putLong("c", e.getKey());
            t.putUniqueId("o", e.getValue());
            land.add(t);
        }
        nbt.put("land", land);
        ListNBT pl = new ListNBT();
        for (Map.Entry<Long, Map<UUID, Set<Long>>> e : placed.entrySet()) {
            for (Map.Entry<UUID, Set<Long>> e2 : e.getValue().entrySet()) {
                if (e2.getValue().isEmpty()) continue;
                CompoundNBT t = new CompoundNBT();
                t.putLong("c", e.getKey());
                t.putUniqueId("u", e2.getKey());
                long[] arr = new long[e2.getValue().size()];
                int i = 0;
                for (long l : e2.getValue()) arr[i++] = l;
                t.putLongArray("p", arr);
                pl.add(t);
            }
        }
        nbt.put("placed", pl);
        return nbt;
    }
}
