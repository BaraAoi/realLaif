package com.baraaoi.reallife.structure;

import com.baraaoi.reallife.economy.BizBuild;
import com.baraaoi.reallife.economy.BizEquip;
import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.Mall;
import com.baraaoi.reallife.event.Inspections;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CauldronBlock;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.StandingSignBlock;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * v1.10: здания бизнеса «под ключ» и торговые центры.
 * Локальные координаты Blueprint: x — влево, z — вглубь (от игрока), y=0 — пол.
 */
public final class BizBuildings {
    private BizBuildings() {}

    /** Здание «под ключ»: x −SHOP_X..SHOP_X, z 0..SHOP_Z, стены до SHOP_H−1, крыша на SHOP_H. */
    public static final int SHOP_X = 6, SHOP_Z = 12, SHOP_H = 6;

    public static BlockPos shopCounter(Blueprint b) {
        return b.pos(0, 1, 7);
    }

    private static BlockState s(Block b) {
        return b.getDefaultState();
    }

    /** Повернуть оборудование лицом в нужную (локальную) сторону. */
    static BlockState facing(BlockState st, Direction d) {
        return st.hasProperty(HorizontalBlock.HORIZONTAL_FACING) ? st.with(HorizontalBlock.HORIZONTAL_FACING, d) : st;
    }

    static BlockState water() {
        return Blocks.CAULDRON.getDefaultState().with(CauldronBlock.LEVEL, 3);
    }

    /** Список оборудования для максимума работников (без главной кассы — она идёт с лицензией). */
    public static List<BizEquip> fullEquip(BizType t) {
        List<BizEquip> l = new ArrayList<>();
        for (int i = 0; i < t.total(BizEquip.RACK, t.maxWorkers); i++) l.add(BizEquip.RACK);
        for (BizEquip e : BizEquip.values()) {
            if (e == BizEquip.RACK) continue;
            int n = t.total(e, t.maxWorkers) - (e == BizEquip.COUNTER ? 1 : 0);
            for (int i = 0; i < n; i++) l.add(e);
        }
        return l;
    }

    public static Blueprint buildShop(World w, BlockPos origin, Direction forward, BizType type, String ownerName) {
        Blueprint b = new Blueprint(w, origin, forward);
        int X = SHOP_X, Z = SHOP_Z, H = SHOP_H;
        BlockState wall = s(Blocks.WHITE_CONCRETE), trim = s(Blocks.GRAY_CONCRETE), glass = s(Blocks.GLASS);
        BlockState lamp = s(Blocks.SEA_LANTERN), floor = s(Blocks.POLISHED_ANDESITE);
        b.clear(-X, 0, -2, X, H + 2, Z);
        b.foundation(-X, -2, X, Z, s(Blocks.STONE_BRICKS));
        b.fill(-X, 0, 0, X, 0, Z, floor);
        b.fill(-2, 0, -2, 2, 0, -1, s(Blocks.STONE_BRICKS));
        // стены, углы, окна
        b.walls(-X, 1, 0, X, H - 1, Z, wall);
        for (int y = 1; y < H; y++) {
            b.set(-X, y, 0, trim); b.set(X, y, 0, trim); b.set(-X, y, Z, trim); b.set(X, y, Z, trim);
        }
        for (int y = 2; y <= 3; y++) {
            for (int x = 3; x <= 5; x++) { b.set(x, y, 0, glass); b.set(-x, y, 0, glass); }
            for (int z = 3; z <= 4; z++) { b.set(X, y, z, glass); b.set(-X, y, z, glass); }
            for (int z = 8; z <= 9; z++) { b.set(X, y, z, glass); b.set(-X, y, z, glass); }
        }
        // вход 3×3 без двери (покупатели ходят свободно)
        b.clear(-1, 1, 0, 1, 3, 0);
        // крыша с потолочными фонарями и свет в полу
        b.fill(-X, H, 0, X, H, Z, s(Blocks.SMOOTH_STONE));
        for (int x = -4; x <= 4; x += 4)
            for (int z = 2; z <= 10; z += 4) {
                b.set(x, H, z, lamp);
                b.set(x, 0, z, lamp);
            }
        // козырёк и вывеска
        b.fill(-2, 4, -1, 2, 4, -1, s(Blocks.SMOOTH_STONE_SLAB));
        b.sign(0, 5, -1, s(Blocks.OAK_WALL_SIGN), Direction.NORTH, BizBuild.shortTitle(type), quoted(type.title),
                "владелец", ownerName == null ? "" : ownerName);
        // раковина в углу (СЭС требует для еды; другим не мешает)
        b.set(-5, 1, 1, water());
        // оборудование вдоль стен лицом в зал
        List<int[]> spots = new ArrayList<>();
        for (int x = -5; x <= 5; x++) spots.add(new int[]{x, 11, 2});            // задняя стена → лицом к входу (NORTH)
        for (int z = 10; z >= 2; z--) spots.add(new int[]{5, z, 3});             // левая стена → лицом WEST
        for (int z = 10; z >= 2; z--) spots.add(new int[]{-5, z, 1});            // правая стена → лицом EAST
        for (int x = 5; x >= 3; x--) spots.add(new int[]{x, 1, 0});              // у фасада → лицом SOUTH
        for (int x = -3; x >= -4; x--) spots.add(new int[]{x, 1, 0});
        Direction[] dirs = {Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST};
        List<BizEquip> eq = fullEquip(type);
        for (int i = 0; i < eq.size() && i < spots.size(); i++) {
            int[] sp = spots.get(i);
            b.set(sp[0], 1, sp[1], facing(eq.get(i).block.get().getDefaultState(), dirs[sp[2]]));
        }
        // главная касса — в центре, лицом к входу
        b.set(0, 1, 7, facing(com.baraaoi.reallife.registry.ModBlocks.BIZ_COUNTER.get().getDefaultState(), Direction.NORTH));
        return b;
    }

