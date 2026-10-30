package com.baraaoi.reallife.event;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.LegalBusiness;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CauldronBlock;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import javax.annotation.Nullable;
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
            Blocks.GRASS_PATH, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.GRAVEL, Blocks.SAND, Blocks.RED_SAND, Blocks.COBBLESTONE,
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
        return day < b.sealedUntil;
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

    private static boolean food(BizType t) {
        return t == BizType.SHAWARMA || t == BizType.MINIMARKET;
    }

    /** Проверка СЭС. confiscate — сразу изъять просрочку. */
    public static Report sanepid(World w, LegalBusiness b, long today, boolean confiscate) {
        Report r = new Report();
        int floorY = b.counter.getY();
        Set<BlockPos> exposed = new HashSet<>();
        int dirty = 0, cobweb = 0, floorSpots = 0, dark = 0;
        boolean sink = false;
        for (BlockPos mp : BlockPos.getAllInBoxMutable(b.min, b.max)) {
            BlockState st = w.getBlockState(mp);
            if (st.getBlock() == Blocks.COBWEB) cobweb++;
            if (st.getBlock() == Blocks.CAULDRON && st.get(CauldronBlock.LEVEL) > 0) sink = true;
            if (mp.getY() < floorY || !st.isAir(w, mp) || w.canSeeSky(mp)) continue;
            BlockPos pos = mp.toImmutable();
            for (Direction d : Direction.values()) {
                BlockPos q = pos.offset(d);
                if (!b.contains(q, 0) || exposed.contains(q)) continue;
                BlockState qs = w.getBlockState(q);
                if (qs.isAir(w, q) || !qs.getMaterial().isSolid()) continue;
                exposed.add(q);
                if (DIRTY.contains(qs.getBlock())) dirty++;
            }
            if (pos.getY() <= floorY + 1 && w.getBlockState(pos.down()).getMaterial().isSolid()) {
                floorSpots++;
                if (w.getLightFor(LightType.BLOCK, pos) < 8) dark++;
            }
        }
        if (exposed.size() < 25) {
            r.add("Нет помещения — торговля под открытым небом", 1000);
        } else {
            if (dirty > exposed.size() * 0.25) r.add("Отделка из земли/булыжника (" + Math.round(dirty * 100.0 / exposed.size()) + "%) — антисанитария", 1000);
            if (floorSpots > 0 && dark > floorSpots * 0.3) r.add("Тёмный зал (" + Math.round(dark * 100.0 / floorSpots) + "% пола без света)", 500);
        }
        if (cobweb > 0) r.add("Паутина в торговом зале (" + cobweb + ")", 300);
        List<MonsterEntity> mobs = w.getEntitiesWithinAABB(MonsterEntity.class, new AxisAlignedBB(b.min, b.max.add(1, 1, 1)));
        if (!mobs.isEmpty()) r.add("Нечисть в заведении (" + mobs.size() + ")", 1500);
        if (food(b.type)) {
            if (!sink) r.add("Нет раковины (котёл с водой)", 400);
            int spoiled = 0;
            for (BlockPos mp : BlockPos.getAllInBoxMutable(b.min, b.max)) {
                TileEntity te = w.getTileEntity(mp);
                if (!(te instanceof IInventory)) continue;
                IInventory inv = (IInventory) te;
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    ItemStack it = inv.getStackInSlot(i);
                    if (it.isEmpty() || !SPOILED.contains(it.getItem())) continue;
                    spoiled += it.getCount();
                    if (confiscate) inv.setInventorySlotContents(i, ItemStack.EMPTY);
                }
                if (confiscate) inv.markDirty();
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
    public static List<BlockPos[]> homeBoxes(ServerPlayerEntity p, Account a, EconomyData data, int r) {
        List<BlockPos[]> l = new ArrayList<>();
        if (a.hasHouse && !a.houseMin.equals(BlockPos.ZERO)) l.add(new BlockPos[]{a.houseMin, a.houseMax});
        for (LegalBusiness b : data.bizOf(a.id)) l.add(new BlockPos[]{b.min, b.max});
        int py = (int) p.getPosY();
        BlockPos[] land = null;
        double best = Double.MAX_VALUE;
        for (Map.Entry<Long, UUID> e : data.landOwners.entrySet()) {
            if (!a.id.equals(e.getValue())) continue;
            int cx = ChunkPos.getX(e.getKey()), cz = ChunkPos.getZ(e.getKey());
            double dx = cx * 16 + 8 - p.getPosX(), dz = cz * 16 + 8 - p.getPosZ();
            double d = dx * dx + dz * dz;
            if (d < best) {
                best = d;
                land = new BlockPos[]{new BlockPos(cx * 16, Math.max(1, py - 6), cz * 16), new BlockPos(cx * 16 + 15, Math.min(254, py + 10), cz * 16 + 15)};
            }
        }
        if (land != null) l.add(land);
        l.removeIf(bx -> {
            double cx = (bx[0].getX() + bx[1].getX()) / 2.0 - p.getPosX(), cz = (bx[0].getZ() + bx[1].getZ()) / 2.0 - p.getPosZ();
            return cx * cx + cz * cz > r * r;
        });
        return l;
    }

    /** Места, где может вспыхнуть: горючий блок, над ним воздух. */
    public static List<BlockPos> fireSpots(World w, BlockPos min, BlockPos max, int n, Random rnd, @Nullable EconomyData data) {
        List<BlockPos> cand = new ArrayList<>();
        for (BlockPos mp : BlockPos.getAllInBoxMutable(min, max)) {
            BlockState st = w.getBlockState(mp);
            if (st.isAir(w, mp) || !st.isFlammable(w, mp, Direction.UP)) continue;
            BlockPos up = mp.up();
            if (!w.isAirBlock(up) || (data != null && data.isProtected(up))) continue;
            cand.add(up.toImmutable());
        }
        Collections.shuffle(cand, rnd);
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos c : cand) {
            if (out.size() >= n) break;
            boolean far = true;
            for (BlockPos o : out) if (o.distanceSq(c) < 9) far = false;
            if (far) out.add(c);
        }
        return out;
    }

    public static int ignite(World w, List<BlockPos> spots) {
        int n = 0;
        for (BlockPos p : spots) if (w.isAirBlock(p)) { w.setBlockState(p, Blocks.FIRE.getDefaultState(), 11); n++; }
        return n;
    }

    public static int countFire(World w, BlockPos min, BlockPos max) {
        int n = 0;
        for (BlockPos mp : BlockPos.getAllInBoxMutable(min, max)) {
            Block b = w.getBlockState(mp).getBlock();
            if (b == Blocks.FIRE || b == Blocks.SOUL_FIRE) n++;
        }
        return n;
    }

    public static int extinguish(World w, BlockPos min, BlockPos max) {
        int n = 0;
        for (BlockPos mp : BlockPos.getAllInBoxMutable(min, max)) {
            Block b = w.getBlockState(mp).getBlock();
            if (b == Blocks.FIRE || b == Blocks.SOUL_FIRE) { w.setBlockState(mp.toImmutable(), Blocks.AIR.getDefaultState(), 3); n++; }
        }
        return n;
    }

    // ------------------------------------------------------------------ обвал

    /** Блоки потолка над точкой, которые обвалятся (природная порода, 1–2 слоя, радиус 2). */
    public static List<BlockPos> caveIn(World w, BlockPos at, @Nullable EconomyData data) {
        List<BlockPos> l = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                BlockPos c = at.add(dx, 1, dz);
                int y = 0;
                while (y < 6 && w.isAirBlock(c.up(y))) y++;
                if (y >= 6) continue;
                for (int k = 0; k < 2; k++) {
                    BlockPos q = c.up(y + k);
                    if (q.getY() > 250) break;
                    if (!ROCK.contains(w.getBlockState(q).getBlock()) || (data != null && data.isProtected(q))) break;
                    l.add(q.toImmutable());
                }
            }
        return l;
    }

    public static int collapse(World w, List<BlockPos> blocks) {
        int n = 0;
        for (BlockPos q : blocks) {
            if (!ROCK.contains(w.getBlockState(q).getBlock())) continue;
            w.setBlockState(q, Blocks.GRAVEL.getDefaultState(), 3);
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ домушник

    /** Непустой сундук/бочка в своих коробках (самый «жирный» из случайных трёх). */
    @Nullable
    public static BlockPos burglarTarget(World w, List<BlockPos[]> boxes, Random rnd) {
        List<BlockPos> chests = new ArrayList<>();
        for (BlockPos[] bx : boxes)
            for (BlockPos mp : BlockPos.getAllInBoxMutable(bx[0], bx[1])) {
                TileEntity te = w.getTileEntity(mp);
                if (!(te instanceof IInventory) || ((IInventory) te).isEmpty()) continue;
                if (te.getClass().getName().contains("reallife")) continue; // кассы, станки, полки бизнеса не трогает
                chests.add(mp.toImmutable());
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
