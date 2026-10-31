package com.baraaoi.reallife.event;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.LegalBusiness;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * v1.9: «страшные» проверки и происшествия — расчёты без диалогов (диалоги — в {@link Visitors}).
 * СЭС смотрит качество отделки, свет, грызунов, паутину, просрочку, раковину и медкнижки работников;
 * миграционная служба — патенты; пожар и обвал — здесь и сейчас.
 */
public final class Inspections {
    private Inspections() {}

    /** Документы работников (медкнижка + патент) на одного человека на 7 дней. */
    public static final int DOCS_PRICE = 300;
    public static final int DOCS_DAYS = 7;

    /** «Грязные» отделочные блоки: в торговом зале так нельзя. */
    public static final Set<Block> DIRTY = new HashSet<>(Arrays.asList(Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.GRASS_BLOCK,
            Blocks.DIRT_PATH, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.GRAVEL, Blocks.SAND, Blocks.RED_SAND, Blocks.COBBLESTONE,
            Blocks.MOSSY_COBBLESTONE, Blocks.NETHERRACK, Blocks.SOUL_SAND, Blocks.SOUL_SOIL, Blocks.CLAY, Blocks.FARMLAND));
    /** Что считается просрочкой/отравой на полках еды. */
    public static final Set<Item> SPOILED = new HashSet<>(Arrays.asList(Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO,
            Items.PUFFERFISH, Items.SUSPICIOUS_STEW));
    /** Природная порода, которая может обвалиться. */
    public static final Set<Block> ROCK = new HashSet<>(Arrays.asList(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE,
            Blocks.DIRT, Blocks.GRAVEL, Blocks.COARSE_DIRT));

    // ------------------------------------------------------------------ документы и пломбы

    /** Сколько работников без действующих документов. */
    public static int undocumented(LegalBusiness b, long today) {
        int n = b.workers.size();
        if (today >= b.docsUntil) return n;
        return Math.max(0, n - b.docsWorkers);
    }

    /** Опечатано ли заведение в этот день. */
    public static boolean sealed(LegalBusiness b, long day) {
        return b.sealedUntil > 0 && day < b.sealedUntil;
    }

    public static void seal(LegalBusiness b, long today, int days, String reason) {
        b.sealedUntil = Math.max(b.sealedUntil, today + days);
        b.sealReason = reason;
    }

    // ------------------------------------------------------------------ СЭС

    public static final class Report {
        public final List<String> lines = new ArrayList<>();
        public int fine;
        public int violations;
        public int spoiled;

        void add(String text, int amount) {
            lines.add(text + " — §c" + amount + "₽");
            fine += amount;
            violations++;
        }
    }

    public static boolean food(BizType t) {
        return t == BizType.SHAWARMA || t == BizType.MINIMARKET;
    }