    private static String quoted(String title) {
        int i = title.indexOf('«');
        return i >= 0 ? title.substring(i) : "";
    }

    // =====================================================================================
    //  ТОРГОВЫЙ ЦЕНТР
    // =====================================================================================

    static Block padColor(BizType t) {
        switch (t) {
            case SHAWARMA: return Blocks.ORANGE_CONCRETE;
            case MINIMARKET: return Blocks.LIME_CONCRETE;
            case PAWNSHOP: return Blocks.YELLOW_CONCRETE;
            case ELECTRONICS: return Blocks.LIGHT_BLUE_CONCRETE;
            default: return Blocks.GRAY_CONCRETE;
        }
    }

    static Block padMarker(BizType t) {
        switch (t) {
            case SHAWARMA: return Blocks.ORANGE_GLAZED_TERRACOTTA;
            case MINIMARKET: return Blocks.LIME_GLAZED_TERRACOTTA;
            case PAWNSHOP: return Blocks.YELLOW_GLAZED_TERRACOTTA;
            case ELECTRONICS: return Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA;
            default: return Blocks.GRAY_GLAZED_TERRACOTTA;
        }
    }

    /** Стоячая табличка (rotation 0 — лицом на локальный юг, 8 — на север). */
    static void standingSign(Blueprint b, int x, int y, int z, int rotation, String... lines) {
        b.set(x, y, z, Blocks.OAK_SIGN.getDefaultState().with(StandingSignBlock.ROTATION, rotation));
        BlockPos p = b.pos(x, y, z);
        TileEntity te = b.world().getTileEntity(p);
        if (te instanceof SignTileEntity) {
            SignTileEntity sign = (SignTileEntity) te;
            for (int i = 0; i < 4 && i < lines.length; i++) sign.setText(i, new StringTextComponent(lines[i]));
            sign.markDirty();
            BlockState st = b.world().getBlockState(p);
            b.world().notifyBlockUpdate(p, st, st, 3);
        }
    }

