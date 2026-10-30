package com.baraaoi.reallife.event;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.economy.CreditRating;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.economy.ItemValues;
import com.baraaoi.reallife.economy.LegalBusiness;
import com.baraaoi.reallife.entity.VisitorEntity;
import com.baraaoi.reallife.entity.VisitorKind;
import com.baraaoi.reallife.registry.ModEntities;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.LlamaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Случайные гости. Раз в 3–7 минут (бездомным и путешественникам — чаще) к игроку кто-нибудь приходит: налоговая, сантехник, цыганка,
 * гопник, конокрад, риелтор, бабушка с пирожками... На вопросы отвечают кликом в чате [Да]/[Нет]
 * (команда /reallife npc yes|no), ПКМ по гостю = «да», молчание 30 секунд = «нет».
 * Состояние не сохраняется: после перезапуска сервера гости исчезают.
 */
public final class Visitors {
    private Visitors() {}

    private static final Random R = new Random();
    private static final Map<UUID, Session> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final List<Pending> PENDING = new ArrayList<>();
    private static final Set<UUID> MANAGED = new HashSet<>();
    private static final List<NeighborBox> BOXES = new ArrayList<>();

    public static final String SURPRISE_TAG = "reallifeSurprise";
    private static final int ASK_TICKS = 600;

    static final class Session {
        final UUID player;
        final VisitorKind kind;
        final long start;
        final List<VisitorEntity> npcs = new ArrayList<>();
        int stage;
        long nextStep;
        boolean asking;
        long askUntil;
        long leaveAt = -1;
        int price;
        int visit = 1;
        boolean forced;
        boolean fined;
        AbstractHorseEntity horse;
        ItemStack offer = ItemStack.EMPTY;
        List<BlockPos> spots = new ArrayList<>();
        // v1.9
        boolean noNpc;
        int count;
        BlockPos target;
        BlockPos[] box;
        LegalBusiness biz;
        World world;

        Session(UUID player, VisitorKind kind, long start) {
            this.player = player;
            this.kind = kind;
            this.start = start;
        }

        VisitorEntity main() {
            return npcs.isEmpty() ? null : npcs.get(0);
        }
    }

    static final class Pending {
        final UUID player;
        final VisitorKind kind;
        final long at;
        final int visit;
        final boolean forced;

        Pending(UUID player, VisitorKind kind, long at, int visit, boolean forced) {
            this.player = player;
            this.kind = kind;
            this.at = at;
            this.visit = visit;
            this.forced = forced;
        }
    }

    static final class NeighborBox {
        final UUID player;
        final BlockPos min, max;
        final long until;
        boolean fined;

        NeighborBox(UUID player, BlockPos min, BlockPos max, long until) {
            this.player = player;
            this.min = min;
            this.max = max;
            this.until = until;
        }

        boolean contains(BlockPos p) {
            return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY()
                    && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
        }
    }

    /** Гости, которые приходят сами (без помощников). */
    public static List<VisitorKind> eventKinds() {
        List<VisitorKind> l = new ArrayList<>();
        for (VisitorKind k : VisitorKind.values()) if (!k.helper) l.add(k);
        return l;
    }

    public static boolean isManaged(VisitorEntity e) {
        return MANAGED.contains(e.getUniqueID());
    }

    /** Взять сущность под управление (иначе она сама исчезнет). */
    public static void manage(VisitorEntity e) {
        MANAGED.add(e.getUniqueID());
    }

    public static void unmanage(VisitorEntity e) {
        MANAGED.remove(e.getUniqueID());
    }

    // =====================================================================================
    //  планировщик
    // =====================================================================================

    /** Дольше этого (5 мин) ни один гость не задерживается. */
    static final long SESSION_MAX = 6000L;
    /** Если до следующего события «дольше вечности» — значит, таймер остался от другого мира/сессии. */
    static long maxWait() {
        return (Math.max(Config.EVENTS_MIN_MINUTES.get(), Config.EVENTS_MAX_MINUTES.get()) + 2) * 1200L;
    }

    /** Остановка сервера (в одиночке — выход из мира): сбросить всё состояние, чтобы в другом мире ничего не зависло. */
    public static void onStop() {
        for (Session s : ACTIVE.values()) finishQuiet(s);
        ACTIVE.clear();
        NEXT.clear();
        PENDING.clear();
        MANAGED.clear();
        BOXES.clear();
    }

    /** Для /reallife admin events: через сколько тиков следующее событие у игрока (−1 — идёт сейчас). */
    public static long ticksToNext(ServerPlayerEntity p) {
        if (ACTIVE.containsKey(p.getUniqueID())) return -1;
        Long n = NEXT.get(p.getUniqueID());
        return n == null ? 0 : Math.max(0, n - p.world.getGameTime());
    }

    public static String activeKind(ServerPlayerEntity p) {
        Session s = ACTIVE.get(p.getUniqueID());
        return s == null ? null : s.kind.display;
    }

    private static long interval() {
        int min = Config.EVENTS_MIN_MINUTES.get();
        int max = Math.max(min, Config.EVENTS_MAX_MINUTES.get());
        return (min + R.nextInt(max - min + 1)) * 1200L + R.nextInt(1200);
    }

    /** Чем больше станков у игрока, тем чаще гости и проверки (не реже раза в 90 с). */
    public static long interval(int printers) {
        long base = interval();
        if (printers <= 0) return base;
        return Math.max(1800L, (long) (base / (1.0 + 0.15 * printers)));
    }

    /** Интервал до следующего гостя: станки ускоряют, бездомным и путешественникам гости приходят в 1.4 раза чаще. */
    static long intervalFor(ServerPlayerEntity p, Account a) {
        long t = interval(a.printerList.size());
        if (nomad(p, a) || underground(p)) t = Math.max(1200L, (long) (t / 1.4));
        return t;
    }

    /** Гости кочевников: бездомных, шахтёров и путешественников. */
    static final Set<VisitorKind> NOMAD_KINDS = new HashSet<>(Arrays.asList(VisitorKind.UCHASTKOVY, VisitorKind.BLACK_DIGGER,
            VisitorKind.LOST_CAVER, VisitorKind.ECO_INSPECTOR, VisitorKind.HITCHHIKER, VisitorKind.PEDDLER, VisitorKind.HOBO,
            VisitorKind.TREASURE_HUNTER, VisitorKind.DPS));
    /** Кто вообще может прийти под землю. */
    static final Set<VisitorKind> UNDERGROUND_KINDS = new HashSet<>(Arrays.asList(VisitorKind.BLACK_DIGGER, VisitorKind.LOST_CAVER,
            VisitorKind.ECO_INSPECTOR, VisitorKind.PEDDLER, VisitorKind.GOPNIK, VisitorKind.PHILOSOPHER, VisitorKind.BANK_SCAMMER,
            VisitorKind.COURIER, VisitorKind.HOBO, VisitorKind.CAVE_IN));

    /** Игрок под землёй: ниже 56 и не видит неба. */
    public static boolean underground(PlayerEntity p) {
        return p.getPosY() < 56 && !p.world.canSeeSky(p.getPosition().up());
    }

    /**
     * v1.9: «свой угол» — не только ипотечный дом, но и купленная земля (самострой), свои заведения
     * и кровать (точка возрождения в верхнем мире).
     */
    public static List<BlockPos> homePoints(PlayerEntity p, Account a) {
        List<BlockPos> l = new ArrayList<>();
        if (houseOk(a)) l.add(new BlockPos((a.houseMin.getX() + a.houseMax.getX()) / 2, a.houseMin.getY(), (a.houseMin.getZ() + a.houseMax.getZ()) / 2));
        MinecraftServer server = p.getServer();
        if (server != null) {
            EconomyData data = EconomyData.get(server);
            for (Map.Entry<Long, UUID> e : data.landOwners.entrySet()) {
                if (!a.id.equals(e.getValue())) continue;
                l.add(new BlockPos(net.minecraft.util.math.ChunkPos.getX(e.getKey()) * 16 + 8, 64, net.minecraft.util.math.ChunkPos.getZ(e.getKey()) * 16 + 8));
            }
            for (LegalBusiness b : data.bizOf(a.id)) l.add(b.counter);
        }
        if (p instanceof ServerPlayerEntity) {
            ServerPlayerEntity sp = (ServerPlayerEntity) p;
            BlockPos bed = sp.func_241140_K_();
            if (bed != null && sp.func_241141_L_() == World.OVERWORLD) l.add(bed);
        }
        return l;
    }

    /** Есть ли у игрока свой угол (дом по ипотеке, свой участок, заведение или кровать). */
    public static boolean hasHome(PlayerEntity p, Account a) {
        return !homePoints(p, a).isEmpty();
    }

    /** Кочевник: своего угла нет совсем или ушёл от всех своих мест дальше 250 блоков. */
    public static boolean nomad(PlayerEntity p, Account a) {
        for (BlockPos h : homePoints(p, a)) {
            double dx = p.getPosX() - h.getX() - 0.5, dz = p.getPosZ() - h.getZ() - 0.5;
            if (dx * dx + dz * dz <= 250 * 250) return false;
        }
        return true;
    }

    /** v1.9: события, которые приходят вдвое чаще (там, где применимы). */
    static final Set<VisitorKind> SCARY = new HashSet<>(Arrays.asList(VisitorKind.SANEPID, VisitorKind.MIGRATION, VisitorKind.CAVE_IN,
            VisitorKind.FIRE, VisitorKind.BURGLAR));

    /** Своё заведение в 64 блоках (withWorkers — только с работниками; иначе — хотя бы раз работавшее или с работниками). */
    @javax.annotation.Nullable
    static LegalBusiness nearBiz(PlayerEntity p, Account a, boolean withWorkers) {
        MinecraftServer server = p.getServer();
        if (server == null) return null;
        List<LegalBusiness> l = new ArrayList<>();
        for (LegalBusiness b : EconomyData.get(server).bizOf(a.id)) {
            if (withWorkers ? b.workers.isEmpty() : (b.workers.isEmpty() && b.days == 0)) continue;
            if (b.counter.distanceSq(p.getPosX(), p.getPosY(), p.getPosZ(), true) > 64 * 64) continue;
            l.add(b);
        }
        return l.isEmpty() ? null : l.get(R.nextInt(l.size()));
    }

    static void title(ServerPlayerEntity p, String title, String sub) {
        p.connection.sendPacket(new STitlePacket(5, 60, 20));
        p.connection.sendPacket(new STitlePacket(STitlePacket.Type.SUBTITLE, new StringTextComponent(sub)));
        p.connection.sendPacket(new STitlePacket(STitlePacket.Type.TITLE, new StringTextComponent(title)));
    }