    /** Проверка СЭС. confiscate — сразу изъять просрочку. */
    public static Report sanepid(Level w, LegalBusiness b, long today, boolean confiscate) {
        Report r = new Report();
        int floorY = b.counter.getY();
        Set<BlockPos> exposed = new HashSet<>();
        int dirty = 0, cobweb = 0, floorSpots = 0, dark = 0;
        boolean sink = false;
        for (BlockPos mp : BlockPos.betweenClosed(b.min, b.max)) {
            BlockState st = w.getBlockState(mp);
            if (st.getBlock() == Blocks.COBWEB) cobweb++;
            if (st.getBlock() == Blocks.WATER_CAULDRON) sink = true;
            if (mp.getY() < floorY || !st.isAir() || w.canSeeSky(mp)) continue;
            BlockPos pos = mp.immutable();
            for (Direction d : Direction.values()) {
                BlockPos q = pos.relative(d);
                if (!b.contains(q, 0) || exposed.contains(q)) continue;
                BlockState qs = w.getBlockState(q);
                if (qs.isAir() || !qs.isSolid()) continue;
                exposed.add(q);
                if (DIRTY.contains(qs.getBlock())) dirty++;
            }
            if (pos.getY() <= floorY + 1 && w.getBlockState(pos.below()).isSolid()) {
                floorSpots++;
                if (w.getBrightness(LightLayer.BLOCK, pos) < 8) dark++;
            }
        }
        if (exposed.size() < 25) {
            r.add("Нет помещения — торговля под открытым небом", 1000);
        } else {
            if (dirty > exposed.size() * 0.25) r.add("Отделка из земли/булыжника (" + Math.round(dirty * 100.0 / exposed.size()) + "%) — антисанитария", 1000);
            if (floorSpots > 0 && dark > floorSpots * 0.3) r.add("Тёмный зал (" + Math.round(dark * 100.0 / floorSpots) + "% пола без света)", 500);
        }
        if (cobweb > 0) r.add("Паутина в торговом зале (" + cobweb + ")", 300);
        List<Monster> mobs = w.getEntitiesOfClass(Monster.class, new AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(b.min), net.minecraft.world.phys.Vec3.atLowerCornerOf(b.max.offset(1, 1, 1))));
        if (!mobs.isEmpty()) r.add("Нечисть в заведении (" + mobs.size() + ")", 1500);
        if (food(b.type)) {
            if (!sink) r.add("Нет раковины (котёл с водой)", 400);
            int spoiled = 0;
            for (BlockPos mp : BlockPos.betweenClosed(b.min, b.max)) {
                BlockEntity te = w.getBlockEntity(mp);
                if (!(te instanceof Container)) continue;
                Container inv = (Container) te;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack it = inv.getItem(i);
                    if (it.isEmpty() || !SPOILED.contains(it.getItem())) continue;
                    spoiled += it.getCount();
                    if (confiscate) inv.setItem(i, ItemStack.EMPTY);
                }
                if (confiscate) inv.setChanged();
            }
            r.spoiled = spoiled;
            if (spoiled > 0) r.add("Просрочка/отрава на складе (" + spoiled + " шт., изъято)", 700);
        }
        int undoc = undocumented(b, today);
        if (undoc > 0) r.add("Работники без медкнижек: " + undoc, 800 * undoc);
        return r;
    }

    // ------------------------------------------------------------------ пожар

    /** Где у игрока «своё»: ипотечный дом, заведения, купленная земля рядом. Коробки [min, max] в радиусе r от игрока. */
    public static List<BlockPos[]> homeBoxes(ServerPlayer p, Account a, EconomyData data, int r) {
        List<BlockPos[]> l = new ArrayList<>();
        if (a.hasHouse && !a.houseMin.equals(BlockPos.ZERO)) l.add(new BlockPos[]{a.houseMin, a.houseMax});
        for (LegalBusiness b : data.bizOf(a.id)) l.add(new BlockPos[]{b.min, b.max});
        int py = (int) p.getY();
        BlockPos[] land = null;
        double best = Double.MAX_VALUE;
        for (Map.Entry<Long, UUID> e : data.landOwners.entrySet()) {
            if (!a.id.equals(e.getValue())) continue;
            int cx = ChunkPos.getX(e.getKey()), cz = ChunkPos.getZ(e.getKey());
            double dx = cx * 16 + 8 - p.getX(), dz = cz * 16 + 8 - p.getZ();
            double d = dx * dx + dz * dz;
            if (d < best) {
                best = d;
                land = new BlockPos[]{new BlockPos(cx * 16, Math.max(1, py - 6), cz * 16), new BlockPos(cx * 16 + 15, Math.min(254, py + 10), cz * 16 + 15)};
            }
        }
        if (land != null) l.add(land);
        l.removeIf(bx -> {
            double cx = (bx[0].getX() + bx[1].getX()) / 2.0 - p.getX(), cz = (bx[0].getZ() + bx[1].getZ()) / 2.0 - p.getZ();
            return cx * cx + cz * cz > r * r;
        });
        return l;
    }

    /** Места, где может вспыхнуть: горючий блок, над ним воздух. */
    public static List<BlockPos> fireSpots(Level w, BlockPos min, BlockPos max, int n, Random rnd, EconomyData data) {
        List<BlockPos> cand = new ArrayList<>();
        for (BlockPos mp : BlockPos.betweenClosed(min, max)) {
            BlockState st = w.getBlockState(mp);
            if (st.isAir() || !st.isFlammable(w, mp, Direction.UP)) continue;
            BlockPos up = mp.above();
            if (!w.isEmptyBlock(up) || (data != null && data.isProtected(up))) continue;
            cand.add(up.immutable());
        }
        Collections.shuffle(cand, rnd);
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos c : cand) {
            if (out.size() >= n) break;
            boolean far = true;
            for (BlockPos o : out) if (o.distSqr(c) < 9) far = false;
            if (far) out.add(c);
        }
        return out;
    }

    public static int ignite(Level w, List<BlockPos> spots) {
        int n = 0;
        for (BlockPos p : spots) if (w.isEmptyBlock(p)) { w.setBlock(p, Blocks.FIRE.defaultBlockState(), 11); n++; }
        return n;
    }

    public static int countFire(Level w, BlockPos min, BlockPos max) {
        int n = 0;
        for (BlockPos mp : BlockPos.betweenClosed(min, max)) {
            Block b = w.getBlockState(mp).getBlock();
            if (b == Blocks.FIRE || b == Blocks.SOUL_FIRE) n++;
        }
        return n;
    }

    public static int extinguish(Level w, BlockPos min, BlockPos max) {
        int n = 0;
        for (BlockPos mp : BlockPos.betweenClosed(min, max)) {
            Block b = w.getBlockState(mp).getBlock();
            if (b == Blocks.FIRE || b == Blocks.SOUL_FIRE) { w.setBlock(mp.immutable(), Blocks.AIR.defaultBlockState(), 3); n++; }
        }
        return n;
    }

    // ------------------------------------------------------------------ обвал

    /** Блоки потолка над точкой, которые обвалятся (природная порода, 1–2 слоя, радиус 2). */
    public static List<BlockPos> caveIn(Level w, BlockPos at, EconomyData data) {
        List<BlockPos> l = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                BlockPos c = at.offset(dx, 1, dz);
                int y = 0;
                while (y < 6 && w.isEmptyBlock(c.above(y))) y++;
                if (y >= 6) continue;
                for (int k = 0; k < 2; k++) {
                    BlockPos q = c.above(y + k);
                    if (q.getY() > 250) break;
                    if (!ROCK.contains(w.getBlockState(q).getBlock()) || (data != null && data.isProtected(q))) break;
                    l.add(q.immutable());
                }
            }
        return l;
    }

    public static int collapse(Level w, List<BlockPos> blocks) {
        int n = 0;
        for (BlockPos q : blocks) {
            if (!ROCK.contains(w.getBlockState(q).getBlock())) continue;
            w.setBlock(q, Blocks.GRAVEL.defaultBlockState(), 3);
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ домушник

    /** Непустой сундук/бочка в своих коробках (самый «жирный» из случайных трёх). */
    
    public static BlockPos burglarTarget(Level w, List<BlockPos[]> boxes, Random rnd) {
        List<BlockPos> chests = new ArrayList<>();
        for (BlockPos[] bx : boxes)
            for (BlockPos mp : BlockPos.betweenClosed(bx[0], bx[1])) {
                BlockEntity te = w.getBlockEntity(mp);
                if (!(te instanceof Container) || ((Container) te).isEmpty()) continue;
                if (te.getClass().getName().contains("reallife")) continue; // кассы, станки, полки бизнеса не трогает
                chests.add(mp.immutable());
                if (chests.size() > 64) break;
            }
        if (chests.isEmpty()) return null;
        BlockPos best = null;
        double bv = -1;
        for (int i = 0; i < 3; i++) {
            BlockPos c = chests.get(rnd.nextInt(chests.size()));
            double v = Visitors.houseValue(w, c, c);
            if (v > bv) { bv = v; best = c; }
        }
        return best;
    }
}
