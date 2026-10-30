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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
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
        final ListNBT snapshot = new ListNBT();
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

    static final class Snoop {
        final UUID player;
        final VisitorEntity e;
        final long until;

        Snoop(UUID player, VisitorEntity e, long until) {
            this.player = player;
            this.e = e;
            this.until = until;
        }
    }

    private static final Map<UUID, Tip> TIPS = new HashMap<>();
    private static final Map<UUID, Raid> RAIDS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_SNOOP = new HashMap<>();
    private static final List<Snoop> SNOOPS = new ArrayList<>();

    // =====================================================================================
    //  видимость
    // =====================================================================================

    /** Видно ли центр блока target из точки from (непрозрачные блоки загораживают, стекло — нет). */
    public static boolean canSee(World w, Vector3d from, BlockPos target) {
        Vector3d to = new Vector3d(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        Vector3d d = to.subtract(from);
        double len = d.length();
        if (len < 1e-3) return true;
        int n = (int) Math.ceil(len / 0.25);
        BlockPos start = new BlockPos(from);
        BlockPos last = null;
        for (int i = 1; i < n; i++) {
            Vector3d v = from.add(d.scale(i / (double) n));
            BlockPos bp = new BlockPos(v);
            if (bp.equals(last) || bp.equals(start)) continue;
            last = bp;
            if (bp.equals(target)) return true;
            BlockState st = w.getBlockState(bp);
            if (st.isOpaqueCube(w, bp)) return false;
        }
        return true;
    }

    private static boolean isObserver(Entity e) {
        if (e instanceof VisitorEntity) return ((VisitorEntity) e).getKind() != VisitorKind.OBEP;
        return e instanceof AbstractVillagerEntity || e instanceof DebtNpcEntity;
    }

    @Nullable
    private static Entity spotter(ServerWorld w, BlockPos printer) {
        int r = Config.SPOT_RANGE.get();
        AxisAlignedBB box = new AxisAlignedBB(printer).grow(r);
        for (Entity e : w.getEntitiesWithinAABB(Entity.class, box, Raids::isObserver)) {
            if (!e.isAlive() || e.getDistanceSq(printer.getX() + 0.5, printer.getY() + 0.5, printer.getZ() + 0.5) > r * r) continue;
            if (canSee(w, e.getEyePosition(1.0F), printer)) return e;
        }
        return null;
    }

    private static boolean isPrinter(World w, BlockPos p) {
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

    public static void tick(MinecraftServer server) {
        ServerWorld ow = EconomyData.overworld(server);
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
        // любопытные
        Iterator<Snoop> si = SNOOPS.iterator();
        while (si.hasNext()) {
            Snoop s = si.next();
            if (!s.e.isAlive() || now > s.until + 200) {
                Visitors.unmanage(s.e);
                if (s.e.isAlive()) s.e.remove();
                si.remove();
            } else if (now > s.until && s.e.mode != VisitorEntity.FLEE) {
                s.e.mode = VisitorEntity.FLEE;
            }
        }
        // наводки → рейд
        Iterator<Tip> ti = TIPS.values().iterator();
        while (ti.hasNext()) {
            Tip t = ti.next();
            if (now < t.raidAt) continue;
            ServerPlayerEntity p = server.getPlayerList().getPlayerByUUID(t.player);
            if (p == null) continue; // ждём, пока зайдёт
            Account a = data.get(p);
            if (a.captivity != Account.FREE || p.world != ow || RAIDS.containsKey(t.player)) continue;
            ti.remove();
            startRaid(ow, p, t.pos, now);
        }

        for (ServerPlayerEntity p : server.getPlayerList().getPlayers()) {
            if (p.world != ow || p.isSpectator()) continue;
            Account a = data.get(p);
            if (a.captivity != Account.FREE || a.printerList.isEmpty()) continue;
            UUID id = p.getUniqueID();
            // кто-то увидел станок?
            if (!TIPS.containsKey(id) && !RAIDS.containsKey(id)) {
                for (BlockPos pos : new ArrayList<>(a.printerList)) {
                    if (!ow.isBlockLoaded(pos) || !isPrinter(ow, pos)) continue;
                    Entity who = spotter(ow, pos);
                    if (who == null) continue;
                    TIPS.put(id, new Tip(id, pos.toImmutable(), now + 1200 + R.nextInt(2401)));
                    String name = who.getDisplayName().getString();
                    BankService.msg(p, "§8[Слухи] §7" + name + "§7 заметил твой печатный станок и уже куда-то звонит... "
                            + "§cЖди ОБЭП через пару минут. §7Убери станок или замуруй — но по наводке всё равно будут копать.");
                    break;
                }
            }
            // подозрительный тип
            Long next = NEXT_SNOOP.get(id);
            if (next == null) { NEXT_SNOOP.put(id, now + snoopInterval(a.printerList.size())); continue; }
            if (now < next) continue;
            NEXT_SNOOP.put(id, now + snoopInterval(a.printerList.size()));
            if (RAIDS.containsKey(id)) continue;
            boolean has = false;
            for (Snoop s : SNOOPS) if (s.player.equals(id)) has = true;
            if (!has) spawnSnoop(ow, p, a, now);
        }
    }

    // =====================================================================================
    //  подозрительный тип
    // =====================================================================================

    @Nullable
    private static BlockPos nearestPrinter(ServerWorld w, ServerPlayerEntity p, Account a) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos pos : a.printerList) {
            if (!w.isBlockLoaded(pos)) continue;
            double d = pos.distanceSq(p.getPosX(), p.getPosY(), p.getPosZ(), true);
            if (d < bd) { bd = d; best = pos; }
        }
        return best;
    }

    /** Точка на поверхности в кольце minR..maxR вокруг center. */
    static BlockPos surfaceNear(ServerWorld w, BlockPos center, int minR, int maxR) {
        for (int i = 0; i < 24; i++) {
            double ang = R.nextDouble() * Math.PI * 2;
            double dist = minR + R.nextDouble() * (maxR - minR);
            int x = center.getX() + (int) Math.round(Math.cos(ang) * dist);
            int z = center.getZ() + (int) Math.round(Math.sin(ang) * dist);
            if (!w.isBlockLoaded(new BlockPos(x, 64, z))) continue;
            int y = w.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            if (Math.abs(y - center.getY()) > 24) continue;
            if (w.getBlockState(p.down()).getMaterial().isLiquid()) continue;
            if (data(w).isProtected(p)) continue;
            return p;
        }
        return center.up(2);
    }

    private static EconomyData data(ServerWorld w) {
        return EconomyData.get(w.getServer());
    }

    static void spawnSnoop(ServerWorld w, ServerPlayerEntity p, Account a, long now) {
        BlockPos printer = nearestPrinter(w, p, a);
        if (printer == null) return;
        VisitorEntity e = ModEntities.VISITOR.get().create(w);
        if (e == null) return;
        BlockPos at = surfaceNear(w, printer, 10, 18);
        e.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, R.nextFloat() * 360F, 0F);
        e.setKind(VisitorKind.SNOOP);
        e.targetId = p.getUniqueID();
        e.setCustomName(new StringTextComponent(VisitorKind.SNOOP.display));
        e.setCustomNameVisible(true);
        e.mode = VisitorEntity.GOTO;
        e.gotoPos = printer.toImmutable();
        Visitors.manage(e);
        w.addEntity(e);
        SNOOPS.add(new Snoop(p.getUniqueID(), e, now + 1200));
        BankService.actionBar(p, "§8Возле твоих станков крутится какой-то подозрительный тип...");
    }

    // =====================================================================================
    //  рейд
    // =====================================================================================

    public static boolean raidActive(UUID player) {
        return RAIDS.containsKey(player);
    }

    /** Админ-команда: прислать подозрительного типа или сразу ОБЭП к ближайшему станку. */
    public static String adminStart(ServerPlayerEntity p, boolean obep) {
        MinecraftServer server = p.getServer();
        if (server == null) return "нет сервера";
        ServerWorld ow = EconomyData.overworld(server);
        if (p.world != ow) return "только в обычном мире";
        Account a = EconomyData.get(server).get(p);
        BlockPos printer = nearestPrinter(ow, p, a);
        if (printer == null) return "у вас нет подключённых станков";
        long now = ow.getGameTime();
        if (obep) {
            if (RAIDS.containsKey(p.getUniqueID())) return "рейд уже идёт";
            TIPS.remove(p.getUniqueID());
            startRaid(ow, p, printer, now);
        } else {
            spawnSnoop(ow, p, a, now);
        }
        return null;
    }

    /** Для админ-команды и тестов: наводка прямо сейчас. */
    public static void tipNow(ServerPlayerEntity p, BlockPos pos, long raidAt) {
        TIPS.put(p.getUniqueID(), new Tip(p.getUniqueID(), pos.toImmutable(), raidAt));
    }

    static void startRaid(ServerWorld w, ServerPlayerEntity p, BlockPos target, long now) {
        Raid r = new Raid(p.getUniqueID(), target.toImmutable());
        BlockPos start = surfaceNear(w, target, 8, 12);
        r.cur = start;
        r.start = now;
        r.nextStep = now + 100;
        for (int i = 0; i < 2; i++) {
            VisitorEntity e = ModEntities.VISITOR.get().create(w);
            if (e == null) continue;
            e.setLocationAndAngles(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0F, 0F);
            e.setKind(VisitorKind.OBEP);
            e.targetId = p.getUniqueID();
            e.setCustomName(new StringTextComponent(VisitorKind.OBEP.display));
            e.setCustomNameVisible(true);
            e.mode = VisitorEntity.STAY;
            Visitors.manage(e);
            w.addEntity(e);
            r.opers.add(e);
        }
        RAIDS.put(p.getUniqueID(), r);
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
        if (dy != 0 && (!horizontalLeft || step % 2 == 1)) return cur.up(Integer.signum(dy));
        if (Math.abs(dx) >= Math.abs(dz) && dx != 0) return cur.east(Integer.signum(dx));
        if (dz != 0) return cur.south(Integer.signum(dz));
        return cur.up(Integer.signum(dy));
    }

    /** Дошли: станок на расстоянии вытянутой руки (в том числе на уровне головы). */
    public static boolean arrived(BlockPos cur, BlockPos target) {
        int m = Math.abs(cur.getX() - target.getX()) + Math.abs(cur.getY() - target.getY()) + Math.abs(cur.getZ() - target.getZ());
        if (m <= 1) return true;
        BlockPos head = cur.up();
        return Math.abs(head.getX() - target.getX()) + Math.abs(head.getY() - target.getY()) + Math.abs(head.getZ() - target.getZ()) <= 1;
    }

    /** Выкопать клетку (с запоминанием). false — не пробиться. */
    public static boolean dig(ServerWorld w, EconomyData data, ListNBT snap, BlockPos pos, BlockPos target) {
        if (pos.equals(target)) return true;
        BlockState st = w.getBlockState(pos);
        if (st.isAir(w, pos)) return true;
        if (st.getBlock() == ModBlocks.MONEY_PRINTER.get()) return true; // станки не трогаем — их изымут
        if (st.getBlockHardness(w, pos) < 0 || data.isProtected(pos)) return false;
        CompoundNBT t = new CompoundNBT();
        t.putLong("p", pos.toLong());
        t.put("s", NBTUtil.writeBlockState(st));
        TileEntity te = w.getTileEntity(pos);
        if (te != null) {
            t.put("t", te.write(new CompoundNBT()));
            if (te instanceof IInventory) ((IInventory) te).clear();
        }
        snap.add(t);
        w.playEvent(2001, pos, Block.getStateId(st));
        w.setBlockState(pos, Blocks.AIR.getDefaultState(), Blueprint.FLAGS);
        return true;
    }

    static void stepRaid(MinecraftServer server, ServerWorld w, EconomyData data, Raid r, long now) {
        ServerPlayerEntity p = server.getPlayerList().getPlayerByUUID(r.player);
        if (p == null || p.world != w) { abort(w, r, null); return; }
        if (now - r.start > RAID_TIMEOUT) { abort(w, r, p); return; }
        if (now < r.nextStep) return;
        r.nextStep = now + 20;
        if (arrived(r.cur, r.target) || r.steps >= MAX_STEPS) {
            finishRaid(server, w, data, r, p);
            return;
        }
        BlockPos next = nextCell(r.cur, r.target, r.steps);
        r.steps++;
        if (!dig(w, data, r.snapshot, next, r.target) || !dig(w, data, r.snapshot, next.up(), r.target)) {
            BankService.msg(p, OBEP + "Тут не пробиться... Отбой, обследование прекращено.");
            abort(w, r, p);
            return;
        }
        r.cur = next;
        for (int i = 0; i < r.opers.size(); i++) {
            VisitorEntity e = r.opers.get(i);
            if (!e.isAlive()) continue;
            e.setPositionAndUpdate(next.getX() + 0.5, next.getY(), next.getZ() + 0.5);
            e.getLookController().setLookPosition(r.target.getX() + 0.5, r.target.getY() + 0.5, r.target.getZ() + 0.5, 10F, 10F);
        }
    }

    static void finishRaid(MinecraftServer server, ServerWorld w, EconomyData data, Raid r, ServerPlayerEntity p) {
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
            TileEntity te = w.getTileEntity(pos);
            if (te instanceof IInventory) ((IInventory) te).clear();
            BlockState st = w.getBlockState(pos);
            w.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            Business.onBroken(server, pos, st);
        }
        double fine = Config.RAID_FINE.get() * found.size();
        BankService.msg(p, OBEP + "§cОбнаружено " + found.size() + " печатн. станк. и фальшивые купюры! §fОборудование изъято. "
                + "Штраф: §c" + Finance.money(fine) + "§f.");
        Visitors.forceCharge(p, a, fine);
        data.markDirty();
        end(w, r);
        DebtManager.jailFor(server, data, p, a, Config.RAID_JAIL_TICKS.get(), "изготовление фальшивых денег");
        for (ServerPlayerEntity other : server.getPlayerList().getPlayers()) {
            if (other != p) BankService.msg(other, "§9[Новости] §fОБЭП накрыл подпольный цех игрока §e" + p.getGameProfile().getName() + "§f.");
        }
    }

    /** Закончить рейд: тоннель засыпать обратно, оперов увести. */
    static void end(ServerWorld w, Raid r) {
        if (r.done) return;
        r.done = true;
        for (VisitorEntity e : r.opers) {
            Visitors.unmanage(e);
            if (e.isAlive()) e.remove();
        }
        if (!r.snapshot.isEmpty()) Blueprint.restore(w, r.snapshot);
        RAIDS.remove(r.player);
    }

    static void abort(ServerWorld w, Raid r, @Nullable ServerPlayerEntity p) {
        if (p != null && !r.done) BankService.msg(p, OBEP + "§7Опера ушли. Тоннель засыпали.");
        end(w, r);
    }

    /** Остановка сервера: засыпать все тоннели. */
    public static void onStop(MinecraftServer server) {
        ServerWorld ow = EconomyData.overworld(server);
        for (Raid r : new ArrayList<>(RAIDS.values())) {
            if (ow != null) end(ow, r);
        }
        RAIDS.clear();
        TIPS.clear();
        for (Snoop s : SNOOPS) {
            Visitors.unmanage(s.e);
            if (s.e.isAlive()) s.e.remove();
        }
        SNOOPS.clear();
        NEXT_SNOOP.clear();
    }
}