    public static void tick(MinecraftServer server) {
        ServerWorld ow = EconomyData.overworld(server);
        if (ow == null) return;
        long now = ow.getGameTime();
        if (now % 20 != 0) return;
        EconomyData data = EconomyData.get(server);

        for (Session s : new ArrayList<>(ACTIVE.values())) {
            ServerPlayerEntity p = server.getPlayerList().getPlayerByUUID(s.player);
            if (p == null || !p.isAlive() || p.world != ow || data.get(p).captivity != Account.FREE) {
                finish(s, now);
                continue;
            }
            // сторож: зависшая сессия (время мира «прыгнуло» назад, сущности выгрузились, гость застрял) не должна
            // навсегда блокировать случайные события
            if (s.start > now || now - s.start > SESSION_MAX || (s.leaveAt >= 0 && s.leaveAt - now > 1200)) {
                RealLife.LOG.info("Real Life: visitor session " + s.kind + " force-finished (watchdog)");
                finish(s, now);
                continue;
            }
            if (s.leaveAt >= 0) {
                if (now >= s.leaveAt) finish(s, now);
                continue;
            }
            try {
                second(s, p, data.get(p), now);
            } catch (RuntimeException ex) {
                RealLife.LOG.error("Real Life: visitor " + s.kind + " failed", ex);
                finish(s, now);
            }
        }
        BOXES.removeIf(b -> now > b.until);

        if (!Config.EVENTS_ENABLED.get()) return;
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending pe = it.next();
            if (now < pe.at) continue;
            ServerPlayerEntity p = server.getPlayerList().getPlayerByUUID(pe.player);
            if (p == null) { it.remove(); continue; }
            if (ACTIVE.containsKey(pe.player) || p.world != ow || data.get(p).captivity != Account.FREE) continue;
            it.remove();
            if (pe.kind == VisitorKind.REALTOR) startRealtor(p, now, pe.visit);
            else start(p, data.get(p), pe.kind, now, pe.forced);
        }
        for (ServerPlayerEntity p : server.getPlayerList().getPlayers()) {
            if (p.world != ow || p.isSpectator()) continue;
            UUID id = p.getUniqueID();
            if (ACTIVE.containsKey(id)) continue;
            Account a = data.get(p);
            if (a.captivity != Account.FREE) continue;
            Long next = NEXT.get(id);
            if (next == null || next - now > maxWait()) { NEXT.put(id, now + intervalFor(p, a)); continue; }
            if (now < next) continue;
            NEXT.put(id, now + intervalFor(p, a));
            List<VisitorKind> ok = new ArrayList<>();
            boolean nomad = nomad(p, a) || underground(p);
            for (VisitorKind k : eventKinds()) {
                if (!applicable(k, p, a, ow)) continue;
                ok.add(k);
                if (nomad && NOMAD_KINDS.contains(k)) ok.add(k); // бездомным/шахтёрам «их» гости — вдвое чаще
                if (SCARY.contains(k)) ok.add(k); // проверки и ЧП — тоже вдвое чаще
            }
            if (!ok.isEmpty()) start(p, a, ok.get(R.nextInt(ok.size())), now, false);
        }
    }

    public static double totalDebt(Account a) {
        return Math.max(0, a.arrears) + Math.max(0, a.loanPrincipal) + Math.max(0, a.mortPrincipal) + Math.max(0, a.mfoDebt);
    }

    static boolean isNight(World w) {
        long t = w.getDayTime() % 24000L;
        return t >= 13000 && t <= 23000;
    }

    public static boolean applicable(VisitorKind k, ServerPlayerEntity p, Account a, World w) {
        boolean under = underground(p);
        if (under && !UNDERGROUND_KINDS.contains(k)) return false;
        switch (k) {
            case BLACK_DIGGER:
            case LOST_CAVER:
                return under;
            case ECO_INSPECTOR:
                return under || nomad(p, a);
            case PEDDLER:
                return nomad(p, a);
            case UCHASTKOVY:
            case HOBO:
                return !hasHome(p, a);
            case HITCHHIKER:
            case TREASURE_HUNTER:
                return nomad(p, a) && !p.isPassenger();
            case DPS:
                return nomad(p, a); // v1.9: у своего дома/участка/кровати ДПС не тормозит
            case SANEPID:
                return nearBiz(p, a, false) != null;
            case MIGRATION:
                return nearBiz(p, a, true) != null;
            case CAVE_IN:
                return under;
            case FIRE:
                return !Inspections.homeBoxes(p, a, EconomyData.get(p.getServer()), 48).isEmpty();
            case BURGLAR:
                return isNight(w) && !Inspections.homeBoxes(p, a, EconomyData.get(p.getServer()), 64).isEmpty();
            case TAX:
            case PLUMBER:
            case ELECTRIC:
            case REALTOR:
            case FIRE_INSPECTOR:
            case NEIGHBOR:
                return a.hasHouse;
            case DEALER:
                return isNight(w);
            case HORSE_THIEF:
                return findHorse(p) != null;
            case PHILOSOPHER:
                return totalDebt(a) > 0.5;
            default:
                return !k.helper;
        }
    }

    // =====================================================================================
    //  общие действия
    // =====================================================================================

    private static VisitorEntity spawn(Session s, ServerWorld w, PlayerEntity p, VisitorKind k, int minR, int maxR) {
        VisitorEntity e = ModEntities.VISITOR.get().create(w);
        if (e == null) return null;
        BlockPos pos = DebtManager.findSpawnPos(w, p, minR, maxR);
        e.setLocationAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, R.nextFloat() * 360F, 0F);
        e.setKind(k);
        e.targetId = p.getUniqueID();
        e.setCustomName(new StringTextComponent(k.display));
        e.setCustomNameVisible(true);
        MANAGED.add(e.getUniqueID());
        w.addEntity(e);
        s.npcs.add(e);
        return e;
    }

    private static void sayAs(VisitorKind k, PlayerEntity p, String text) {
        p.sendStatusMessage(new StringTextComponent("§7<" + k.display + "§7> §f" + text), false);
    }

    private static void say(Session s, PlayerEntity p, String text) {
        sayAs(s.kind, p, text);
    }

    private static void note(PlayerEntity p, String text) {
        p.sendStatusMessage(new StringTextComponent(text), false);
    }

    private static ITextComponent button(String label, String cmd) {
        return new StringTextComponent(label).setStyle(Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd)));
    }

    private static void ask(Session s, PlayerEntity p, String question, long now) {
        say(s, p, question);
        IFormattableTextComponent line = new StringTextComponent("§7   ");
        line.append(button("§a§l[Да]", "/reallife npc yes"));
        line.append(new StringTextComponent("   "));
        line.append(button("§c§l[Нет]", "/reallife npc no"));
        line.append(new StringTextComponent("   §8(ПКМ по гостю — да; молчание 30 с — нет)"));
        p.sendStatusMessage(line, false);
        s.asking = true;
        s.askUntil = now + ASK_TICKS;
    }

    private static void leave(Session s, long now, int ticks) {
        s.asking = false;
        for (VisitorEntity e : s.npcs) {
            if (e.isPassenger()) e.stopRiding();
            e.mode = VisitorEntity.FLEE;
        }
        s.leaveAt = now + ticks;
    }

    private static void finishQuiet(Session s) {
        // игрок умер/вышел посреди пожара — огонь гасят, чтобы не сгорело всё без него
        if (s.kind == VisitorKind.FIRE && s.stage == 1 && s.box != null && s.world != null) Inspections.extinguish(s.world, s.box[0], s.box[1]);
        for (VisitorEntity e : s.npcs) {
            MANAGED.remove(e.getUniqueID());
            if (e.isAlive()) e.remove();
        }
    }

    private static void finish(Session s, long now) {
        finishQuiet(s);
        ACTIVE.remove(s.player);
        NEXT.put(s.player, now + interval());
    }

    /** Списать штраф: со счёта и наличными, а чего не хватило — в долг банку. */
    public static double forceCharge(PlayerEntity p, Account a, double amt) {
        if (amt <= 0) return 0;
        if (BankService.charge(p, a, amt)) return amt;
        double bal = Math.max(0, a.balance);
        a.balance -= bal;
        a.arrears += amt - bal;
        note(p, BankService.BANK + "§cНе хватило денег — " + Finance.money(amt - bal) + " записано вам в долг.");
        return amt;
    }

    static ItemStack named(Item item, int count, String name, String... lore) {
        ItemStack st = new ItemStack(item, count);
        st.setDisplayName(new StringTextComponent(name));
        if (lore.length > 0) {
            CompoundNBT disp = st.getOrCreateChildTag("display");
            ListNBT l = new ListNBT();
            for (String s : lore) l.add(StringNBT.valueOf(ITextComponent.Serializer.toJson(new StringTextComponent(s))));
            disp.put("Lore", l);
        }
        return st;
    }

    private static String pick(String... v) {
        return v[R.nextInt(v.length)];
    }

    private static ItemStack pick(ItemStack... v) {
        return v[R.nextInt(v.length)];
    }

    private static boolean npcsAlive(Session s) {
        for (VisitorEntity e : s.npcs) if (e.isAlive()) return true;
        return false;
    }

    @SuppressWarnings("unused")
    private static Session sessionOf(VisitorEntity e) {
        for (Session s : ACTIVE.values()) if (s.npcs.contains(e)) return s;
        return null;
    }

    // =====================================================================================
    //  дом: вспомогательные функции (без игрока — проверяются в самотесте)
    // =====================================================================================

    public static double taxLimit(int score) {
        return 2000 + score * 10.0;
    }

    private static boolean houseOk(Account a) {
        return a.hasHouse && !a.houseMin.equals(BlockPos.ZERO);
    }

    /** Сколько стоит всё, что лежит в сундуках/холодильниках дома. */
    public static double houseValue(World w, BlockPos min, BlockPos max) {
        double v = 0;
        for (BlockPos p : BlockPos.getAllInBoxMutable(min, max)) {
            TileEntity te = w.getTileEntity(p);
            if (!(te instanceof IInventory)) continue;
            IInventory inv = (IInventory) te;
            for (int i = 0; i < inv.getSizeInventory(); i++) v += ItemValues.stackValue(inv.getStackInSlot(i));
        }
        return v;
    }

    /** Изъять самую дорогую стопку из сундуков дома. */
    public static ItemStack confiscateBest(World w, BlockPos min, BlockPos max) {
        IInventory best = null;
        int slot = -1;
        double bv = 0;
        for (BlockPos p : BlockPos.getAllInBoxMutable(min, max)) {
            TileEntity te = w.getTileEntity(p);
            if (!(te instanceof IInventory)) continue;
            IInventory inv = (IInventory) te;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                double v = ItemValues.stackValue(inv.getStackInSlot(i));
                if (v > bv) { bv = v; best = inv; slot = i; }
            }
        }
        if (best == null) return ItemStack.EMPTY;
        ItemStack st = best.getStackInSlot(slot).copy();
        best.setInventorySlotContents(slot, ItemStack.EMPTY);
        best.markDirty();
        return st;
    }

    public static int countLights(World w, BlockPos min, BlockPos max) {
        int n = 0;
        for (BlockPos p : BlockPos.getAllInBoxMutable(min, max)) {
            BlockState st = w.getBlockState(p);
            if (st.isAir(w, p) || st.getFluidState().isTagged(FluidTags.LAVA) || st.getBlock() == Blocks.FIRE) continue;
            if (st.getLightValue(w, p) > 0) n++;
        }
        return n;
    }

    public static int countFurnaces(World w, BlockPos min, BlockPos max) {
        int n = 0;
        for (BlockPos p : BlockPos.getAllInBoxMutable(min, max)) {
            if (w.getBlockState(p).getBlock() instanceof AbstractFurnaceBlock) n++;
        }
        return n;
    }

    public static List<BlockPos> findLava(World w, BlockPos min, BlockPos max, int limit) {
        List<BlockPos> l = new ArrayList<>();
        for (BlockPos p : BlockPos.getAllInBoxMutable(min, max)) {
            if (w.getFluidState(p).isTagged(FluidTags.LAVA)) {
                l.add(p.toImmutable());
                if (l.size() >= limit) break;
            }
        }
        return l;
    }

    private static boolean standable(World w, BlockPos p) {
        return w.isAirBlock(p) && w.getBlockState(p.down()).getMaterial().isSolid() && w.getTileEntity(p.down()) == null;
    }

    /** Свободные клетки пола внутри дома. */
    public static List<BlockPos> floorSpots(World w, BlockPos min, BlockPos max) {
        List<BlockPos> l = new ArrayList<>();
        for (BlockPos p : BlockPos.getAllInBoxMutable(min.add(1, 1, 1), max.add(-1, -1, -1))) {
            if (standable(w, p) && w.isAirBlock(p.up())) l.add(p.toImmutable());
        }
        return l;
    }

    /** Протечка: n источников воды внутри дома или вокруг него. Возвращает, сколько поставлено. */
    public static int leak(World w, BlockPos min, BlockPos max, boolean inside, int n, Random r) {
        List<BlockPos> spots = new ArrayList<>();
        if (inside) {
            spots = floorSpots(w, min, max);
        } else {
            for (int x = min.getX() - 3; x <= max.getX() + 3; x++)
                for (int z = min.getZ() - 3; z <= max.getZ() + 3; z++) {
                    boolean ring = x < min.getX() - 1 || x > max.getX() + 1 || z < min.getZ() - 1 || z > max.getZ() + 1;
                    if (!ring) continue;
                    int y = w.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos p = new BlockPos(x, y, z);
                    if (standable(w, p)) spots.add(p);
                }
        }
        Collections.shuffle(spots, r);
        int placed = 0;
        for (BlockPos p : spots) {
            if (placed >= n) break;
            w.setBlockState(p, Blocks.WATER.getDefaultState(), 3);
            placed++;
        }
        return placed;
    }

    /** Земляная коробка соседа 3x3x3 (полая) рядом с домом. Возвращает нижний угол или null. */
    public static BlockPos buildNeighborBox(World w, EconomyData data, BlockPos min, BlockPos max) {
        int cz = (min.getZ() + max.getZ()) / 2;
        int cx = (min.getX() + max.getX()) / 2;
        int[][] corners = {{max.getX() + 2, cz - 1}, {min.getX() - 4, cz - 1}, {cx - 1, max.getZ() + 2}, {cx - 1, min.getZ() - 4}};
        for (int[] c : corners) {
            int y = w.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, c[0] + 1, c[1] + 1);
            BlockPos lo = new BlockPos(c[0], y, c[1]);
            BlockPos hi = lo.add(2, 2, 2);
            boolean ok = true;
            for (BlockPos p : BlockPos.getAllInBoxMutable(lo, hi)) {
                if (w.getTileEntity(p) != null || (data != null && data.isProtected(p))) { ok = false; break; }
                if (w.getBlockState(p).getBlockHardness(w, p) < 0) { ok = false; break; }
            }
            if (!ok) continue;
            BlockPos center = lo.add(1, 1, 1);
            for (BlockPos p : BlockPos.getAllInBoxMutable(lo, hi)) {
                w.setBlockState(p.toImmutable(), p.equals(center) ? Blocks.AIR.getDefaultState() : Blocks.DIRT.getDefaultState(), 3);
            }
            return lo;
        }
        return null;
    }

    private static AbstractHorseEntity findHorse(PlayerEntity p) {
        List<AbstractHorseEntity> l = p.world.getEntitiesWithinAABB(AbstractHorseEntity.class, p.getBoundingBox().grow(64.0D),
                h -> h.isAlive() && h.isTame() && h.isHorseSaddled() && !h.isBeingRidden() && !(h instanceof LlamaEntity)
                        && p.getUniqueID().equals(h.getOwnerUniqueId()));
        return l.isEmpty() ? null : l.get(0);
    }

    private static ItemStack pie(boolean surprise) {
        ItemStack st = named(Items.PUMPKIN_PIE, 1, "§6Пирожок " + pick("с картошкой", "с капустой", "с повидлом", "с ливером", "с яйцом и луком"),
                "§7Бабушка Зина пекла с любовью");
        if (surprise) st.getOrCreateTag().putInt(SURPRISE_TAG, 1 + R.nextInt(4));
        return st;
    }

    // =====================================================================================
    //  старт
    // =====================================================================================

    /** Админ: вызвать гостя прямо сейчас. */
    public static String adminStart(ServerPlayerEntity p, VisitorKind k) {
        MinecraftServer server = p.getServer();
        if (server == null) return "нет сервера";
        if (p.world != EconomyData.overworld(server)) return "гости приходят только в верхний мир";
        long now = EconomyData.overworld(server).getGameTime();
        Session old = ACTIVE.get(p.getUniqueID());
        if (old != null) finish(old, now);
        Account a = EconomyData.get(server).get(p);
        if (k.helper) return k.id() + " — помощник, сам не приходит";
        if ((k == VisitorKind.TAX || k == VisitorKind.PLUMBER || k == VisitorKind.ELECTRIC || k == VisitorKind.REALTOR
                || k == VisitorKind.FIRE_INSPECTOR || k == VisitorKind.NEIGHBOR) && !houseOk(a)) return "нужен свой дом";
        if (k == VisitorKind.HORSE_THIEF && findHorse(p) == null) return "нужна своя осёдланная лошадь в 64 блоках";
        if ((k == VisitorKind.BLACK_DIGGER || k == VisitorKind.LOST_CAVER || k == VisitorKind.CAVE_IN) && !underground(p)) return "нужно быть под землёй (ниже 56, без неба над головой)";
        if (k == VisitorKind.SANEPID && nearBiz(p, a, false) == null) return "нужно своё работающее заведение в 64 блоках";
        if (k == VisitorKind.MIGRATION && nearBiz(p, a, true) == null) return "нужно своё заведение с работниками в 64 блоках";
        if ((k == VisitorKind.FIRE || k == VisitorKind.BURGLAR) && Inspections.homeBoxes(p, a, EconomyData.get(server), 64).isEmpty())
            return "нужен свой дом, участок или заведение рядом";
        return start(p, a, k, now, false) == null ? "не получилось" : null;
    }

    private static Session start(ServerPlayerEntity p, Account a, VisitorKind k, long now, boolean forced) {
        ServerWorld w = p.getServerWorld();
        Session s = new Session(p.getUniqueID(), k, now);
        s.forced = forced;
        ACTIVE.put(p.getUniqueID(), s);
        VisitorEntity e;
        switch (k) {
            case TAX:
                e = spawn(s, w, p, k, 4, 7);
                if (forced) {
                    spawn(s, w, p, VisitorKind.TAX_POLICE, 3, 6);
                    spawn(s, w, p, VisitorKind.TAX_POLICE, 3, 6);
                    say(s, p, "§cМы вернулись. С полицией. Проверка теперь принудительная, штраф — двойной.");
                    s.stage = 10;
                    s.nextStep = now + 100;
                } else {
                    ask(s, p, "Добрый день, налоговая инспекция. Плановая проверка имущества. Пройдём в дом, посмотрим сундуки?", now);
                }
                break;
            case BAILIFF:
                e = spawn(s, w, p, k, 4, 7);
                if (e != null) e.followDist = 1.2D;
                say(s, p, "Здравствуйте. Федеральная служба судебных приставов.");
                s.stage = 1;
                break;
            case PLUMBER:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, "Здрасьте, хозяин! Плановая проверка труб. Пустите посмотреть стояк?", now);
                break;
            case ELECTRIC:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, "Энергосбыт! Снимаю показания счётчика. Пустите к щитку? По 3₽ за каждый светильник в доме.", now);
                break;
            case GYPSY:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, "Позолоти ручку, яхонтовый! 50₽ — и всю правду скажу, что было и что будет!", now);
                break;
            case GOPNIK:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, "Слышь, есть сто рублей?", now);
                break;
            case DEALER: {
                spawn(s, w, p, k, 3, 6);
                makeDealerOffer(s);
                ask(s, p, "Тсс... братишка. " + s.offer.getDisplayName().getString() + " надо? Всего " + Finance.money(s.price)
                        + ". Отдаю почти даром, мамой клянусь.", now);
                break;
            }
            case HORSE_THIEF: {
                AbstractHorseEntity horse = findHorse(p);
                if (horse == null) { ACTIVE.remove(p.getUniqueID()); return null; }
                e = ModEntities.VISITOR.get().create(w);
                if (e == null) { ACTIVE.remove(p.getUniqueID()); return null; }
                e.setLocationAndAngles(horse.getPosX(), horse.getPosY(), horse.getPosZ(), horse.rotationYaw, 0F);
                e.setKind(k);
                e.targetId = p.getUniqueID();
                e.setCustomName(new StringTextComponent(k.display));
                e.setCustomNameVisible(true);
                e.mode = VisitorEntity.IDLE;
                MANAGED.add(e.getUniqueID());
                w.addEntity(e);
                s.npcs.add(e);
                e.startRiding(horse, true);
                s.horse = horse;
                note(p, "§c§l[!] Конокрад уводит вашу лошадь! §fДогоните и ударьте его — иначе через 90 секунд её не найти.");
                break;
            }
            case COURIER: {
                spawn(s, w, p, k, 3, 6);
                int r = R.nextInt(3);
                s.offer = r == 0 ? named(Items.CAKE, 1, "§fТорт «Прага»")
                        : r == 1 ? named(Items.BREAD, 3, "§fХлеб «Бородинский»")
                        : named(Items.POISONOUS_POTATO, 4, "§eКартошка фри", "§7Немного позеленела в дороге");
                s.price = 50 + R.nextInt(11) * 10;
                ask(s, p, "Яндекс.Еда! Ваш заказ: " + s.offer.getDisplayName().getString() + ". С вас " + Finance.money(s.price)
                        + ". Как не заказывали? Тут ваш адрес!", now);
                break;
            }
            case GRANNY:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, "Внучек, пирожки домашние, горяченькие! Пять штук — 30 рублей. Возьмёшь?", now);
                break;
            case FIRE_INSPECTOR:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, "Пожарный надзор! Плановая проверка: печи, лава, огнетушители. Впустите?", now);
                break;
            case NEIGHBOR:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, "Здорово, сосед! Я тут к твоему дому пристроечку поставлю, из земли. Ты же не против?", now);
                break;
            case PROMOTER:
                e = spawn(s, w, p, k, 3, 5);
                if (e != null) e.followDist = 1.6D;
                say(s, p, "§a§lКРЕДИТ ОТ 0%! ЗВОНИ СЕЙЧАС!");
                s.stage = 1;
                s.nextStep = now + 1200;
                break;
            case GRANDPA:
                e = spawn(s, w, p, k, 2, 4);
                if (e != null) e.mode = VisitorEntity.STAY;
                say(s, p, "Внучек! Иди сюда, присядь. Расскажу, как мы жили... Только не уходи далеко, дед обидится.");
                s.nextStep = now + 100;
                break;
            case PHILOSOPHER:
                spawn(s, w, p, k, 3, 5);
                s.nextStep = now;
                break;
            case BANK_SCAMMER:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, "Здравствуйте, служба безопасности банка. По вашему счёту подозрительная операция! "
                        + "Назовите код из СМС — и мы её отменим. Назовёте?", now);
                break;
            case POSTMAN:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, "Почта России! Вам посылка. Шла всего четыре месяца. Наложенный платёж — 30₽. Забираете?", now);
                break;
            case SOCIOLOGIST:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, "Здравствуйте, ВЦИОМ. Короткий опрос — три вопроса, плачу 50₽. Пройдёте?", now);
                break;
            case REALTOR:
                ACTIVE.remove(p.getUniqueID());
                startRealtor(p, now, 1);
                return ACTIVE.get(p.getUniqueID());
            case UCHASTKOVY:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, "Старший лейтенант Анискин, участковый. Документики! Регистрация по месту пребывания есть? "
                        + "Нету... Проживание без регистрации — штраф 300₽. Или решим вопрос на месте за 150₽?", now);
                break;
            case BLACK_DIGGER: {
                spawn(s, w, p, k, 2, 5);
                double v = oreValue(p);
                if (v < 20) {
                    s.stage = 1;
                    s.price = 300;
                    ask(s, p, "Тсс, коллега... Руды у тебя кот наплакал. Тогда купи у меня кирку — алмазная, почти новая, «с объекта». 300₽?", now);
                } else {
                    s.price = (int) Math.ceil(v * 1.25 / 10.0) * 10;
                    ask(s, p, "Здорово, коллега. Руду сдаёшь? Беру всё ценное — без документов и налогов. Банк дал бы "
                            + Finance.money(v) + ", я даю §e" + Finance.money(s.price) + "§f наличкой. По рукам?", now);
                }
                break;
            }
            case LOST_CAVER:
                spawn(s, w, p, k, 2, 5);
                ask(s, p, "Ох, живой человек! Я третий день тут блуждаю, фонарик сел, бутерброды кончились... "
                        + "Выведи на поверхность, а? Заплачу, честно!", now);
                break;
            case ECO_INSPECTOR:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, (underground(p) ? "Росприроднадзор! Незаконная добыча полезных ископаемых без лицензии на недропользование. "
                        : "Росприроднадзор! Самовольная стоянка и вырубка в природоохранной зоне. ")
                        + "Штраф по КоАП — 600₽. Оплатите на месте — скидка 50%, 300₽. Оплачиваем?", now);
                break;
            case HITCHHIKER:
                spawn(s, w, p, k, 3, 6);
                s.spots.add(p.getPosition());
                ask(s, p, "Привет! Я Гоша, еду автостопом до Владивостока. Можно я с тобой немного пройдусь? "
                        + "Блоков двести — и я заплачу. Деньгами, не песнями!", now);
                break;
            case PEDDLER:
                spawn(s, w, p, k, 3, 6);
                s.price = 160;
                ask(s, p, "Коробейник Ашот! Набор путешественника: 32 факела, 8 хлеба, 4 стейка и кровать — всего 160₽! "
                        + "Дешевле только даром, дорогой!", now);
                break;
            case HOBO:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, "Братишка, ты тоже без хаты? Уважаю. Дай двадцатку на хлебушек — Валера добро помнит!", now);
                break;
            case TREASURE_HUNTER:
                spawn(s, w, p, k, 3, 6);
                s.price = 250;
                ask(s, p, "Карту клада продам! Старинную, дед мой рисовал. Сам бы выкопал, да спина не та. Всего 250₽. Берёшь?", now);
                break;
            case SANEPID:
            case MIGRATION:
            case FIRE:
            case CAVE_IN:
            case BURGLAR:
                if (!startScary(s, p, a, w, now)) {
                    finishQuiet(s);
                    ACTIVE.remove(p.getUniqueID());
                    return null;
                }
                break;
            case DPS:
                spawn(s, w, p, k, 3, 6);
                ask(s, p, (p.isPassenger() ? "Инспектор ДПС, сержант Петренко. Превышаем, гражданин? Сто сорок в зоне шестьдесят! "
                        : "Инспектор ДПС, сержант Петренко. Пешеход, переходим в неположенном месте! ")
                        + "Штраф 400₽... Или по-быстрому 150₽ — и разъехались?", now);
                break;
            default:
                ACTIVE.remove(p.getUniqueID());
                return null;
        }
        if (s.npcs.isEmpty() && !s.noNpc) {
            ACTIVE.remove(p.getUniqueID());
            return null;
        }
        return s;
    }

    private static void startRealtor(ServerPlayerEntity p, long now, int visit) {
        Session s = new Session(p.getUniqueID(), VisitorKind.REALTOR, now);
        s.visit = visit;
        ACTIVE.put(p.getUniqueID(), s);
        spawn(s, p.getServerWorld(), p, VisitorKind.REALTOR, 3, 6);
        if (s.npcs.isEmpty()) { ACTIVE.remove(p.getUniqueID()); return; }
        s.price = 200 + (visit - 1) * 50;
        String hello = visit == 1 ? "Здравствуйте! Эдуард, агентство «Честный метр»." : visit == 2 ? "Это снова я, Эдуард! Я подумал..." : "Последнее предложение, честно!";
        ask(s, p, hello + " Продайте дом за " + Finance.money(s.price) + "! Лучшее предложение на рынке!", now);
    }

    private static void makeDealerOffer(Session s) {
        int r = R.nextInt(5);
        boolean real;
        switch (r) {
            case 0:
                real = R.nextDouble() < 0.3;
                s.offer = named(real ? Items.DIAMOND : Items.LIGHT_BLUE_STAINED_GLASS, 1, "§bАлмаз (настоящий)");
                break;
            case 1:
                real = R.nextDouble() < 0.4;
                s.offer = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), real ? Potions.STRONG_REGENERATION : Potions.WATER);
                s.offer.setDisplayName(new StringTextComponent("§dЗелье бессмертия"));
                break;
            case 2:
                s.offer = named(Items.BRICK, 1, "§fАйфон 15 Про Макс", "§7В коробке. Тяжёленький какой-то...");
                break;
            case 3:
                real = R.nextDouble() < 0.4;
                s.offer = named(real ? Items.CLOCK : Items.GOLD_NUGGET, 1, "§6Золотые часы «Ролекс»");
                break;
            default:
                real = R.nextDouble() < 0.2;
                s.offer = named(real ? Items.NETHERITE_SCRAP : Items.CHARCOAL, 1, "§8Незерит, чистый");
        }
        s.price = 200 + R.nextInt(31) * 10;
    }

    // =====================================================================================
    //  ответы
    // =====================================================================================

    public static void answer(ServerPlayerEntity p, boolean yes) {
        Session s = ACTIVE.get(p.getUniqueID());
        if (s == null || !s.asking || s.leaveAt >= 0) {
            note(p, "§8Никто ничего не спрашивал.");
            return;
        }
        MinecraftServer server = p.getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        long now = EconomyData.overworld(server).getGameTime();
        s.asking = false;
        onAnswer(s, p, data.get(p), yes, now);
        data.markDirty();
    }

    private static void onAnswer(Session s, ServerPlayerEntity p, Account a, boolean yes, long now) {
        ServerWorld w = p.getServerWorld();
        switch (s.kind) {
            case TAX:
                if (yes) {
                    say(s, p, "Так-с, посмотрим, что у нас в сундуках...");
                    s.stage = 10;
                    s.nextStep = now + 80;
                } else if (R.nextDouble() < 0.4) {
                    say(s, p, "§cОтказываетесь? Ну-ну. Мы вернёмся. И не одни.");
                    PENDING.add(new Pending(p.getUniqueID(), VisitorKind.TAX, now + 2400, 1, true));
                    leave(s, now, 100);
                } else {
                    say(s, p, "Ладно. В этот раз повезло. Мы с вами ещё свяжемся.");
                    leave(s, now, 100);
                }
                break;
            case PLUMBER:
                if (yes) {
                    List<BlockPos> spots = houseOk(a) ? floorSpots(w, a.houseMin, a.houseMax) : new ArrayList<>();
                    VisitorEntity m = s.main();
                    if (!spots.isEmpty() && m != null) {
                        BlockPos sp = spots.get(R.nextInt(spots.size()));
                        m.setPositionAndUpdate(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5);
                        m.mode = VisitorEntity.STAY;
                    }
                    say(s, p, "Так, где тут у вас стояк... Ага. Сейчас подкрутим.");
                    s.stage = 1;
                    s.nextStep = now + 80;
                } else {
                    int n = houseOk(a) ? leak(w, a.houseMin, a.houseMax, false, 3 + R.nextInt(3), R) : 0;
                    say(s, p, "Как хотите. У вас, кстати, снаружи трубу прорвало" + (n > 0 ? " — вон, " + n + " луж(и)." : ".") + " Не я, оно само.");
                    leave(s, now, 100);
                }
                break;
            case ELECTRIC: {
                int lights = houseOk(a) ? countLights(w, a.houseMin, a.houseMax) : 0;
                double bill = Math.max(10, lights * 3);
                if (yes) {
                    forceCharge(p, a, bill);
                    say(s, p, "Насчитал " + lights + " светильник(ов). С вас " + Finance.money(bill) + ". Свет — не роскошь, а тариф.");
                } else {
                    forceCharge(p, a, bill * 2);
                    say(s, p, "§cНе пускаете? Посчитаем по нормативу: " + Finance.money(bill * 2) + " (вдвое). Квитанцию в ящик кину.");
                }
                leave(s, now, 100);
                break;
            }
            case GYPSY:
                if (yes && BankService.charge(p, a, 50)) {
                    switch (R.nextInt(4)) {
                        case 0:
                            p.addPotionEffect(new EffectInstance(Effects.LUCK, 6000, 0));
                            say(s, p, "Вижу удачу! Большую! Иди в казино, золотой!");
                            break;
                        case 1:
                            p.addPotionEffect(new EffectInstance(Effects.UNLUCK, 6000, 0));
                            say(s, p, "Ой... порча на тебе, яхонтовый. Не я наводила, честно!");
                            break;
                        case 2:
                            p.addPotionEffect(new EffectInstance(Effects.POISON, 600, 0));
                            say(s, p, "Это... очищение. Так надо. Потерпи тридцать секунд.");
                            break;
                        default:
                            p.addPotionEffect(new EffectInstance(Effects.NIGHT_VISION, 6000, 0));
                            say(s, p, "Глаза твои откроются, и увидишь ты во тьме!");
                    }
                } else {
                    p.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 2400, 0));
                    say(s, p, "§5Ах так?! Чтоб у тебя ноги отнялись на два... минуты!");
                }
                leave(s, now, 100);
                break;
            case GOPNIK:
                if (s.stage == 0) {
                    if (yes && BankService.charge(p, a, 100)) {
                        say(s, p, "Красава. Живи.");
                        leave(s, now, 100);
                    } else {
                        if (yes) say(s, p, "Чё, нету? А чё карманы звенят тогда?");
                        spawn(s, w, p, VisitorKind.GOPNIK_FRIEND, 3, 5);
                        spawn(s, w, p, VisitorKind.GOPNIK_FRIEND, 3, 5);
                        say(s, p, "Пацаны! Сюда! Тут один жадный!");
                        s.stage = 1;
                        ask(s, p, "Ну всё, теперь с тебя триста. Есть?", now);
                    }
                } else {
                    if (yes && BankService.charge(p, a, 300)) {
                        say(s, p, "Вот это разговор. Бывай, братан.");
                        leave(s, now, 100);
                    } else {
                        fight(s, p, now);
                    }
                }
                break;
            case DEALER:
                if (yes) {
                    if (BankService.charge(p, a, s.price)) {
                        BankService.give(p, s.offer.copy());
                        say(s, p, "Носи на здоровье. Мы не знакомы.");
                    } else {
                        say(s, p, "Денег нет? Не трать моё время.");
                    }
                } else {
                    say(s, p, "Ну и сиди без " + s.offer.getDisplayName().getString() + "§f.");
                }
                leave(s, now, 80);
                break;
            case COURIER:
                if (yes) {
                    if (BankService.charge(p, a, s.price)) {
                        BankService.give(p, s.offer.copy());
                        say(s, p, "Приятного аппетита! Поставьте пять звёзд, пожалуйста!");
                    } else {
                        say(s, p, "Не хватает? Ну... тогда я сам съем.");
                    }
                } else {
                    say(s, p, "Ну и ладно, сам съем. А вам — одна звезда.");
                }
                leave(s, now, 80);
                break;
            case REALTOR:
                if (yes) {
                    say(s, p, "Отлично! Только внесите аванс за оформление — 50₽. Всё, бегу в МФЦ за документами!");
                    if (BankService.charge(p, a, 50)) {
                        note(p, "§8*Риелтор взял аванс и растворился в воздухе. Дом, к счастью, всё ещё ваш.*");
                    } else {
                        say(s, p, "Даже пятидесяти нет? Ой, всё.");
                    }
                    leave(s, now, 40);
                } else {
                    say(s, p, "Подумайте ещё! Я пока тут рядом похожу.");
                    s.stage = 1;
                    s.nextStep = now + 800;
                    VisitorEntity m = s.main();
                    if (m != null) m.followDist = 2.0D;
                }
                break;
            case GRANNY:
                if (yes && BankService.charge(p, a, 30)) {
                    int surprise = R.nextInt(5);
                    for (int i = 0; i < 5; i++) BankService.give(p, pie(i == surprise));
                    say(s, p, "Кушай, кушай, худенький какой! Один — с сюрпризом, хи-хи.");
                } else {
                    BankService.give(p, pie(R.nextBoolean()));
                    say(s, p, "Ну хоть один возьми, бесплатно. А то совсем отощал.");
                }
                leave(s, now, 100);
                break;
            case FIRE_INSPECTOR:
                if (yes) {
                    say(s, p, "Посмотрим, что у вас тут горит...");
                    s.stage = 1;
                    s.nextStep = now + 60;
                } else {
                    forceCharge(p, a, 200);
                    say(s, p, "§cВоспрепятствование проверке — 200₽. Акт составлен.");
                    leave(s, now, 100);
                }
                break;
            case NEIGHBOR: {
                BlockPos box = houseOk(a) ? buildNeighborBox(w, EconomyData.get(p.getServer()), a.houseMin, a.houseMax) : null;
                if (box == null) {
                    say(s, p, "Хм, места нет. Ну ладно, в другой раз.");
                } else {
                    BOXES.add(new NeighborBox(p.getUniqueID(), box, box.add(2, 2, 2), now + 24000));
                    say(s, p, yes ? "Вот спасибо, сосед! Только не ломай — заявление участковому напишу."
                            : "А я и не спрашивал. Уже стоит! Сломаешь в течение суток — штраф 200₽.");
                }
                leave(s, now, 100);
                break;
            }
            case BANK_SCAMMER:
                if (yes) {
                    double loss = Math.floor(Math.min(2000, Math.max(0, a.balance * 0.2)));
                    a.balance -= loss;
                    say(s, p, "Спасибо! Операция... эээ... отменена. Всего доброго!");
                    note(p, BankService.BANK + "§cСписание " + Finance.money(loss) + " — перевод на карту «Иван И.». Сотрудники банка никогда не спрашивают коды!");
                } else {
                    say(s, p, "Алло? Алло? Связь пропадает... *пип-пип-пип*");
                    note(p, BankService.BANK + "§aПравильно! Сотрудники банка никогда не спрашивают коды из СМС.");
                }
                leave(s, now, 60);
                break;
            case POSTMAN:
                if (yes) {
                    if (BankService.charge(p, a, 30)) {
                        BankService.give(p, parcel());
                        say(s, p, "Распишитесь тут. И тут. И вот тут ещё. Коробка слегка помялась, это нормально.");
                    } else {
                        say(s, p, "Без оплаты не отдам. Вернём отправителю.");
                    }
                } else {
                    say(s, p, "Ну и ладно, вернём отправителю. Месяцев через восемь дойдёт.");
                }
                leave(s, now, 100);
                break;
            case SOCIOLOGIST:
                if (yes) {
                    s.stage = 1;
                    s.nextStep = now + 40;
                } else {
                    say(s, p, "Так и запишем: «затруднился ответить».");
                    leave(s, now, 80);
                }
                break;
            case SANEPID:
            case MIGRATION:
                answerScary(s, p, a, yes, now);
                break;
            case UCHASTKOVY:
            case DPS: {
                boolean dps = s.kind == VisitorKind.DPS;
                int fine = dps ? 400 : 300;
                if (yes && BankService.charge(p, a, 150)) {
                    if (R.nextDouble() < 0.2) {
                        forceCharge(p, a, fine);
                        say(s, p, "§cДача взятки должностному лицу! " + (dps ? "Видеорегистратор всё записал." : "Это была контрольная закупка.")
                                + " §fИ штраф " + Finance.money(fine) + " сверху.");
                    } else {
                        say(s, p, dps ? "Счастливого пути. Пристегнитесь." : "Свободен. И купи уже себе дом, бродяга. Ипотеку дают!");
                    }
                } else if (R.nextBoolean()) {
                    forceCharge(p, a, fine);
                    say(s, p, "§cПротокол составлен. Штраф " + Finance.money(fine) + ". Распишитесь вот тут.");
                } else {
                    say(s, p, dps ? "Ладно, на первый раз — устное предупреждение. Аккуратнее там."
                            : "Ладно, вижу — человек честный. Но в следующий раз — в обезьянник до выяснения.");
                }
                leave(s, now, 100);
                break;
            }
            case BLACK_DIGGER:
                if (s.stage == 1) {
                    if (yes && BankService.charge(p, a, s.price)) {
                        if (R.nextDouble() < 0.6) {
                            ItemStack pick = named(Items.DIAMOND_PICKAXE, 1, "§bКирка «с объекта»", "§7Без документов. Не спрашивай.");
                            pick.setDamage(pick.getMaxDamage() * 2 / 3);
                            BankService.give(p, pick);
                            say(s, p, "Держи. Почти новая. Если что — ты меня не видел.");
                        } else {
                            BankService.give(p, named(Items.WOODEN_PICKAXE, 1, "§bКирка «алмазная»", "§7Покрашена голубой краской"));
                            say(s, p, "Держи. Алмазная, мамой клянусь! §8*быстро исчезает в темноте*");
                        }
                    } else {
                        say(s, p, "Ну и копай руками.");
                    }
                } else if (yes) {
                    double v = sellOres(p);
                    int pay = (int) Math.ceil(v * 1.25 / 10.0) * 10;
                    if (v <= 0) {
                        say(s, p, "Э, а руда где? Пока болтали — всё растерял?");
                    } else if (R.nextDouble() < 0.15) {
                        BankService.give(p, com.baraaoi.reallife.item.DirtyMoneyItem.of(100, Math.max(1, pay / 100)));
                        say(s, p, "Держи налик. Свеженький, ещё краской пахнет. §8*это грязные деньги — отмывай*");
                    } else {
                        BankService.giveCash(p, pay);
                        say(s, p, "Держи " + Finance.money(pay) + ". Приятно иметь дело. Будет ещё — я тут, в штреке.");
                    }
                } else {
                    say(s, p, "Тащи в банк, там тебе меньше дадут. И налоговую в придачу.");
                }
                leave(s, now, 80);
                break;
            case LOST_CAVER:
                if (yes) {
                    VisitorEntity m = s.main();
                    if (m != null) { m.mode = VisitorEntity.FOLLOW; m.followDist = 2.2D; }
                    say(s, p, "Спасибо! Иду за тобой. Только не беги — я уже ног не чувствую.");
                    s.stage = 1;
                    s.nextStep = now + 4800;
                } else {
                    say(s, p, "Эх... Ладно. Нацарапаю на стене: «Здесь был Вася». Может, кто прочтёт.");
                    leave(s, now, 80);
                }
                break;
            case ECO_INSPECTOR:
                if (yes) {
                    forceCharge(p, a, 300);
                    say(s, p, "Квитанция ваша. Берегите природу — мать вашу.");
                } else if (R.nextBoolean()) {
                    forceCharge(p, a, 600);
                    say(s, p, "§cТогда полный штраф: 600₽. Квитанция придёт на Госуслуги.");
                } else {
                    say(s, p, "Ладно, на первый раз — предупреждение. Но я вас запомнил. Фото есть.");
                }
                leave(s, now, 100);
                break;
            case HITCHHIKER:
                if (yes) {
                    VisitorEntity m = s.main();
                    if (m != null) { m.mode = VisitorEntity.FOLLOW; m.followDist = 2.6D; }
                    say(s, p, "Огонь! Погнали. Я буду рассказывать истории. Ты не против? Я всё равно буду.");
                    s.stage = 1;
                    s.nextStep = now + 5000;
                } else {
                    say(s, p, "Понял, не дурак. Удачи на трассе!");
                    leave(s, now, 80);
                }
                break;
            case PEDDLER:
                if (yes && BankService.charge(p, a, s.price)) {
                    BankService.give(p, new ItemStack(Items.TORCH, 32));
                    BankService.give(p, new ItemStack(Items.BREAD, 8));
                    BankService.give(p, new ItemStack(Items.COOKED_BEEF, 4));
                    BankService.give(p, new ItemStack(Items.RED_BED, 1));
                    if (R.nextDouble() < 0.25) BankService.give(p, named(Items.COMPASS, 1, "§fКомпас (китайский)", "§7Показывает куда-то"));
                    say(s, p, "Носи на здоровье, дорогой! Кровать — ортопедическая. Почти.");
                } else {
                    BankService.give(p, new ItemStack(Items.TORCH, 1));
                    say(s, p, "Ай, обидел! Ладно, держи факел в подарок — вспомнишь Ашота!");
                }
                leave(s, now, 80);
                break;
            case HOBO:
                if (yes && BankService.charge(p, a, 20)) {
                    int r = R.nextInt(3);
                    if (r == 0) {
                        say(s, p, "Спасибо, братан. Лайфхак за лайфхак: " + pick(HOBO_TIPS));
                    } else if (r == 1) {
                        ItemStack gift = pick(new ItemStack(Items.IRON_INGOT, 3), new ItemStack(Items.GOLDEN_CARROT, 2),
                                named(Items.EMERALD, 1, "§aНайденный изумруд", "§7Валера нашёл. Валера делится."), new ItemStack(Items.COOKED_COD, 4));
                        BankService.give(p, gift);
                        say(s, p, "Держи, нашёл тут недалеко. Мне без надобности, а тебе пригодится.");
                    } else {
                        p.addPotionEffect(new EffectInstance(Effects.LUCK, 3600, 0));
                        say(s, p, "Ты добрый человек. Удача к таким возвращается — сам увидишь.");
                    }
                } else {
                    BankService.give(p, new ItemStack(Items.BREAD, 1));
                    say(s, p, "Даже двадцатки нет? Да ты беднее меня! На, держи хлеб. Держись, братишка.");
                }
                leave(s, now, 100);
                break;
            case TREASURE_HUNTER:
                if (yes && BankService.charge(p, a, s.price)) {
                    BlockPos at = R.nextDouble() < 0.7 ? buryTreasure(w, p) : null;
                    BlockPos shown = at != null ? at : p.getPosition().add(R.nextInt(81) - 40, 0, R.nextInt(81) - 40);
                    BankService.give(p, named(Items.PAPER, 1, "§6Карта клада", "§7X: " + shown.getX() + "  Z: " + shown.getZ(),
                            "§7Копать на 3–4 блока вглубь", "§8Подпись: Семёныч"));
                    say(s, p, "Вот. X " + shown.getX() + ", Z " + shown.getZ() + ". Копай метра на три. Если пусто — значит, кто-то успел раньше!");
                } else {
                    say(s, p, "Ну и ходи без клада. А там, между прочим, золото!");
                }
                leave(s, now, 80);
                break;
            default:
                leave(s, now, 60);
        }
    }

    private static final String[] HOBO_TIPS = {
            "участок можно купить сразу 3×3 чанка: /reallife land buy. Строй сколько влезет — и никакой инспектор.",
            "не ходи в МФО. Я вот ходил. Видишь, где я теперь живу?",
            "банк проверяет кредитный рейтинг. Плати вовремя — и даже ипотеку дадут.",
            "в вендинге продают справочник «Как выжить». Я по нему и выживаю.",
            "чёрный копатель под землёй берёт руду дороже банка. Только деньги у него иногда... пахнут.",
            "шаурмичная окупается быстрее, чем кажется. Главное — склад и поставки.",
            "налоговая смотрит сундуки дома. Меня вот не смотрит — у меня нет дома. Плюсы!"};

    private static final Set<Item> ORE_ITEMS = new HashSet<>(Arrays.asList(Items.COAL, Items.IRON_ORE, Items.IRON_INGOT, Items.GOLD_ORE,
            Items.GOLD_INGOT, Items.REDSTONE, Items.LAPIS_LAZULI, Items.DIAMOND, Items.EMERALD, Items.QUARTZ, Items.ANCIENT_DEBRIS,
            Items.NETHERITE_SCRAP));

    /** Сколько банк дал бы за руду в инвентаре. */
    static double oreValue(PlayerEntity p) {
        double v = 0;
        for (ItemStack st : p.inventory.mainInventory) if (ORE_ITEMS.contains(st.getItem())) v += ItemValues.stackValue(st);
        return v;
    }

    /** Забрать руду из инвентаря, вернуть её банковскую стоимость. */
    static double sellOres(PlayerEntity p) {
        double v = 0;
        for (int i = 0; i < p.inventory.mainInventory.size(); i++) {
            ItemStack st = p.inventory.mainInventory.get(i);
            if (!ORE_ITEMS.contains(st.getItem())) continue;
            v += ItemValues.stackValue(st);
            p.inventory.mainInventory.set(i, ItemStack.EMPTY);
        }
        return v;
    }

    /** Закопать сундук с кладом в 30–60 блоках на ничейной земле. null — не вышло. */
    static BlockPos buryTreasure(ServerWorld w, PlayerEntity p) {
        MinecraftServer server = w.getServer();
        EconomyData data = EconomyData.get(server);
        for (int i = 0; i < 12; i++) {
            double ang = R.nextDouble() * Math.PI * 2;
            int r = 30 + R.nextInt(31);
            int x = (int) Math.round(p.getPosX() + Math.cos(ang) * r), z = (int) Math.round(p.getPosZ() + Math.sin(ang) * r);
            int top = w.getHeight(Heightmap.Type.OCEAN_FLOOR, x, z);
            BlockPos pos = new BlockPos(x, top - 4, z);
            if (pos.getY() < 6 || data.owner(EconomyData.chunkKey(pos)) != null) continue;
            if (!w.getFluidState(pos.up(4)).isEmpty()) continue;
            w.setBlockState(pos, Blocks.CHEST.getDefaultState(), 2);
            TileEntity te = w.getTileEntity(pos);
            if (!(te instanceof IInventory)) continue;
            IInventory inv = (IInventory) te;
            List<ItemStack> loot = new ArrayList<>();
            int cash = 200 + R.nextInt(5) * 100;
            for (int c = 0; c < cash / 100; c++) loot.add(new ItemStack(BankService.noteItem(100)));
            loot.add(new ItemStack(Items.GOLD_INGOT, 2 + R.nextInt(4)));
            loot.add(new ItemStack(Items.IRON_INGOT, 3 + R.nextInt(6)));
            if (R.nextBoolean()) loot.add(new ItemStack(Items.EMERALD, 1 + R.nextInt(3)));
            if (R.nextDouble() < 0.25) loot.add(new ItemStack(Items.DIAMOND, 1 + R.nextInt(2)));
            loot.add(named(Items.GOLD_NUGGET, 3, "§6Царский червонец", "§7Найден по карте Семёныча"));
            for (int k = 0; k < loot.size() && k < inv.getSizeInventory(); k++) inv.setInventorySlotContents(k * 2 % inv.getSizeInventory(), loot.get(k));
            te.markDirty();
            return pos;
        }
        return null;
    }

    private static ItemStack parcel() {
        if (R.nextDouble() < 0.3) return named(Items.PAPER, 1, "§7Мятая коробка", "§8Внутри пусто. «Потерялось при пересылке».");
        switch (R.nextInt(6)) {
            case 0: return named(Items.IRON_INGOT, 5, "§fЗапчасти с Алиэкспресса");
            case 1: return named(Items.GOLDEN_APPLE, 1, "§6Яблоко из Турции");
            case 2: return named(Items.LEATHER_BOOTS, 1, "§fНоски от бабушки", "§7Шерстяные. Колются.");
            case 3: return named(Items.COOKIE, 12, "§6Печенье «Юбилейное»", "§7Слегка раскрошилось");
            case 4: return named(Items.EMERALD, 2, "§aСувенир с моря");
            default: return named(Items.BOOK, 1, "§fКвитанция об оплате", "§7За посылку, которую вы уже получили");
        }
    }

    private static void fight(Session s, ServerPlayerEntity p, long now) {
        s.asking = false;
        s.stage = 2;
        s.nextStep = now + 600;
        for (VisitorEntity e : s.npcs) e.mode = VisitorEntity.ATTACK;
        say(s, p, "§cНу всё, ты попал.");
    }

    // =====================================================================================
    //  каждую секунду
    // =====================================================================================

    private static final String[] GRANDPA_STORY = {
            "Вот в наше время хлеб стоил 16 копеек! А сейчас? Тьфу!",
            "Я в 91-м ваучер на «Волгу» поменял. Без «Волги» остался, зато с опытом.",
            "Мы в армии кирпичи красили, чтоб генералу красиво было. Белым. Зимой.",
            "А бабка твоя, царствие... то есть, она жива, на рынке. Тоже история.",
            "Был у меня кредит в 98-м. Потом дефолт. Потом кредита не было. И денег не было.",
            "Я этот дом, считай, своими руками... Ну, не этот. И не своими. Но строил!",
            "Знаешь, внучек, главное в жизни — не брать в МФО. Эх, кто бы мне сказал...",
            "Ну вот. А теперь иди, внучек. Деда уважил — дед не забудет."
    };
    private static final String[] PROMO = {
            "§a§lКРЕДИТ ОТ 0%! ЗВОНИ СЕЙЧАС!", "§a§lДЕНЬГИ ЗА 5 МИНУТ! БЕЗ СПРАВОК!", "§a§lПЕРВЫЙ ЗАЙМ БЕСПЛАТНО!*",
            "§a§lОДОБРЕНИЕ 99%! ДАЖЕ С ПРОСРОЧКАМИ!", "§a§lВОЗЬМИ ЗАЙМ — ПОЛУЧИ МАГНИТИК!", "§8*мелким шрифтом: 1% в день, штраф за всё*"};
    private static final String[] REALTOR_NAG = {
            "Цены падают! Завтра будет поздно!", "Я вам как родному говорю — продавайте!", "Соседи уже продали. Вы последний!",
            "Могу накинуть... ну, ещё десятку. Нет? Подумайте!", "А давайте я просто запишу ваш номер?"};
    private static final String[] PHILOSOPHY = {
            "Долг в %s — это лишь число. Но числа, в отличие от людей, не прощают.",
            "Ты думаешь, что владеешь деньгами. Но это %s владеют тобой.",
            "Сократ говорил: «Я знаю, что ничего не знаю». А я знаю, что ты должен %s.",
            "Всё проходит. Кроме процентов. %s и растёт.",
            "Не я прихожу к должнику. Это долг в %s приходит ко мне. Подумай об этом."};

    private static void second(Session s, ServerPlayerEntity p, Account a, long now) {
        ServerWorld w = p.getServerWorld();
        VisitorEntity m = s.main();
        if (s.kind == VisitorKind.HORSE_THIEF) {
            horseThief(s, p, now);
            return;
        }
        if (s.noNpc && s.npcs.isEmpty()) {
            secondScary(s, p, a, w, now);
            return;
        }
        if (!npcsAlive(s)) {
            if (s.kind == VisitorKind.BURGLAR && !s.offer.isEmpty()) {
                BankService.give(p, s.offer);
                s.offer = ItemStack.EMPTY;
                note(p, "§aДомушник повержен — украденное вернулось к вам.");
            }
            if (s.kind == VisitorKind.GOPNIK) note(p, "§aВы отбились от гопников! Район ваш.");
            else note(p, "§8" + s.kind.display + "§8 больше не придёт.");
            finish(s, now);
            return;
        }
        if (s.asking && now >= s.askUntil) {
            s.asking = false;
            say(s, p, "§7Молчание — знак несогласия.");
            onAnswer(s, p, a, false, now);
            return;
        }
        // кто-то ушёл слишком далеко — «подъехал на такси»
        if (s.kind != VisitorKind.BURGLAR) for (VisitorEntity e : s.npcs) {
            if (e.isAlive() && e.mode != VisitorEntity.STAY && !e.isPassenger() && e.getDistanceSq(p) > 40 * 40) DebtManager.teleportNear(e, p, 6);
        }
        if (s.asking) return;
        switch (s.kind) {
            case SANEPID:
            case MIGRATION:
            case FIRE:
            case CAVE_IN:
            case BURGLAR:
                secondScary(s, p, a, w, now);
                break;
            case TAX:
                if (s.stage == 10 && now >= s.nextStep) {
                    taxCheck(s, p, a, s.forced ? 2 : 1);
                    leave(s, now, 120);
                }
                break;
            case BAILIFF:
                if (m != null && m.getDistanceSq(p) < 2.6D * 2.6D) {
                    List<Integer> slots = new ArrayList<>();
                    for (int i = 0; i < p.inventory.mainInventory.size(); i++) if (!p.inventory.mainInventory.get(i).isEmpty()) slots.add(i);
                    if (slots.isEmpty()) {
                        note(p, "§8*Пристав молча осмотрел ваши пустые карманы, вздохнул и ушёл.*");
                    } else {
                        int slot = slots.get(R.nextInt(slots.size()));
                        ItemStack st = p.inventory.mainInventory.get(slot);
                        note(p, "§8*Пристав молча забрал у вас: §7" + st.getDisplayName().getString() + " ×" + st.getCount() + "§8 — и ушёл, не попрощавшись.*");
                        p.inventory.mainInventory.set(slot, ItemStack.EMPTY);
                    }
                    leave(s, now, 100);
                } else if (now - s.start > 800) {
                    leave(s, now, 60);
                }
                break;
            case PLUMBER:
                if (s.stage == 1 && now >= s.nextStep) {
                    int n = houseOk(a) ? leak(w, a.houseMin, a.houseMax, true, 3 + R.nextInt(3), R) : 0;
                    say(s, p, n > 0 ? "§3*БУЛЬК* §fОй. Трубу сорвало. Это не я, оно само. Пойду за ключом на 32... (не вернётся)"
                            : "Всё в порядке, хозяин. Вызов — бесплатно. Сегодня.");
                    leave(s, now, 100);
                }
                break;
            case GOPNIK:
                if (s.stage == 2 && (p.getHealth() <= 5.0F || now >= s.nextStep)) {
                    say(s, p, "Ладно, живи. Это было предупреждение.");
                    leave(s, now, 100);
                }
                break;
            case REALTOR:
                if (s.stage == 1) {
                    if (now >= s.nextStep) {
                        if (s.visit < 3) {
                            say(s, p, "Я ещё вернусь! С новым предложением!");
                            PENDING.add(new Pending(p.getUniqueID(), VisitorKind.REALTOR, now + 3600 + R.nextInt(2400), s.visit + 1, false));
                        } else {
                            say(s, p, "Ладно, ваша взяла... (уходит, грустно звеня ключами)");
                        }
                        leave(s, now, 100);
                    } else if ((now - s.start) % 100 == 0) {
                        say(s, p, pick(REALTOR_NAG));
                    }
                }
                break;
            case FIRE_INSPECTOR:
                fireInspector(s, p, a, now);
                break;
            case PROMOTER:
                if (now >= s.nextStep) {
                    say(s, p, "Мой рабочий день окончен! Кредит ждёт вас!");
                    leave(s, now, 80);
                } else if ((now - s.start) % 80 == 0) {
                    say(s, p, pick(PROMO));
                }
                break;
            case GRANDPA:
                if (m != null && m.getDistanceSq(p) > 10 * 10) {
                    p.attackEntityFrom(DamageSource.causeMobDamage(m), 2.0F);
                    BankService.give(p, named(Items.LEATHER_BOOTS, 1, "§6Ботинок деда Митрича", "§7Прилетел, когда вы не дослушали"));
                    say(s, p, "§cА НУ ВЕРНИСЬ, Я НЕ ДОГОВОРИЛ!!! §8*в вас летит ботинок*");
                    leave(s, now, 100);
                } else if (now >= s.nextStep) {
                    if (s.stage < GRANDPA_STORY.length) {
                        say(s, p, GRANDPA_STORY[s.stage]);
                        if (m != null && m.getDistanceSq(p) <= 5 * 5) p.giveExperiencePoints(3);
                        s.stage++;
                        s.nextStep = now + 100;
                    } else {
                        p.giveExperiencePoints(10);
                        note(p, "§aДед доволен. Вы получили немного опыта (жизненного).");
                        leave(s, now, 100);
                    }
                }
                break;
            case PHILOSOPHER:
                if (now >= s.nextStep) {
                    if (s.stage < 5) {
                        say(s, p, String.format(PHILOSOPHY[s.stage], Finance.money(totalDebt(a))));
                        s.stage++;
                        s.nextStep = now + 100;
                    } else {
                        say(s, p, "Я ухожу. Но долг — остаётся. Как и всё настоящее.");
                        leave(s, now, 100);
                    }
                }
                break;
            case SOCIOLOGIST:
                if (s.stage >= 1 && now >= s.nextStep) {
                    String[] q = {
                            "Вопрос 1: Довольны ли вы ставкой по ипотеке? §8(записали: «очень»)",
                            "Вопрос 2: Сколько у вас кредитов? §8(записали: «достаточно»)",
                            "Вопрос 3: Верите ли вы в стабильность? §8(записали: «безусловно»)"};
                    if (s.stage <= 3) {
                        say(s, p, q[s.stage - 1]);
                        s.stage++;
                        s.nextStep = now + 60;
                    } else {
                        BankService.giveCash(p, 50);
                        say(s, p, "Спасибо! Итоги опроса: 146% граждан всем довольны. Ваши 50₽.");
                        leave(s, now, 80);
                    }
                }
                break;
            case LOST_CAVER:
                if (s.stage == 1 && m != null) {
                    if (m.getDistanceSq(p) < 12 * 12 && w.canSeeSky(m.getPosition().up())) {
                        int pay = 150 + R.nextInt(6) * 50;
                        BankService.giveCash(p, pay);
                        BankService.give(p, named(Items.LEATHER_HELMET, 1, "§eКаска спелеолога", "§7Подарок от Васи. Фонарик сел."));
                        p.giveExperiencePoints(15);
                        say(s, p, "§aСОЛНЦЕ!!! §fСпасибо, спаситель! Вот " + Finance.money(pay) + " и моя каска. Больше — ни ногой под землю!");
                        leave(s, now, 100);
                    } else if (now >= s.nextStep) {
                        say(s, p, "Ладно... Пойду по сквозняку. Говорят, он всегда к выходу. Спасибо, что пытался.");
                        leave(s, now, 80);
                    } else if ((now - s.start) % 600 == 0) {
                        say(s, p, pick("Ты уверен, что нам туда?", "Тут уже были... или нет? Всё одинаковое!", "Это вода капает или мне кажется?",
                                "А тут криперы водятся? Чисто для справки.", "Свет! Это свет? А, это лава. Обходим."));
                    }
                }
                break;
            case HITCHHIKER:
                if (s.stage == 1 && m != null && !s.spots.isEmpty()) {
                    BlockPos from = s.spots.get(0);
                    double dx = p.getPosX() - from.getX(), dz = p.getPosZ() - from.getZ();
                    if (dx * dx + dz * dz >= 200 * 200 && m.getDistanceSq(p) < 16 * 16) {
                        int pay = 200 + R.nextInt(5) * 50;
                        BankService.giveCash(p, pay);
                        BankService.give(p, pick(named(Items.PAPER, 1, "§bМагнитик «Владивосток»", "§7Гоша там ещё не был, но магнитик уже купил"),
                                new ItemStack(Items.COOKED_PORKCHOP, 4), named(Items.MAP, 1, "§bАтлас автодорог 1987 г.")));
                        say(s, p, "Вот тут я и сойду! Держи " + Finance.money(pay) + " и сувенир. Если будешь во Владике — я там буду. Когда-нибудь.");
                        leave(s, now, 100);
                    } else if (now >= s.nextStep) {
                        BankService.giveCash(p, 50);
                        say(s, p, "Ты как-то кругами ходишь... Ладно, дальше сам. Держи полтинник за компанию.");
                        leave(s, now, 80);
                    } else if ((now - s.start) % 500 == 0) {
                        say(s, p, pick("А вот однажды меня подвозил дальнобойщик Толя. Три дня ехали, он всё про жену рассказывал...",
                                "Знаешь, почему я автостопом? Потому что ипотека. Квартиру сдал — и в путь!",
                                "Ещё пару километров — и будет кафе «У Ашота». Там шаурма... ну, не отравишься.",
                                "Главное правило трассы: никогда не садись к конокраду.",
                                "Уже " + (int) Math.sqrt(dx * dx + dz * dz) + " блоков! До Владивостока осталось... много."));
                    }
                }
                break;
            default:
                if (now - s.start > 1200) leave(s, now, 60);
        }
    }

    private static void taxCheck(Session s, ServerPlayerEntity p, Account a, int mult) {
        World w = p.world;
        double value = houseOk(a) ? houseValue(w, a.houseMin, a.houseMax) : 0;
        int score = CreditRating.score(a, BankService.today(p.getServer()));
        double limit = taxLimit(score);
        if (value <= limit) {
            say(s, p, "Имущество на " + Finance.money(value) + " при лимите " + Finance.money(limit) + ". Всё чисто. Хорошего дня!");
            return;
        }
        double fine = Math.ceil((value - limit) * 0.15 * mult / 10.0) * 10.0;
        forceCharge(p, a, fine);
        ItemStack conf = confiscateBest(w, a.houseMin, a.houseMax);
        say(s, p, "§cИмущество на " + Finance.money(value) + " при лимите " + Finance.money(limit) + " (рейтинг " + score + "). "
                + "Штраф " + Finance.money(fine) + (mult > 1 ? " (двойной)" : "") + "."
                + (conf.isEmpty() ? "" : " И изымаем: " + conf.getDisplayName().getString() + " ×" + conf.getCount() + "."));
    }

    private static void fireInspector(Session s, ServerPlayerEntity p, Account a, long now) {
        World w = p.world;
        if (s.stage == 1 && now >= s.nextStep) {
            int furn = houseOk(a) ? countFurnaces(w, a.houseMin, a.houseMax) : 0;
            s.price = Math.max(0, furn - 3) * 50;
            s.spots = houseOk(a) ? findLava(w, a.houseMin, a.houseMax, 64) : new ArrayList<>();
            if (furn > 3) say(s, p, "Печей: " + furn + ". По нормам — не больше трёх. Штраф 50₽ за каждую лишнюю: " + Finance.money(s.price) + ".");
            if (!s.spots.isEmpty()) {
                say(s, p, "§c§lЛАВА В ЖИЛОМ ДОМЕ?! §fВызываю расчёт!");
                for (int i = 0; i < 2; i++) {
                    VisitorEntity f = spawn(s, p.getServerWorld(), p, VisitorKind.FIREMAN, 3, 6);
                    if (f != null) {
                        f.mode = VisitorEntity.GOTO;
                        f.gotoPos = s.spots.get(0);
                    }
                }
                s.stage = 2;
                s.nextStep = now + 100;
            } else {
                forceCharge(p, a, s.price);
                if (furn <= 3) say(s, p, "Нарушений нет. Огнетушитель купите. Всего доброго!");
                leave(s, now, 100);
            }
        } else if (s.stage == 2 && now >= s.nextStep) {
            int n = 0;
            for (BlockPos pos : s.spots) {
                if (w.getFluidState(pos).isTagged(FluidTags.LAVA)) {
                    w.setBlockState(pos, Blocks.WATER.getDefaultState(), 3);
                    n++;
                }
            }
            for (VisitorEntity e : s.npcs) if (e.getKind() == VisitorKind.FIREMAN) sayAs(VisitorKind.FIREMAN, p, "Тушим! Вода пошла!");
            double fine = s.price + 300;
            forceCharge(p, a, fine);
            say(s, p, "Залили" + (n > 0 ? " " + n + " блок(ов) лавы" : "") + ". Штраф: " + Finance.money(fine) + " (300₽ — за лаву в доме).");
            leave(s, now, 100);
        }
    }

    // =====================================================================================
    //  v1.9: страшные события «здесь и сейчас»
    // =====================================================================================

    private static void sound(ServerPlayerEntity p, BlockPos at, net.minecraft.util.SoundEvent ev, float pitch) {
        p.world.playSound(null, at, ev, SoundCategory.BLOCKS, 1.0F, pitch);
    }

    private static String where(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static boolean startScary(Session s, ServerPlayerEntity p, Account a, ServerWorld w, long now) {
        EconomyData data = EconomyData.get(p.getServer());
        s.world = w;
        switch (s.kind) {
            case SANEPID: {
                LegalBusiness b = nearBiz(p, a, false);
                if (b == null) return false;
                s.biz = b;
                if (spawn(s, w, p, s.kind, 3, 6) == null) return false;
                ask(s, p, pick("Поступила жалоба: покупатель отравился в «" + b.type.title + "».",
                        "Анонимка на «" + b.type.title + "»: крысы, грязь, продавцы без медкнижек.",
                        "Внеплановая проверка «" + b.type.title + "». Почему внеплановая? Потому что можем.")
                        + " §fСанэпидемстанция! Проводите в заведение: смотрим отделку, свет, паутину, раковину, просрочку и медкнижки.", now);
                return true;
            }
            case MIGRATION: {
                LegalBusiness b = nearBiz(p, a, true);
                if (b == null) return false;
                s.biz = b;
                if (spawn(s, w, p, s.kind, 3, 6) == null) return false;
                spawn(s, w, p, VisitorKind.TAX_POLICE, 3, 6);
                title(p, "§9§lРЕЙД", "§7Миграционная служба в «" + b.type.title + "»");
                ask(s, p, "Управление по вопросам миграции! Рейд по «" + b.type.title + "»: у вас работает " + b.workers.size()
                        + " чел. Предъявите патенты и медкнижки! §7([Да] — показать документы, [Нет] — «никого нет, это родственники»)", now);
                return true;
            }
            case FIRE: {
                List<BlockPos[]> boxes = Inspections.homeBoxes(p, a, data, 48);
                Collections.shuffle(boxes, R);
                for (BlockPos[] bx : boxes) {
                    List<BlockPos> spots = Inspections.fireSpots(w, bx[0], bx[1], 2 + R.nextInt(2), R, data);
                    if (spots.isEmpty()) continue;
                    s.noNpc = true;
                    s.box = new BlockPos[]{bx[0].add(-6, -3, -6), bx[1].add(6, 6, 6)};
                    Inspections.ignite(w, spots);
                    s.target = spots.get(0);
                    s.stage = 1;
                    s.nextStep = now + 1200;
                    title(p, "§4§lПОЖАР!", "§cКороткое замыкание — 60 секунд, чтобы потушить");
                    note(p, "§4§l[!] ПОЖАР! §cЗакоротило проводку — горит у вас около §e" + where(s.target) + "§c (" + spots.size()
                            + " очага). §fТушите водой или ломайте огонь! Через 60 секунд приедут пожарные: штраф 1500₽ и по 100₽ за каждый очаг.");
                    sound(p, s.target, SoundEvents.BLOCK_BELL_USE, 0.6F);
                    return true;
                }
                return false;
            }
            case CAVE_IN: {
                if (!underground(p)) return false;
                List<BlockPos> bl = Inspections.caveIn(w, p.getPosition(), data);
                if (bl.size() < 5) return false;
                s.noNpc = true;
                s.spots = bl;
                s.stage = 1;
                s.nextStep = now + 60;
                title(p, "§6§lОБВАЛ!", "§eПорода трещит — бегите!");
                note(p, "§6§l[!] §eС потолка сыплется пыль, крепь трещит... §cУ вас 3 секунды, чтобы отбежать!");
                sound(p, p.getPosition().up(3), SoundEvents.BLOCK_GRAVEL_BREAK, 0.4F);
                return true;
            }
            case BURGLAR: {
                BlockPos chest = Inspections.burglarTarget(w, Inspections.homeBoxes(p, a, data, 64), R);
                if (chest == null) return false;
                VisitorEntity e = spawn(s, w, p, s.kind, 3, 6);
                if (e == null) return false;
                BlockPos from = DebtManager.standNear(w, chest.add(R.nextInt(13) - 6, 0, R.nextInt(13) - 6), 4);
                if (from == null) from = DebtManager.standNear(w, chest, 5);
                if (from != null) e.setPositionAndUpdate(from.getX() + 0.5, from.getY(), from.getZ() + 0.5);
                BlockPos to = DebtManager.standNear(w, chest, 2);
                e.mode = VisitorEntity.GOTO;
                e.gotoPos = to != null ? to : chest;
                s.target = chest;
                title(p, "§8§lСИГНАЛИЗАЦИЯ", "§7В вашем доме чужой...");
                note(p, "§c§l[!] Сработала сигнализация: §fдомушник лезет к сундуку у §e" + where(chest)
                        + "§f! Ударьте его, пока он не вскрыл замок, — или прощайтесь с самым ценным.");
                sound(p, p.getPosition(), SoundEvents.BLOCK_BELL_USE, 1.4F);
                return true;
            }
            default:
                return false;
        }
    }

    /** Убрать n работников (депортация). */
    private static int deport(ServerWorld w, LegalBusiness b, int n) {
        int done = 0;
        while (done < n && !b.workers.isEmpty()) {
            UUID u = b.workers.remove(b.workers.size() - 1);
            Entity e = w.getEntityByUuid(u);
            if (e != null) e.remove();
            done++;
        }
        b.docsWorkers = Math.min(b.docsWorkers, b.workers.size());
        return done;
    }

    private static void answerScary(Session s, ServerPlayerEntity p, Account a, boolean yes, long now) {
        MinecraftServer server = p.getServer();
        if (server == null || s.biz == null) { leave(s, now, 60); return; }
        long today = BankService.today(server);
        ServerWorld w = p.getServerWorld();
        LegalBusiness b = s.biz;
        if (s.kind == VisitorKind.SANEPID) {
            if (s.stage == 0) {
                if (yes) {
                    VisitorEntity m = s.main();
                    BlockPos at = DebtManager.standNear(w, b.counter, 4);
                    if (m != null && at != null) {
                        m.setPositionAndUpdate(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                        m.mode = VisitorEntity.STAY;
                    }
                    say(s, p, "Так-с... §8*надевает перчатки, достаёт фонарик и люксметр*");
                    note(p, "§7Инспектор осматривает «" + b.type.title + "». Акт — через 5 секунд.");
                    s.stage = 1;
                    s.nextStep = now + 100;
                } else {
                    forceCharge(p, a, 2000);
                    Inspections.seal(b, today, 1, "отказ в допуске СЭС");
                    say(s, p, "§cВоспрепятствование проверке! Штраф 2000₽, заведение опечатано на сутки.");
                    leave(s, now, 100);
                }
            } else if (s.stage == 2) {
                int half = s.price / 2;
                if (yes && BankService.charge(p, a, half)) {
                    if (R.nextDouble() < 0.3) {
                        forceCharge(p, a, s.price * 2);
                        Inspections.seal(b, today, 3, "взятка инспектору СЭС");
                        say(s, p, "§4Контрольная закупка! §cПопытка дать взятку: штраф в двойном размере (" + Finance.money(s.price * 2)
                                + "), заведение опечатано на 3 дня.");
                    } else {
                        say(s, p, "Я ничего не видела. Но чтоб к следующему разу — чисто, светло и с медкнижками!");
                    }
                } else {
                    forceCharge(p, a, s.price);
                    boolean seal = s.count >= 3;
                    if (seal) Inspections.seal(b, today, 2, "нарушения СЭС");
                    say(s, p, "§cАкт составлен. Штраф " + Finance.money(s.price) + (seal ? ". Нарушений " + s.count + " — опечатываю на 2 дня." : ". Исправляйте."));
                }
                leave(s, now, 100);
            }
            return;
        }
        // миграционная служба
        int undoc = Inspections.undocumented(b, today);
        if (yes) {
            if (undoc == 0) {
                say(s, p, "Патенты, медкнижки, регистрация... Всё в порядке. Работайте.");
            } else {
                int fine = 3000 * undoc;
                forceCharge(p, a, fine);
                int d = deport(w, b, undoc);
                say(s, p, "§cУ " + undoc + " работник(ов) нет документов! §fШтраф " + Finance.money(fine) + ", " + d
                        + " чел. — на депортацию. Нанимайте новых и оформляйте как положено.");
            }
        } else if (R.nextDouble() < 0.4) {
            say(s, p, "Родственники, говорите... Ладно. Но мы ещё вернёмся.");
        } else if (undoc > 0) {
            int fine = 6000 * undoc;
            forceCharge(p, a, fine);
            int d = deport(w, b, undoc);
            say(s, p, "§4Нашли в подсобке " + undoc + " нелегал(ов)! §cШтраф в двойном размере: " + Finance.money(fine) + ", " + d + " чел. депортировано.");
        } else {
            forceCharge(p, a, 1500);
            say(s, p, "§cДокументы-то у вас есть, а врать зачем? Воспрепятствование — 1500₽.");
        }
        EconomyData.get(server).markDirty();
        leave(s, now, 100);
    }

    private static void secondScary(Session s, ServerPlayerEntity p, Account a, ServerWorld w, long now) {
        switch (s.kind) {
            case SANEPID: {
                if (s.stage != 1 || now < s.nextStep || s.biz == null) return;
                long today = BankService.today(p.getServer());
                Inspections.Report r = Inspections.sanepid(w, s.biz, today, true);
                if (r.violations == 0) {
                    say(s, p, "§aЧисто, светло, раковина есть, медкнижки в порядке. Нарушений нет. Работайте!");
                    leave(s, now, 100);
                    return;
                }
                say(s, p, "§cАкт проверки «" + s.biz.type.title + "»:");
                for (String l : r.lines) note(p, "§7 • §f" + l);
                s.price = r.fine;
                s.count = r.violations;
                s.stage = 2;
                ask(s, p, "Итого " + Finance.money(r.fine) + (r.violations >= 3 ? " и опечатывание на 2 дня" : "")
                        + ". Хотя... можно и по-хорошему: половину — мне, и акт я «потеряю». Договоримся?", now);
                return;
            }
            case FIRE: {
                if (s.stage != 1 || s.box == null) return;
                int left = Inspections.countFire(w, s.box[0], s.box[1]);
                if (left == 0) {
                    note(p, "§a§lПожар потушен! §aУспели до приезда пожарных — ни штрафа, ни протокола.");
                    s.stage = 3;
                    finish(s, now);
                    return;
                }
                if (now >= s.nextStep) {
                    for (int i = 0; i < 2; i++) {
                        VisitorEntity f = spawn(s, w, p, VisitorKind.FIREMAN, 3, 6);
                        if (f != null) { f.mode = VisitorEntity.GOTO; f.gotoPos = s.target; }
                    }
                    int put = Inspections.extinguish(w, s.box[0], s.box[1]);
                    double fine = 1500 + 100.0 * put;
                    forceCharge(p, a, fine);
                    s.stage = 2;
                    sayAs(VisitorKind.FIREMAN, p, "§cПожарная охрана! Потушили " + put + " очаг(ов). §fШтраф и выезд: " + Finance.money(fine)
                            + ". Огнетушитель — это ведро воды, хозяин.");
                    EconomyData.get(p.getServer()).markDirty();
                    if (s.npcs.isEmpty()) finish(s, now);
                    else leave(s, now, 200);
                } else if ((now - s.start) % 200 == 0) {
                    note(p, "§c§l[!] §cГорит: " + left + " блок(ов) огня. До приезда пожарных " + (s.nextStep - now) / 20 + " с.");
                    sound(p, s.target, SoundEvents.BLOCK_BELL_USE, 0.6F);
                }
                return;
            }
            case CAVE_IN: {
                if (now < s.nextStep) return;
                int n = Inspections.collapse(w, s.spots);
                sound(p, p.getPosition().up(2), SoundEvents.BLOCK_GRAVEL_BREAK, 0.3F);
                boolean under = false;
                for (BlockPos q : s.spots) if (Math.abs(q.getX() - p.getPosX() + 0.5) <= 1 && Math.abs(q.getZ() - p.getPosZ() + 0.5) <= 1) under = true;
                note(p, n == 0 ? "§7Потрещало и затихло. Повезло." : "§6*ГРОХОТ* §eОбрушилось " + n + " блоков породы."
                        + (under ? " §cВас завалило — копайте наружу!" : " §aВы успели отскочить."));
                finish(s, now);
                return;
            }
            case BURGLAR: {
                VisitorEntity m = s.main();
                if (m == null || s.target == null) return;
                if (s.stage == 0) {
                    if (m.getDistanceSq(s.target.getX() + 0.5, s.target.getY() + 0.5, s.target.getZ() + 0.5) <= 3.0 * 3.0) {
                        s.stage = 1;
                        s.nextStep = now + 120;
                        note(p, "§c*Скрежет отмычки...* §fДомушник вскрывает сундук у §e" + where(s.target) + "§f — 6 секунд!");
                        sound(p, s.target, SoundEvents.BLOCK_CHEST_LOCKED, 0.8F);
                    } else if (now - s.start > 1800) {
                        BlockPos at = DebtManager.standNear(w, s.target, 2);
                        if (at != null) m.setPositionAndUpdate(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                    }
                } else if (s.stage == 1 && now >= s.nextStep) {
                    ItemStack st = confiscateBest(w, s.target, s.target);
                    if (st.isEmpty()) {
                        say(s, p, "Пусто... тьфу ты!");
                        leave(s, now, 60);
                        return;
                    }
                    s.offer = st;
                    s.stage = 2;
                    s.nextStep = now + 400;
                    m.mode = VisitorEntity.FLEE;
                    note(p, "§4§lДомушник украл: §c" + st.getDisplayName().getString() + " ×" + st.getCount() + "§4§l! §fДогоните и ударьте — 20 секунд.");
                    sound(p, s.target, SoundEvents.BLOCK_CHEST_OPEN, 0.7F);
                } else if (s.stage == 2 && now >= s.nextStep) {
                    note(p, "§8Домушник растворился в ночи вместе с «" + s.offer.getDisplayName().getString() + "». Поставьте замок понадёжнее... или дверь.");
                    s.offer = ItemStack.EMPTY;
                    leave(s, now, 20);
                }
                return;
            }
            default:
        }
    }

    private static void horseThief(Session s, ServerPlayerEntity p, long now) {
        VisitorEntity thief = s.main();
        AbstractHorseEntity horse = s.horse;
        if (horse == null || !horse.isAlive()) {
            finish(s, now);
            return;
        }
        if (thief == null || !thief.isAlive()) {
            note(p, "§aКонокрад повержен. Лошадь ваша!");
            finish(s, now);
            return;
        }
        if (!thief.isPassenger()) return; // спрыгнул — убегает (leave уже назначен)
        if (horse.getDistanceSq(p) > 80 * 80 || now - s.start > 1800) {
            horse.remove();
            note(p, "§cЛошадь угнали. Объявление о пропаже можете повесить на столб.");
            finish(s, now);
        }
    }

    // =====================================================================================
    //  события мира
    // =====================================================================================

    /** Урон по гостю. true — отменить урон. */
    public static boolean onHurt(VisitorEntity e, DamageSource src) {
        if (e.getKind() == VisitorKind.OBEP) return src != DamageSource.OUT_OF_WORLD;
        Session s = sessionOf(e);
        if (s == null || e.world.getServer() == null) return false;
        Entity attacker = src.getTrueSource();
        if (!(attacker instanceof ServerPlayerEntity)) return e.getKind() == VisitorKind.PROMOTER;
        ServerPlayerEntity p = (ServerPlayerEntity) attacker;
        long now = EconomyData.overworld(e.world.getServer()).getGameTime();
        switch (e.getKind()) {
            case PROMOTER:
                DebtManager.teleportNear(e, p, 4);
                sayAs(VisitorKind.PROMOTER, p, "Мимо! А процент — нет!");
                return true;
            case HORSE_THIEF:
                if (e.isPassenger()) {
                    e.stopRiding();
                    sayAs(VisitorKind.HORSE_THIEF, p, "Ай! Всё-всё, забирай свою клячу!");
                    note(p, "§aВы отбили лошадь! Конокрад удирает пешком.");
                    leave(s, now, 160);
                }
                return false;
            case GOPNIK:
            case GOPNIK_FRIEND:
                if (s.stage < 2 && s.leaveAt < 0) {
                    if (s.npcs.size() < 3) {
                        spawn(s, p.getServerWorld(), p, VisitorKind.GOPNIK_FRIEND, 3, 5);
                        spawn(s, p.getServerWorld(), p, VisitorKind.GOPNIK_FRIEND, 3, 5);
                    }
                    say(s, p, "§cТы чё, бессмертный?!");
                    fight(s, p, now);
                }
                return false;
            case GRANNY:
                if (s.leaveAt < 0) {
                    say(s, p, "Ой! Хулиган! Вот я твоей матери расскажу!");
                    leave(s, now, 60);
                }
                return false;
            case LOST_CAVER:
            case HITCHHIKER:
            case HOBO:
            case PEDDLER:
            case TREASURE_HUNTER:
            case BLACK_DIGGER:
                if (s.leaveAt < 0) {
                    say(s, p, pick("§cАй! Ты чего?!", "§cПсих! Помогите!", "§cВсё-всё, ухожу!"));
                    leave(s, now, 60);
                }
                return false;
            case TAX:
            case TAX_POLICE:
            case BAILIFF:
            case BURGLAR:
                if (!s.offer.isEmpty()) {
                    BankService.give(p, s.offer.copy());
                    s.offer = ItemStack.EMPTY;
                    sayAs(VisitorKind.BURGLAR, p, "§cАй! Всё-всё, забирай! §8*бросает добычу и убегает*");
                    note(p, "§aВы отбили украденное!");
                } else if (s.leaveAt < 0) {
                    sayAs(VisitorKind.BURGLAR, p, "§cШухер! §8*убегает*");
                    note(p, "§aВы спугнули домушника — сундук цел.");
                }
                if (s.leaveAt < 0) leave(s, now, 60);
                return false;
            case FIRE_INSPECTOR:
            case FIREMAN:
            case UCHASTKOVY:
            case ECO_INSPECTOR:
            case DPS:
            case SANEPID:
            case MIGRATION:
                if (!s.fined) {
                    s.fined = true;
                    Account a = EconomyData.get(p.getServer()).get(p);
                    forceCharge(p, a, 500);
                    EconomyData.get(p.getServer()).markDirty();
                    sayAs(e.getKind(), p, "§4Нападение на должностное лицо при исполнении! Штраф 500₽.");
                }
                return false;
            default:
                return false;
        }
    }

    /** ПКМ по гостю = «да». */
    public static void onInteract(ServerPlayerEntity p, VisitorEntity e) {
        Session s = sessionOf(e);
        if (s == null) return;
        if (!s.player.equals(p.getUniqueID())) {
            sayAs(e.getKind(), p, "Я не к вам.");
            return;
        }
        if (s.asking) answer(p, true);
        else if (s.kind == VisitorKind.GRANDPA) sayAs(e.getKind(), p, "Не перебивай деда!");
        else if (s.kind == VisitorKind.PROMOTER) sayAs(e.getKind(), p, "Вот визитка! Звоните! §8*суёт листовку*");
        else if (s.kind == VisitorKind.LOST_CAVER) sayAs(e.getKind(), p, "Я тут, я иду! Только к небу, пожалуйста.");
        else if (s.kind == VisitorKind.HITCHHIKER) sayAs(e.getKind(), p, "Идём-идём! Ещё немного — и я сойду.");
    }

    public static void onBreak(ServerPlayerEntity p, BlockPos pos, BlockState state) {
        if (BOXES.isEmpty() || p.getServer() == null) return;
        long now = EconomyData.overworld(p.getServer()).getGameTime();
        for (NeighborBox b : BOXES) {
            if (b.fined || now > b.until || !b.player.equals(p.getUniqueID()) || !b.contains(pos)) continue;
            b.fined = true;
            Account a = EconomyData.get(p.getServer()).get(p);
            forceCharge(p, a, 200);
            EconomyData.get(p.getServer()).markDirty();
            sayAs(VisitorKind.NEIGHBOR, p, "§cТы чё мою пристройку ломаешь?! Заявление участковому! §fШтраф 200₽.");
            return;
        }
    }

    /** Съели пирожок бабушки Зины — сюрприз. */
    public static void onEaten(LivingEntity who, ItemStack st) {
        if (!(who instanceof ServerPlayerEntity) || !st.hasTag() || !st.getTag().contains(SURPRISE_TAG)) return;
        PlayerEntity p = (PlayerEntity) who;
        switch (st.getTag().getInt(SURPRISE_TAG)) {
            case 1:
                p.addPotionEffect(new EffectInstance(Effects.POISON, 200, 0));
                note(p, "§d*В пирожке оказался сюрприз: ливер был не первой свежести.*");
                break;
            case 2:
                p.addPotionEffect(new EffectInstance(Effects.HUNGER, 600, 1));
                note(p, "§d*Сюрприз: пирожок пустой внутри. Есть хочется ещё сильнее.*");
                break;
            case 3:
                p.addPotionEffect(new EffectInstance(Effects.REGENERATION, 200, 1));
                note(p, "§d*Сюрприз: бабушкина любовь! Вам стало лучше.*");
                break;
            default:
                p.addPotionEffect(new EffectInstance(Effects.LEVITATION, 100, 0));
                note(p, "§d*Сюрприз: в пирожке были «особые» грибы. Вы взлетаете.*");
        }
    }

    public static List<String> eventIds() {
        List<String> l = new ArrayList<>();
        for (VisitorKind k : eventKinds()) l.add(k.id());
        return l;
    }

    /** Для самотеста. */
    public static List<String> kindIds() {
        List<String> l = new ArrayList<>();
        for (VisitorKind k : VisitorKind.values()) l.add(k.id());
        return l;
    }

    static List<VisitorKind> all() {
        return Arrays.asList(VisitorKind.values());
    }
}
