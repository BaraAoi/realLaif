package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.util.Nbt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

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
public class EconomyData extends SavedData {
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
    /** Версия городских построек на спавне (3 — неразрушимые, новый банк с кассирами). */
    public int spawnVersion;
    /** v1.9: казино досвечено (пол с морскими фонарями), чтобы внутри не спавнились монстры. */
    public int casinoLights;
    /** Казино возле спавна уже построено (или место не найдено). */
    public boolean casinoBuilt;
    /** Все постройки мода: банки, МФО, казино. */
    public final List<Structure> structures = new ArrayList<>();
    /** Легальные заведения игроков (v5). */
    public final List<LegalBusiness> businesses = new ArrayList<>();
    public int nextBizId = 1;
    /** v1.10: торговые центры игроков. */
    public final List<Mall> malls = new ArrayList<>();
    public int nextMallId = 1;
    /** v1.11: боты-конкуренты. */
    public final List<Rival> rivals = new ArrayList<>();
    public int nextRivalId = 1;
    /** Курс BTC в рублях и когда он последний раз менялся (игровые тики). */
    public double btcRate;
    public long btcUpdated;
    /** v1.8: курсы остальных монет (индекс = Crypto.Coin.ordinal(); [0] — не используется, BTC в btcRate). */
    public final double[] altRates = new double[8];
    /** История курсов для графика (не сохраняется): снимки всех монет, последние 40 шагов. */
    public final java.util.ArrayDeque<double[]> cryptoHist = new java.util.ArrayDeque<>();
    /** Центры (уровень пола) всех контор МФО — под ними делается подвал. */
    public final List<BlockPos> mfoCenters = new ArrayList<>();

    /** Владельцы участков (чанков) в обычном мире. */
    public final Map<Long, UUID> landOwners = new HashMap<>();
    /** Кто сколько блоков поставил в каждом чанке: чанк -> игрок -> позиции. */
    public final Map<Long, Map<UUID, Set<Long>>> placed = new HashMap<>();

