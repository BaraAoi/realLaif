package com.baraaoi.reallife.event;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.economy.Business;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.entity.DebtNpcEntity;
import com.baraaoi.reallife.entity.VisitorEntity;
import com.baraaoi.reallife.entity.VisitorKind;
import com.baraaoi.reallife.registry.ModBlocks;
import com.baraaoi.reallife.registry.ModEntities;
import com.baraaoi.reallife.structure.Blueprint;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Станки надо прятать. Прохожие (жители, гости, коллекторы, «подозрительные типы») видят станок
 * по прямой — и стучат. Через 1–3 минуты приходит ОБЭП и копает тоннель прямо к месту наводки,
 * даже сквозь стены. Нашли — станки изъяты, штраф, СИЗО. Не нашли — всё восстанавливают и извиняются.
 */
public final class Raids {
    private Raids() {}

    public static final String OBEP = "§9§l[ОБЭП] §r§f";
    private static final Random R = new Random();
    /** Максимум шагов тоннеля. */
    private static final int MAX_STEPS = 96;
    private static final long RAID_TIMEOUT = 3600;

    static final class Tip {
        final UUID player;
        final BlockPos pos;
        final long raidAt;

        Tip(UUID player, BlockPos pos, long raidAt) {
            this.player = player;
            this.pos = pos;
            this.raidAt = raidAt;
        }
    }

    static final class Raid {
        final UUID player;
        final BlockPos target;
        final List<VisitorEntity> opers = new ArrayList<>();
        final ListTag snapshot = new ListTag();
        BlockPos cur;
        int steps;
        long start;
        long nextStep;
        boolean done;

        Raid(UUID player, BlockPos target) {
            this.player = player;
            this.target = target;
        }
    }

    private static final Map<UUID, Tip> TIPS = new HashMap<>();
    private static final Map<UUID, Raid> RAIDS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_SNOOP = new HashMap<>();

    // =====================================================================================
    //  видимость
    // =====================================================================================

    /** Видно ли центр блока target из точки from (непрозрачные блоки загораживают, стекло — нет). */
    public static boolean canSee(Level w, Vec3 from, BlockPos target) {
        Vec3 to = new Vec3(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1e-3) return true;
        int n = (int) Math.ceil(len / 0.25);
        BlockPos start = BlockPos.containing(from);
        BlockPos last = null;
        for (int i = 1; i < n; i++) {
            Vec3 v = from.add(d.scale(i / (double) n));
            BlockPos bp = BlockPos.containing(v);
            if (bp.equals(last) || bp.equals(start)) continue;
            last = bp;
            if (bp.equals(target)) return true;
            BlockState st = w.getBlockState(bp);
            if (st.isSolidRender(w, bp)) return false;
        }
        return true;
    }

    private static boolean isObserver(Entity e) {
        if (e instanceof com.baraaoi.reallife.entity.SnoopEntity) return true;
        if (e instanceof VisitorEntity) return ((VisitorEntity) e).getKind() != VisitorKind.OBEP;
        return e instanceof AbstractVillager || e instanceof DebtNpcEntity;
    }

    
    private static Entity spotter(ServerLevel w, BlockPos printer) {
        int r = Config.SPOT_RANGE.get();
        AABB box = new AABB(printer).grow(r);
        for (Entity e : w.getEntitiesOfClass(Entity.class, box, Raids::isObserver)) {
            if (!e.isAlive() || e.distanceToSqr(printer.getX() + 0.5, printer.getY() + 0.5, printer.getZ() + 0.5) > r * r) continue;
            if (canSee(w, e.getEyePosition(1.0F), printer)) return e;
        }
        return null;
    }

    private static boolean isPrinter(Level w, BlockPos p) {
        return w.getBlockState(p).getBlock() == ModBlocks.MONEY_PRINTER.get();
    }

    // =====================================================================================
    //  тик
    // =====================================================================================

    public static long snoopInterval(int printers) {
        long base = (long) (12000 / (0.6 + 0.4 * Math.max(1, printers)));
        base = (long) (base * (0.7 + R.nextDouble() * 0.6));
        return Math.max(1800L, base);
    }

    static long snoopIntervalFor(Account a) {
        if (!a.printerList.isEmpty()) return snoopInterval(a.printerList.size());
        return 18000L + R.nextInt(18000);
    }

    /** Сколько «подозрительных» уже ходит вокруг игрока. */
    public static int snoopsNear(ServerLevel w, ServerPlayer p) {
        return w.getEntitiesOfClass(com.baraaoi.reallife.entity.SnoopEntity.class, p.getBoundingBox().inflate(96, 64, 96), Entity::isAlive).size();
    }

