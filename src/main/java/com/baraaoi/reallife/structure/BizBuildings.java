package com.baraaoi.reallife.structure;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Легальные заведения: шаурмичная, мини-маркет, автомойка, карьер, ломбард, заправка, магазин техники.
 * Все стоят на участке 11×11 (локально x −5..5, z 0..10). Касса — (0,0,8), работник — (0,0,9).
 */
public final class BizBuildings {
    private BizBuildings() {}

    /** Габарит в локальных координатах: x1, y1, z1, x2, y2, z2. */
    public static final int[] BOX = {-5, 0, 0, 5, 6, 10};
    public static final int[] COUNTER = {0, 0, 8};
    public static final int[] WORKER = {0, 0, 9};

    private static BlockState s(Block b) {
        return b.getDefaultState();
    }

    private static BlockState facing(Block b, Direction d) {
        return b.getDefaultState().with(HorizontalBlock.HORIZONTAL_FACING, d);
    }

    private static BlockState slab(Block b) {
        return b.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    /** Габарит заведения в мире (для проверок). [0] — min, [1] — max. */
    public static BlockPos[] box(World w, BlockPos origin, Direction facing) {
        Blueprint b = new Blueprint(w, origin, facing);
        BlockPos p1 = b.pos(BOX[0], BOX[1], BOX[2]), p2 = b.pos(BOX[3], BOX[4], BOX[5]);
        BlockPos min = new BlockPos(Math.min(p1.getX(), p2.getX()), origin.getY() - 1, Math.min(p1.getZ(), p2.getZ()));
        BlockPos max = new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));
        return new BlockPos[]{min, max};
    }

    /** Можно ли строить заведение. null — можно, иначе причина отказа. */
    public static String checkSite(World w, BlockPos origin, Direction facing, EconomyData data, UUID player) {
        BlockPos[] bx = box(w, origin, facing);
        String occ = Buildings.checkOccupied(bx[0], bx[1], data, player);
        if (occ != null) return occ;
        Blueprint b = new Blueprint(w, origin, facing);
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int x = BOX[0]; x <= BOX[3]; x++)
            for (int z = BOX[2]; z <= BOX[5]; z++) {
                BlockPos c = b.pos(x, 0, z);
                int g = Buildings.groundY(w, c.getX(), c.getZ(), origin.getY());
                lo = Math.min(lo, g);
                hi = Math.max(hi, g);
            }
        int tol = Config.HOUSE_FLATNESS.get();
        if (hi - lo > tol) return "площадка неровная: перепад " + (hi - lo) + " бл. (допустимо " + tol + "). Выровняйте участок";
        int base = origin.getY() - 1;
        if (base - lo > tol || hi - base > tol) return "заведение повиснет в воздухе или уйдёт в землю — кликните по земле на ровном месте";
        for (BlockPos p : BlockPos.getAllInBoxMutable(bx[0].up(), bx[1])) {
            if (w.getTileEntity(p) != null) {
                return "на месте стройки чьё-то имущество (" + w.getBlockState(p).getBlock().getTranslatedName().getString()
                        + " на " + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
            }
        }
        return null;
    }

    /** Построить заведение. Касса — b.pos(COUNTER), работник — b.pos(WORKER). */
    public static Blueprint build(World w, BlockPos origin, Direction facing, BizType type) {
        Blueprint b = new Blueprint(w, origin, facing);
        b.clear(BOX[0], 0, BOX[2], BOX[3], BOX[4] + 1, BOX[5]);
        switch (type) {
            case SHAWARMA: shawarma(b); break;
            case MINIMARKET: minimarket(b); break;
            case CAR_WASH: carWash(b); break;
            case QUARRY: quarry(b); break;
            case PAWNSHOP: pawnshop(b); break;
            case GAS_STATION: gasStation(b); break;
            default: electronics(b); break;
        }
        // касса и работник — одинаково для всех
        b.set(COUNTER[0], COUNTER[1], COUNTER[2], facing(ModBlocks.BIZ_COUNTER.get(), Direction.NORTH));
        b.set(WORKER[0], 0, WORKER[2], s(Blocks.AIR));
        b.set(WORKER[0], 1, WORKER[2], s(Blocks.AIR));
        return b;
    }

    // ------------------------------------------------------------------ общая коробка

    /** Коробка магазина от z1 до 10: стены, крыша на y=5, окна, дверь, свет, прилавок на z=8. */
    private static void shop(Blueprint b, int z1, BlockState floor, BlockState wall, BlockState roof, BlockState window,
                             BlockState accent, BlockState counterMat, Block door, Block sign, String... signLines) {
        b.foundation(-5, BOX[2], 5, 10, floor);
        b.fill(-5, -1, z1, 5, -1, 10, floor);
        b.walls(-5, 0, z1, 5, 4, 10, wall);
        b.fill(-5, 5, z1, 5, 5, 10, roof);
        b.fill(-5, 4, z1, 5, 4, z1, accent);
        // окна
        b.fill(-4, 1, z1, -2, 2, z1, window);
        b.fill(2, 1, z1, 4, 2, z1, window);
        if (10 - z1 >= 6) {
            b.fill(-5, 1, z1 + 2, -5, 2, z1 + 3, window);
            b.fill(5, 1, z1 + 2, 5, 2, z1 + 3, window);
        }
        // дверь и вывеска
        b.door(0, 0, z1, s(door), Direction.NORTH);
        b.sign(0, 2, z1 - 1, s(sign), Direction.NORTH, signLines);
        // свет
        int mid = (z1 + 10) / 2;
        b.set(-2, 5, mid, s(Blocks.GLOWSTONE));
        b.set(2, 5, mid, s(Blocks.GLOWSTONE));
        b.set(0, 5, 9, s(Blocks.GLOWSTONE));
        // прилавок
        b.fill(-3, 0, 8, -1, 0, 8, counterMat);
        b.fill(1, 0, 8, 3, 0, 8, counterMat);
    }

    private static void table(Blueprint b, int x, int z) {
        b.set(x, 0, z, s(Blocks.OAK_FENCE));
        b.set(x, 1, z, s(Blocks.OAK_PRESSURE_PLATE));
    }

    // ------------------------------------------------------------------ виды

    private static void shawarma(Blueprint b) {
        shop(b, 2, s(Blocks.SMOOTH_STONE), s(Blocks.WHITE_TERRACOTTA), s(Blocks.RED_TERRACOTTA), s(Blocks.GLASS),
                s(Blocks.ORANGE_TERRACOTTA), s(Blocks.SPRUCE_PLANKS), Blocks.SPRUCE_DOOR, Blocks.SPRUCE_WALL_SIGN,
                "§lШАУРМА", "«У Ашота»", "24 часа", "халяль");
        // уличные столики
        table(b, -3, 0);
        table(b, 3, 0);
        b.set(-4, 0, 0, b.stairs(s(Blocks.SPRUCE_STAIRS), Direction.WEST));
        b.set(4, 0, 0, b.stairs(s(Blocks.SPRUCE_STAIRS), Direction.EAST));
        // кухня за прилавком
        b.set(-2, 0, 9, facing(Blocks.SMOKER, Direction.NORTH));
        b.set(2, 0, 9, facing(Blocks.SMOKER, Direction.NORTH));
        b.set(-1, 0, 9, s(Blocks.CAULDRON));
        b.set(1, 0, 9, s(Blocks.BARREL));
        b.set(-4, 0, 9, s(Blocks.BARREL));
        b.set(4, 0, 9, s(Blocks.BARREL));
        table(b, -3, 4);
        table(b, 3, 4);
    }

    private static void minimarket(Blueprint b) {
        shop(b, 0, s(Blocks.POLISHED_ANDESITE), s(Blocks.LIGHT_GRAY_CONCRETE), s(Blocks.GRAY_CONCRETE), s(Blocks.GLASS),
                s(Blocks.GREEN_CONCRETE), s(Blocks.WHITE_CONCRETE), Blocks.BIRCH_DOOR, Blocks.BIRCH_WALL_SIGN,
                "§2§lКОПЕЕЧКА", "мини-маркет", "продукты", "хозтовары");
        // стеллажи
        b.fill(-4, 0, 2, -4, 2, 6, s(Blocks.BARREL));
        b.fill(-1, 0, 3, 1, 1, 5, s(Blocks.BOOKSHELF));
        for (int z = 2; z <= 5; z++) b.set(4, 0, z, facing(ModBlocks.FRIDGE.get(), Direction.WEST));
        b.set(4, 1, 2, s(Blocks.MELON));
        b.set(4, 1, 4, s(Blocks.PUMPKIN));
        b.set(-3, 0, 9, s(Blocks.BARREL));
        b.set(3, 0, 9, s(Blocks.BARREL));
    }

    private static void carWash(Blueprint b) {
        shop(b, 0, s(Blocks.SMOOTH_STONE), s(Blocks.LIGHT_BLUE_CONCRETE), s(Blocks.WHITE_CONCRETE), s(Blocks.LIGHT_BLUE_STAINED_GLASS),
                s(Blocks.BLUE_CONCRETE), s(Blocks.WHITE_CONCRETE), Blocks.BIRCH_DOOR, Blocks.OAK_WALL_SIGN,
                "§9§lАВТОМОЙКА", "«Чистюля»", "мойка 300₽", "воск в подарок");
        // ворота бокса слева
        b.fill(-4, 0, 0, -2, 2, 0, s(Blocks.AIR));
        for (int z = 1; z <= 6; z++) {
            b.set(-4, -1, z, s(Blocks.BLUE_CONCRETE));
            b.set(-2, -1, z, s(Blocks.BLUE_CONCRETE));
            b.set(-3, -1, z, s(Blocks.IRON_TRAPDOOR));
        }
        b.set(-4, 3, 3, s(Blocks.CAULDRON));
        b.set(-2, 3, 3, s(Blocks.CAULDRON));
        b.set(-4, 0, 6, s(Blocks.CAULDRON));
        b.set(-2, 0, 6, s(Blocks.CAULDRON));
        b.fill(-5, 1, 2, -5, 2, 3, s(Blocks.LIGHT_BLUE_CONCRETE));
        // кладовка с химией
        b.set(4, 0, 2, s(Blocks.BARREL));
        b.set(4, 0, 3, s(Blocks.BARREL));
        b.set(4, 1, 2, s(Blocks.BARREL));
    }

    private static void quarry(Blueprint b) {
        // двор с отвалами, вагончик в глубине
        b.foundation(-5, 0, 5, 10, s(Blocks.GRAVEL));
        for (int x = -5; x <= 5; x++) {
            if (Math.abs(x) > 1) b.set(x, 0, 0, s(Blocks.OAK_FENCE));
        }
        for (int z = 0; z <= 4; z++) {
            b.set(-5, 0, z, s(Blocks.OAK_FENCE));
            b.set(5, 0, z, s(Blocks.OAK_FENCE));
        }
        b.fill(-4, 0, 1, -3, 1, 2, s(Blocks.COBBLESTONE));
        b.set(-4, 2, 1, s(Blocks.COBBLESTONE));
        b.fill(3, 0, 1, 4, 0, 3, s(Blocks.ANDESITE));
        b.set(4, 1, 2, s(Blocks.ANDESITE));
        b.fill(-4, 0, 3, -3, 0, 4, s(Blocks.STONE));
        // «экскаватор»
        b.fill(1, 0, 2, 2, 1, 3, s(Blocks.YELLOW_CONCRETE));
        b.set(2, 2, 3, s(Blocks.BLACK_CONCRETE));
        b.set(1, 2, 2, s(Blocks.IRON_BARS));
        b.set(0, 2, 1, s(Blocks.IRON_BARS));
        b.set(0, 1, 1, s(Blocks.IRON_BARS));
        shop(b, 5, s(Blocks.GRAVEL), s(Blocks.YELLOW_CONCRETE), s(Blocks.GRAY_CONCRETE), s(Blocks.GLASS),
                s(Blocks.BLACK_CONCRETE), s(Blocks.SPRUCE_PLANKS), Blocks.SPRUCE_DOOR, Blocks.SPRUCE_WALL_SIGN,
                "§6§lКАРЬЕР", "«Гранит»", "щебень • камень", "песок");
        b.fill(-5, -1, 5, 5, -1, 10, s(Blocks.SPRUCE_PLANKS));
        b.set(-4, 0, 9, s(Blocks.BARREL));
        b.set(4, 0, 9, facing(Blocks.BLAST_FURNACE, Direction.NORTH));
    }

    private static void pawnshop(Blueprint b) {
        shop(b, 0, s(Blocks.POLISHED_BLACKSTONE), s(Blocks.DARK_OAK_PLANKS), s(Blocks.BLACK_CONCRETE), s(Blocks.IRON_BARS),
                s(Blocks.YELLOW_CONCRETE), s(Blocks.POLISHED_BLACKSTONE_BRICKS), Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_WALL_SIGN,
                "§6§lЛОМБАРД", "«Золотой телец»", "скупка • залог", "§8без вопросов");
        // витрины
        for (int z = 2; z <= 6; z += 2) {
            b.set(-4, 0, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            b.set(-4, 1, z, s(Blocks.GLASS));
            b.set(4, 0, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            b.set(4, 1, z, s(Blocks.GLASS));
        }
        b.set(-3, 0, 9, s(Blocks.BARREL));
        b.set(3, 0, 9, s(Blocks.BARREL));
        b.set(-4, 0, 9, s(Blocks.ANVIL));
    }

    private static void gasStation(Blueprint b) {
        // навес с колонками
        b.foundation(-5, 0, 5, 10, s(Blocks.GRAY_CONCRETE));
        for (int x = -4; x <= 4; x += 8) {
            for (int z = 1; z <= 3; z += 2) b.fill(x, 0, z, x, 3, z, s(Blocks.WHITE_CONCRETE));
        }
        b.fill(-5, 4, 0, 5, 4, 4, s(Blocks.WHITE_CONCRETE));
        b.fill(-5, 4, 0, 5, 4, 0, s(Blocks.RED_CONCRETE));
        b.set(2, 4, 2, s(Blocks.GLOWSTONE));
        b.set(-2, 4, 2, s(Blocks.GLOWSTONE));
        for (int x = -2; x <= 2; x += 4) {
            b.set(x, 0, 2, s(Blocks.RED_CONCRETE));
            b.set(x, 1, 2, s(Blocks.RED_CONCRETE));
            b.set(x, 2, 2, slab(Blocks.SMOOTH_STONE_SLAB));
        }
        shop(b, 5, s(Blocks.GRAY_CONCRETE), s(Blocks.WHITE_CONCRETE), s(Blocks.RED_CONCRETE), s(Blocks.GLASS),
                s(Blocks.YELLOW_CONCRETE), s(Blocks.WHITE_CONCRETE), Blocks.OAK_DOOR, Blocks.OAK_WALL_SIGN,
                "§c§lАЗС", "Роснефть-Лайт", "АИ-95 • ДТ", "кофе с собой");
        b.set(-4, 0, 6, s(Blocks.BARREL));
        b.set(4, 0, 6, facing(ModBlocks.FRIDGE.get(), Direction.WEST));
    }

    private static void electronics(Blueprint b) {
        shop(b, 0, s(Blocks.SMOOTH_QUARTZ), s(Blocks.WHITE_CONCRETE), s(Blocks.GRAY_CONCRETE), s(Blocks.GLASS),
                s(Blocks.RED_CONCRETE), s(Blocks.SMOOTH_QUARTZ), Blocks.OAK_DOOR, Blocks.OAK_WALL_SIGN,
                "§c§lЭЛЬДОРАДИО", "техника", "в кредит", "§80% переплаты*");
        // витрины и «телевизоры»
        for (int z = 2; z <= 6; z++) {
            b.set(-4, 0, z, s(Blocks.SMOOTH_QUARTZ));
            b.set(-4, 1, z, z % 2 == 0 ? s(Blocks.DAYLIGHT_DETECTOR) : s(Blocks.AIR));
            b.set(4, 0, z, s(Blocks.SMOOTH_QUARTZ));
            b.set(4, 1, z, z % 2 == 0 ? s(Blocks.OBSERVER) : s(Blocks.AIR));
        }
        b.fill(-5, 2, 5, -5, 3, 6, s(Blocks.BLACK_CONCRETE));
        b.fill(5, 2, 5, 5, 3, 6, s(Blocks.BLACK_CONCRETE));
        b.fill(-1, 0, 3, 1, 0, 4, s(Blocks.SMOOTH_QUARTZ));
        b.set(0, 1, 3, s(Blocks.SEA_LANTERN));
        b.set(-3, 0, 9, s(Blocks.BARREL));
        b.set(3, 0, 9, s(Blocks.BARREL));
    }
}