    public static Blueprint buildMall(World w, BlockPos origin, Direction forward, String ownerName) {
        Blueprint b = new Blueprint(w, origin, forward);
        int X1 = Mall.X1, X2 = Mall.X2, Z1 = Mall.Z1, Z2 = Mall.Z2, H = Mall.H;
        BlockState wall = s(Blocks.WHITE_CONCRETE), trim = s(Blocks.LIGHT_GRAY_CONCRETE), glass = s(Blocks.LIGHT_BLUE_STAINED_GLASS);
        BlockState lamp = s(Blocks.SEA_LANTERN), floor = s(Blocks.SMOOTH_QUARTZ);
        b.clear(X1, 0, Z1 - 4, X2, H + 1, Z2);
        b.foundation(X1, Z1 - 4, X2, Z2, s(Blocks.STONE_BRICKS));
        b.fill(X1, 0, Z1, X2, 0, Z2, floor);
        b.fill(-8, 0, Z1 - 4, 8, 0, Z1 - 1, s(Blocks.POLISHED_ANDESITE));
        // стены с панорамными окнами
        b.walls(X1, 1, Z1, X2, H - 1, Z2, wall);
        for (int y = 3; y <= 7; y++) {
            for (int x = X1 + 1; x < X2; x++) {
                if (Math.floorMod(x, 6) == 0) continue;
                b.set(x, y, Z1, glass);
                b.set(x, y, Z2, glass);
            }
            for (int z = Z1 + 1; z < Z2; z++) {
                if (Math.floorMod(z, 6) == 0) continue;
                b.set(X1, y, z, glass);
                b.set(X2, y, z, glass);
            }
        }
        for (int y = 1; y < H; y++) {
            b.set(X1, y, Z1, trim); b.set(X2, y, Z1, trim); b.set(X1, y, Z2, trim); b.set(X2, y, Z2, trim);
        }
        // крыша
        b.fill(X1, H, Z1, X2, H, Z2, wall);
        for (int x = X1 + 2; x <= X2 - 2; x += 4)
            for (int z = Z1 + 2; z <= Z2 - 2; z += 4) b.set(x, H, z, lamp);
        // свет в полу (иначе СЭС найдёт «тёмный зал», а ночью полезут мобы)
        for (int x = X1 + 1; x < X2; x++)
            for (int z = Z1 + 1; z < Z2; z++)
                if (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0) b.set(x, 0, z, lamp);
        // главный вход
        b.clear(-3, 1, Z1, 3, 4, Z1);
        b.fill(-4, 5, Z1 - 1, 4, 5, Z1 - 1, s(Blocks.SMOOTH_QUARTZ_SLAB));
        b.sign(0, 6, Z1 - 1, s(Blocks.OAK_WALL_SIGN), Direction.NORTH, "ТОРГОВЫЙ ЦЕНТР", ownerName == null ? "" : "«" + ownerName + "»",
                "8 мест", "прибыль +50%");
        // разметка мест
        for (int i = 0; i < Mall.LAYOUT.length; i++) {
            BizType t = Mall.LAYOUT[i];
            int cx = Mall.SLOT_X[i], cz = Mall.SLOT_Z[i];
            BlockState pad = s(padColor(t));
            for (int d = -7; d <= 7; d++) {
                b.set(cx + d, 0, cz - 7, pad);
                b.set(cx + d, 0, cz + 7, pad);
                b.set(cx - 7, 0, cz + d, pad);
                b.set(cx + 7, 0, cz + d, pad);
            }
            b.set(cx, 0, cz, s(padMarker(t)));
            boolean frontRow = cz < (Z1 + Z2) / 2;
            int signZ = frontRow ? cz + 7 : cz - 7;     // табличка на краю площадки, лицом в коридор
            standingSign(b, cx, 1, signZ, frontRow ? 0 : 8, "Место под", BizBuild.shortTitle(t), "лицензию — ПКМ", "по центру метки");
            if (Inspections.food(t)) {
                int sinkZ = frontRow ? cz - 6 : cz + 6;  // раковина в дальнем углу площадки
                b.set(cx - 6, 1, sinkZ, water());
            }
        }
        return b;
    }
}