    public static void tick(MinecraftServer server) {
        ServerLevel ow = EconomyData.overworld(server);
        if (ow == null) return;
        long now = ow.getGameTime();
        if (now % 20 != 0) return;
        EconomyData data = EconomyData.get(server);

        // рейды
        for (Raid r : new ArrayList<>(RAIDS.values())) {
            try {
                stepRaid(server, ow, data, r, now);
            } catch (RuntimeException ex) {
                RealLife.LOG.error("Real Life: raid failed", ex);
                abort(ow, r, null);
            }
        }
        // наводки → рейд
        Iterator<Tip> ti = TIPS.values().iterator();
        while (ti.hasNext()) {
            Tip t = ti.next();
            if (now < t.raidAt) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(t.player);
            if (p == null) continue; // ждём, пока зайдёт
            Account a = data.get(p);
            if (a.captivity != Account.FREE || p.level() != ow || RAIDS.containsKey(t.player)) continue;
            ti.remove();
            startRaid(ow, p, t.pos, now);
        }

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level() != ow || p.isSpectator()) continue;
            Account a = data.get(p);
            if (a.captivity != Account.FREE) continue;
            UUID id = p.getUUID();
            // кто-то увидел станок?
            if (!a.printerList.isEmpty() && !TIPS.containsKey(id) && !RAIDS.containsKey(id)) {
                for (BlockPos pos : new ArrayList<>(a.printerList)) {
                    if (!ow.isLoaded(pos) || !isPrinter(ow, pos)) continue;
                    Entity who = spotter(ow, pos);
                    if (who == null) continue;
                    TIPS.put(id, new Tip(id, pos.immutable(), now + 1200 + R.nextInt(2401)));
                    String name = who.getDisplayName().getString();
                    BankService.msg(p, "§8[Слухи] §7" + name + "§7 заметил твой печатный станок и уже куда-то звонит... "
                            + "§cЖди ОБЭП через пару минут. §7Убери станок или замуруй — но по наводке всё равно будут копать.");
                    break;
                }
            }
            // подозрительный тип: к владельцам станков — чаще, к остальным — изредка, просто «погулять»
            Long next = NEXT_SNOOP.get(id);
            if (next == null) { NEXT_SNOOP.put(id, now + snoopIntervalFor(a)); continue; }
            if (now < next) continue;
            NEXT_SNOOP.put(id, now + snoopIntervalFor(a));
            if (RAIDS.containsKey(id)) continue;
            if (snoopsNear(ow, p) == 0) spawnSnoop(ow, p, a, now);
        }
    }

    // =====================================================================================
    //  подозрительный тип
    // =====================================================================================

    
    private static BlockPos nearestPrinter(ServerLevel w, ServerPlayer p, Account a) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos pos : a.printerList) {
            if (!w.isLoaded(pos)) continue;
            double d = pos.distanceSq(p.getX(), p.getY(), p.getZ(), true);
            if (d < bd) { bd = d; best = pos; }
        }
        return best;
    }

    /** Точка на поверхности в кольце minR..maxR вокруг center. */
    static BlockPos surfaceNear(ServerLevel w, BlockPos center, int minR, int maxR) {
        for (int i = 0; i < 24; i++) {
            double ang = R.nextDouble() * Math.PI * 2;
            double dist = minR + R.nextDouble() * (maxR - minR);
            int x = center.getX() + (int) Math.round(Math.cos(ang) * dist);
            int z = center.getZ() + (int) Math.round(Math.sin(ang) * dist);
            if (!w.isLoaded(new BlockPos(x, 64, z))) continue;
            int y = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            if (Math.abs(y - center.getY()) > 24) continue;
            if (w.getBlockState(p.below()).liquid()) continue;
            if (data(w).isProtected(p)) continue;
            return p;
        }
        return center.above(2);
    }

    private static EconomyData data(ServerLevel w) {
        return EconomyData.get(w.getServer());
    }

    /** Выпустить подозрительного типа: в 16–28 блоках от ближайшего станка (или дома, или самого игрока). */
    
    public static com.baraaoi.reallife.entity.SnoopEntity spawnSnoop(ServerLevel w, ServerPlayer p, Account a, long now) {
        BlockPos printer = nearestPrinter(w, p, a);
        BlockPos anchor = printer;
        if (anchor == null && a.hasHouse && !a.houseMin.equals(BlockPos.ZERO) && w.isLoaded(a.houseMin)
                && a.houseMin.distanceSq(p.blockPosition()) < 64 * 64) anchor = a.houseMin;
        if (anchor == null) anchor = p.blockPosition();
        com.baraaoi.reallife.entity.SnoopEntity e = ModEntities.SNOOP.get().create(w, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (e == null) return null;
        BlockPos at = surfaceNear(w, anchor, 16, 28);
        e.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, R.nextFloat() * 360F, 0F);
        e.targetPlayer = p.getUUID();
        e.life = 6000;
        w.addFreshEntity(e);
        if (printer != null) BankService.actionBar(p, "§8Возле твоих станков крутится какой-то подозрительный тип...");
        return e;
    }

    // =====================================================================================
    //  рейд
    // =====================================================================================

    public static boolean raidActive(UUID player) {
        return RAIDS.containsKey(player);
    }

    /** Админ-команда: прислать подозрительного типа или сразу ОБЭП к ближайшему станку. */
    public static String adminStart(ServerPlayer p, boolean obep) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return "нет сервера";
        ServerLevel ow = EconomyData.overworld(server);
        if (p.level() != ow) return "только в обычном мире";
        Account a = EconomyData.get(server).get(p);
        BlockPos printer = nearestPrinter(ow, p, a);
        long now = ow.getGameTime();
        if (!obep) {
            spawnSnoop(ow, p, a, now);
            return null;
        }
        if (printer == null) return "у вас нет подключённых станков";
        if (obep) {
            if (RAIDS.containsKey(p.getUUID())) return "рейд уже идёт";
            TIPS.remove(p.getUUID());
            startRaid(ow, p, printer, now);
        }
        return null;
    }

    /** Для админ-команды и тестов: наводка прямо сейчас. */
    public static void tipNow(ServerPlayer p, BlockPos pos, long raidAt) {
        TIPS.put(p.getUUID(), new Tip(p.getUUID(), pos.immutable(), raidAt));
    }

    static void startRaid(ServerLevel w, ServerPlayer p, BlockPos target, long now) {
        Raid r = new Raid(p.getUUID(), target.immutable());
        BlockPos start = surfaceNear(w, target, 8, 12);
        r.cur = start;
        r.start = now;
        r.nextStep = now + 100;
        for (int i = 0; i < 2; i++) {
            VisitorEntity e = ModEntities.VISITOR.get().create(w, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (e == null) continue;
            e.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0F, 0F);
            e.setKind(VisitorKind.OBEP);
            e.targetId = p.getUUID();
            e.setCustomName(Component.literal(VisitorKind.OBEP.display));
            e.setCustomNameVisible(true);
            e.mode = VisitorEntity.STAY;
            Visitors.manage(e);
            w.addFreshEntity(e);
            r.opers.add(e);
        }
        RAIDS.put(p.getUUID(), r);
        BankService.msg(p, OBEP + "Поступила оперативная информация о незаконном изготовлении денежных знаков по адресу ["
                + target.getX() + ", " + target.getY() + ", " + target.getZ() + "]. Проводим обследование помещения!");
        BankService.msg(p, OBEP + "§7Опера начинают копать к месту наводки. Мешать бесполезно.");
    }

    /** Следующая клетка тоннеля: по горизонтали, по вертикали — лесенкой. */
    public static BlockPos nextCell(BlockPos cur, BlockPos target, int step) {
        int dx = target.getX() - cur.getX();
        int dy = target.getY() - cur.getY();
        int dz = target.getZ() - cur.getZ();
        boolean horizontalLeft = dx != 0 || dz != 0;
        if (dy != 0 && (!horizontalLeft || step % 2 == 1)) return cur.above(Integer.signum(dy));
        if (Math.abs(dx) >= Math.abs(dz) && dx != 0) return cur.east(Integer.signum(dx));
        if (dz != 0) return cur.south(Integer.signum(dz));
        return cur.above(Integer.signum(dy));
    }

    /** Дошли: станок на расстоянии вытянутой руки (в том числе на уровне головы). */
    public static boolean arrived(BlockPos cur, BlockPos target) {
        int m = Math.abs(cur.getX() - target.getX()) + Math.abs(cur.getY() - target.getY()) + Math.abs(cur.getZ() - target.getZ());
        if (m <= 1) return true;
        BlockPos head = cur.above();
        return Math.abs(head.getX() - target.getX()) + Math.abs(head.getY() - target.getY()) + Math.abs(head.getZ() - target.getZ()) <= 1;
    }

    /** Выкопать клетку (с запоминанием). false — не пробиться. */
    public static boolean dig(ServerLevel w, EconomyData data, ListTag snap, BlockPos pos, BlockPos target) {
        if (pos.equals(target)) return true;
        BlockState st = w.getBlockState(pos);
        if (st.isAir(w, pos)) return true;
        if (st.getBlock() == ModBlocks.MONEY_PRINTER.get()) return true; // станки не трогаем — их изымут
        if (st.getDestroySpeed(w, pos) < 0 || data.isProtected(pos)) return false;
        CompoundTag t = new CompoundTag();
        t.putLong("p", pos.asLong());
        t.put("s", NbtUtils.writeBlockState(st));
        BlockEntity te = w.getBlockEntity(pos);
        if (te != null) {
            t.put("t", te.write(new CompoundTag()));
            if (te instanceof Container) ((Container) te).clear();
        }
        snap.add(t);
        w.levelEvent(2001, pos, Block.getStateId(st));
        w.setBlockState(pos, Blocks.AIR.defaultBlockState(), Blueprint.FLAGS);
        return true;
    }

    static void stepRaid(MinecraftServer server, ServerLevel w, EconomyData data, Raid r, long now) {
        ServerPlayer p = server.getPlayerList().getPlayer(r.player);
        if (p == null || p.level() != w) { abort(w, r, null); return; }
        if (now - r.start > RAID_TIMEOUT) { abort(w, r, p); return; }
        if (now < r.nextStep) return;
        r.nextStep = now + 20;
        if (arrived(r.cur, r.target) || r.steps >= MAX_STEPS) {
            finishRaid(server, w, data, r, p);
            return;
        }
        BlockPos next = nextCell(r.cur, r.target, r.steps);
        r.steps++;
        if (!dig(w, data, r.snapshot, next, r.target) || !dig(w, data, r.snapshot, next.above(), r.target)) {
            BankService.msg(p, OBEP + "Тут не пробиться... Отбой, обследование прекращено.");
            abort(w, r, p);
            return;
        }
        r.cur = next;
        for (int i = 0; i < r.opers.size(); i++) {
            VisitorEntity e = r.opers.get(i);
            if (!e.isAlive()) continue;
            e.teleportTo(next.getX() + 0.5, next.getY(), next.getZ() + 0.5);
            e.getLookControl().setLookAt(r.target.getX() + 0.5, r.target.getY() + 0.5, r.target.getZ() + 0.5, 10F, 10F);
        }
    }

    static void finishRaid(MinecraftServer server, ServerLevel w, EconomyData data, Raid r, ServerPlayer p) {
        Account a = data.get(p);
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : new ArrayList<>(a.printerList)) {
            if (pos.distanceSq(r.target) <= 6 * 6 && isPrinter(w, pos)) found.add(pos);
        }
        if (isPrinter(w, r.target) && !found.contains(r.target)) found.add(r.target);
        if (found.isEmpty()) {
            BankService.msg(p, OBEP + "Хм... Ничего нет. Извините за беспокойство, информация не подтвердилась.");
            BankService.msg(p, OBEP + "§7Всё вернём как было.");
            end(w, r);
            return;
        }
        for (BlockPos pos : found) {
            BlockEntity te = w.getBlockEntity(pos);
            if (te instanceof Container) ((Container) te).clear();
            BlockState st = w.getBlockState(pos);
            w.setBlockState(pos, Blocks.AIR.defaultBlockState(), 3);
            Business.onBroken(server, pos, st);
        }
        double fine = Config.RAID_FINE.get() * found.size();
        BankService.msg(p, OBEP + "§cОбнаружено " + found.size() + " печатн. станк. и фальшивые купюры! §fОборудование изъято. "
                + "Штраф: §c" + Finance.money(fine) + "§f.");
        Visitors.forceCharge(p, a, fine);
        data.setDirty();
        end(w, r);
        DebtManager.jailFor(server, data, p, a, Config.RAID_JAIL_TICKS.get(), "изготовление фальшивых денег");
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other != p) BankService.msg(other, "§9[Новости] §fОБЭП накрыл подпольный цех игрока §e" + p.getGameProfile().getName() + "§f.");
        }
    }

    /** Закончить рейд: тоннель засыпать обратно, оперов увести. */
    static void end(ServerLevel w, Raid r) {
        if (r.done) return;
        r.done = true;
        for (VisitorEntity e : r.opers) {
            Visitors.unmanage(e);
            if (e.isAlive()) e.discard();
        }
        if (!r.snapshot.isEmpty()) Blueprint.restore(w, r.snapshot);
        RAIDS.remove(r.player);
    }

    static void abort(ServerLevel w, Raid r, ServerPlayer p) {
        if (p != null && !r.done) BankService.msg(p, OBEP + "§7Опера ушли. Тоннель засыпали.");
        end(w, r);
    }

    /** Остановка сервера: засыпать все тоннели. */
    public static void onStop(MinecraftServer server) {
        ServerLevel ow = EconomyData.overworld(server);
        for (Raid r : new ArrayList<>(RAIDS.values())) {
            if (ow != null) end(ow, r);
        }
        RAIDS.clear();
        TIPS.clear();
        NEXT_SNOOP.clear();
    }
}