    public static final net.minecraft.world.level.saveddata.SavedDataType<EconomyData> TYPE = new net.minecraft.world.level.saveddata.SavedDataType<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("reallife", "economy"), EconomyData::new,
            CompoundTag.CODEC.xmap(EconomyData::fromTag, d -> d.write(new CompoundTag())), null);

    public EconomyData() {
    }

    private static EconomyData fromTag(CompoundTag tag) {
        EconomyData d = new EconomyData();
        d.read(tag);
        return d;
    }

    public static ServerLevel overworld(MinecraftServer server) {
        return server.overworld();
    }

    public static EconomyData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public Account get(Player player) {
        Account a = accounts.computeIfAbsent(player.getUUID(), Account::new);
        a.name = player.getGameProfile().name();
        if (a.createdDay < 0 && player.level() != null) {
            a.createdDay = player.level().getOverworldClockTime() / 24000L;
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
        setDirty();
    }

    // ------------------------------------------------------------ земля

    public static long chunkKey(BlockPos pos) {
        return ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public UUID owner(long chunk) {
        return landOwners.get(chunk);
    }

    public void claim(UUID player, long chunk) {
        landOwners.put(chunk, player);
        placed.remove(chunk);
        setDirty();
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
        s.add(pos.asLong());
        setDirty();
        return s.size();
    }

    /** @return true если блок был поставлен этим игроком. */
    public boolean removePlaced(long chunk, UUID player, BlockPos pos) {
        Map<UUID, Set<Long>> m = placed.get(chunk);
        if (m == null) return false;
        Set<Long> s = m.get(player);
        if (s == null) return false;
        boolean r = s.remove(pos.asLong());
        if (r) setDirty();
        return r;
    }

    public void clearPlaced(long chunk) {
        placed.remove(chunk);
        setDirty();
    }

    // ------------------------------------------------------------ постройки

    /** Зарегистрировать постройку (заменяет прежнюю того же типа с тем же углом). */
    public Structure addStructure(Structure st) {
        structures.removeIf(o -> o.type.equals(st.type) && o.min.equals(st.min));
        structures.add(st);
        setDirty();
        return st;
    }

    public Structure structureAt(BlockPos p, int grow) {
        for (Structure st : structures) if (st.contains(p, grow)) return st;
        return null;
    }

    public Structure structureAt(BlockPos p, String type, int grow) {
        for (Structure st : structures) if (st.type.equals(type) && st.contains(p, grow)) return st;
        return null;
    }

    /** Блок внутри неразрушимой городской постройки. */
    public LegalBusiness bizAtCounter(BlockPos p) {
        for (LegalBusiness b : businesses) if (b.counter.equals(p)) return b;
        return null;
    }

    /** Торговый центр, внутри которого (с запасом grow) находится точка. */
    public Mall mallAt(BlockPos p, int grow) {
        for (Mall m : malls) if (m.contains(p, grow)) return m;
        return null;
    }

    public Rival rivalById(int id) {
        for (Rival r : rivals) if (r.id == id) return r;
        return null;
    }

    public Mall mallById(int id) {
        for (Mall m : malls) if (m.id == id) return m;
        return null;
    }

    public List<Mall> mallsOf(UUID owner) {
        List<Mall> l = new ArrayList<>();
        for (Mall m : malls) if (owner.equals(m.owner)) l.add(m);
        return l;
    }

    public LegalBusiness bizById(int id) {
        for (LegalBusiness b : businesses) if (b.id == id) return b;
        return null;
    }

    public List<LegalBusiness> bizOf(UUID owner) {
        List<LegalBusiness> l = new ArrayList<>();
        for (LegalBusiness b : businesses) if (owner.equals(b.owner)) l.add(b);
        return l;
    }

    public boolean ownsBusiness(UUID owner) {
        for (LegalBusiness b : businesses) if (owner.equals(b.owner)) return true;
        return false;
    }

    public boolean isProtected(BlockPos p) {
        for (Structure st : structures) if (st.protect && st.contains(p, 0)) return true;
        return false;
    }

    // ------------------------------------------------------------ NBT

    public void read(CompoundTag nbt) {
        accounts.clear();
        ListTag list = nbt.getListOrEmpty("accounts");
        for (Tag n : list) {
            Account a = Account.read((CompoundTag) n);
            accounts.put(a.id, a);
        }
        lastDay = nbt.getLongOr("lastDay", 0L);
        jailBuilt = nbt.getBooleanOr("jailBuilt", false);
        jailPos = BlockPos.of(nbt.getLongOr("jailPos", 0L));
        pitBuilt = nbt.getBooleanOr("pitBuilt", false);
        pitPos = BlockPos.of(nbt.getLongOr("pitPos", 0L));
        spawnBuilt = nbt.getBooleanOr("spawnBuilt", false);
        spawnVersion = nbt.getIntOr("spawnVersion", 0);
        casinoLights = nbt.getIntOr("casinoLights", 0);
        casinoBuilt = nbt.getBooleanOr("casinoBuilt", false);
        structures.clear();
        for (Tag n : nbt.getListOrEmpty("structures")) structures.add(Structure.read((CompoundTag) n));
        businesses.clear();
        for (Tag n : nbt.getListOrEmpty("legal")) businesses.add(LegalBusiness.read((CompoundTag) n));
        nextBizId = Math.max(1, nbt.getIntOr("nextBizId", 0));
        for (LegalBusiness b : businesses) nextBizId = Math.max(nextBizId, b.id + 1);
        malls.clear();
        for (Tag n : nbt.getListOrEmpty("malls")) malls.add(Mall.read((CompoundTag) n));
        nextMallId = Math.max(1, nbt.getIntOr("nextMallId", 0));
        for (Mall m : malls) nextMallId = Math.max(nextMallId, m.id + 1);
        rivals.clear();
        for (Tag n : nbt.getListOrEmpty("rivals")) rivals.add(Rival.read((CompoundTag) n));
        nextRivalId = Math.max(1, nbt.getIntOr("nextRivalId", 0));
        for (Rival r : rivals) nextRivalId = Math.max(nextRivalId, r.id + 1);
        btcRate = nbt.getDoubleOr("btcRate", 0.0);
        btcUpdated = nbt.getLongOr("btcUpdated", 0L);
        for (int i = 1; i < altRates.length; i++) altRates[i] = nbt.getDoubleOr("altRate" + i, 0.0);
        mfoCenters.clear();
        for (long l : nbt.getLongArray("mfoCenters").orElse(new long[0])) mfoCenters.add(BlockPos.of(l));
        landOwners.clear();
        for (Tag n : nbt.getListOrEmpty("land")) {
            CompoundTag t = (CompoundTag) n;
            landOwners.put(t.getLongOr("c", 0L), Nbt.getUUID(t, "o"));
        }
        placed.clear();
        for (Tag n : nbt.getListOrEmpty("placed")) {
            CompoundTag t = (CompoundTag) n;
            Set<Long> s = new HashSet<>();
            for (long l : t.getLongArray("p").orElse(new long[0])) s.add(l);
            placed.computeIfAbsent(t.getLongOr("c", 0L), k -> new HashMap<>()).put(Nbt.getUUID(t, "u"), s);
        }
    }

    public CompoundTag write(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (Account a : accounts.values()) list.add(a.write());
        nbt.put("accounts", list);
        nbt.putLong("lastDay", lastDay);
        nbt.putBoolean("jailBuilt", jailBuilt);
        nbt.putLong("jailPos", jailPos.asLong());
        nbt.putBoolean("pitBuilt", pitBuilt);
        nbt.putLong("pitPos", pitPos.asLong());
        nbt.putBoolean("spawnBuilt", spawnBuilt);
        nbt.putInt("spawnVersion", spawnVersion);
        nbt.putInt("casinoLights", casinoLights);
        nbt.putBoolean("casinoBuilt", casinoBuilt);
        ListTag sl = new ListTag();
        for (Structure st : structures) sl.add(st.write());
        nbt.put("structures", sl);
        ListTag bl = new ListTag();
        for (LegalBusiness b : businesses) bl.add(b.write());
        nbt.put("legal", bl);
        nbt.putInt("nextBizId", nextBizId);
        ListTag ml = new ListTag();
        for (Mall m : malls) ml.add(m.write());
        nbt.put("malls", ml);
        nbt.putInt("nextMallId", nextMallId);
        ListTag rl = new ListTag();
        for (Rival r : rivals) rl.add(r.write());
        nbt.put("rivals", rl);
        nbt.putInt("nextRivalId", nextRivalId);
        nbt.putDouble("btcRate", btcRate);
        nbt.putLong("btcUpdated", btcUpdated);
        for (int i = 1; i < altRates.length; i++) if (altRates[i] > 0) nbt.putDouble("altRate" + i, altRates[i]);
        long[] mc = new long[mfoCenters.size()];
        for (int i = 0; i < mc.length; i++) mc[i] = mfoCenters.get(i).asLong();
        nbt.putLongArray("mfoCenters", mc);
        ListTag land = new ListTag();
        for (Map.Entry<Long, UUID> e : landOwners.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putLong("c", e.getKey());
            Nbt.putUUID(t, "o", e.getValue());
            land.add(t);
        }
        nbt.put("land", land);
        ListTag pl = new ListTag();
        for (Map.Entry<Long, Map<UUID, Set<Long>>> e : placed.entrySet()) {
            for (Map.Entry<UUID, Set<Long>> e2 : e.getValue().entrySet()) {
                if (e2.getValue().isEmpty()) continue;
                CompoundTag t = new CompoundTag();
                t.putLong("c", e.getKey());
                Nbt.putUUID(t, "u", e2.getKey());
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
