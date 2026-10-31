package com.baraaoi.reallife.structure;

import com.baraaoi.reallife.economy.BizBuild;
import com.baraaoi.reallife.economy.BizEquip;
import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.Mall;
import com.baraaoi.reallife.registry.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * v1.11: здания «под ключ» (у каждого вида своя планировка), торговый центр и ларьки ботов-конкурентов.
 * Локальные координаты Blueprint: x — влево (local EAST), z — вглубь (local SOUTH), y=0 — пол.
 */
public final class BizBuildings {
    private BizBuildings() {}

    /** Здание «под ключ»: x −SHOP_X..SHOP_X, z 0..SHOP_Z (у заправки перед магазином ещё площадка с колонками). */
    public static final int SHOP_X = 7, SHOP_Z = 14, SHOP_H = 6, SHOP_FRONT_MIN = -3;

    public static BlockPos shopCounter(Blueprint b) {
        return b.pos(0, 1, 7);
    }

    static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** Повернуть блок лицом в нужную (локальную) сторону. */
    static BlockState facing(BlockState st, Direction d) {
        return st.hasProperty(HorizontalDirectionalBlock.FACING) ? st.setValue(HorizontalDirectionalBlock.FACING, d) : st;
    }

    static BlockState water() {
        return Blocks.CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
    }

    /** Решётка/забор, соединённые вдоль x (east-west) или вдоль z. */
    static BlockState joined(Block b, boolean alongX) {
        BlockState st = b.defaultBlockState();
        return alongX ? st.setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true)
                : st.setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true);
    }

    static void eq(Blueprint b, int x, int z, BizEquip e, Direction d) {
        b.set(x, 1, z, facing(e.block.get().defaultBlockState(), d));
    }

    static void counter(Blueprint b, int x, int z, Direction d) {
        b.set(x, 1, z, facing(ModBlocks.BIZ_COUNTER.get().defaultBlockState(), d));
    }

    /** Стол: забор + ковёр сверху. */
    static void table(Blueprint b, int x, int z, Block carpet) {
        b.set(x, 1, z, s(Blocks.SPRUCE_FENCE));
        b.set(x, 2, z, s(carpet));
    }

    /** Стул из ступеньки: back — где спинка. */
    static void chair(Blueprint b, int x, int z, Direction back) {
        b.set(x, 1, z, b.stairs(s(Blocks.SPRUCE_STAIRS), back));
    }

    /** Перегородка вдоль x на линии z (с y=1 до SHOP_H−1). */
    static void wallAlongX(Blueprint b, int x1, int x2, int z, BlockState st) {
        b.fill(x1, 1, z, x2, SHOP_H - 1, z, st);
    }

    static void wallAlongZ(Blueprint b, int x, int z1, int z2, BlockState st) {
        b.fill(x, 1, z1, x, SHOP_H - 1, z2, st);
    }

    static void door(Blueprint b, int x, int z, Block door, Direction facing) {
        b.door(x, 1, z, s(door), facing);
    }

    static final class Style {
        BlockState wall, trim, floor, roof, glass, awning;

        Style(Block wall, Block trim, Block floor, Block roof, Block glass, Block awning) {
            this.wall = s(wall);
            this.trim = s(trim);
            this.floor = s(floor);
            this.roof = s(roof);
            this.glass = s(glass);
            this.awning = awning == null ? null : s(awning);
        }
    }

    /** Коробка здания: пол, стены, окна, вход 3×3, крыша со светом, козырёк и вывеска. fz — линия фасада. */
    static Blueprint shell(Level w, BlockPos origin, Direction fwd, Style st, int fz, BizType type, String ownerName) {
        Blueprint b = new Blueprint(w, origin, fwd);
        int X = SHOP_X, Z = SHOP_Z, H = SHOP_H;
        b.clear(-X, 0, SHOP_FRONT_MIN, X, H + 2, Z);
        b.foundation(-X, SHOP_FRONT_MIN, X, Z, s(Blocks.STONE_BRICKS));
        b.fill(-X, 0, SHOP_FRONT_MIN, X, 0, fz - 1, s(Blocks.POLISHED_ANDESITE));
        b.fill(-X, 0, fz, X, 0, Z, st.floor);
        b.walls(-X, 1, fz, X, H - 1, Z, st.wall);
        for (int y = 1; y < H; y++) {
            b.set(-X, y, fz, st.trim); b.set(X, y, fz, st.trim); b.set(-X, y, Z, st.trim); b.set(X, y, Z, st.trim);
        }
        for (int x = -X; x <= X; x++) b.set(x, H - 1, fz, st.trim); // полоса под крышей
        for (int y = 2; y <= 3; y++) {
            for (int x = 3; x <= 5; x++) { b.set(x, y, fz, st.glass); b.set(-x, y, fz, st.glass); }
            for (int z = fz + 3; z <= fz + 4 && z < Z; z++) { b.set(X, y, z, st.glass); b.set(-X, y, z, st.glass); }
        }
        b.clear(-1, 1, fz, 1, 3, fz);
        b.fill(-X, H, fz, X, H, Z, st.roof);
        b.walls(-X, H + 1, fz, X, H + 1, Z, st.trim); // парапет
        for (int x = -4; x <= 4; x += 4)
            for (int z = fz + 2; z <= Z - 1; z += 4) {
                b.set(x, H, z, s(Blocks.SEA_LANTERN));
                b.set(x, 0, z, s(Blocks.SEA_LANTERN));
            }
        if (st.awning != null) b.fill(-3, 4, fz - 1, 3, 4, fz - 1, st.awning);
        b.sign(0, 5, fz - 1, s(Blocks.OAK_WALL_SIGN), Direction.NORTH, BizBuild.shortTitle(type), quoted(type.title),
                "владелец:", ownerName == null ? "" : ownerName);
        return b;
    }

    public static Blueprint buildShop(Level w, BlockPos origin, Direction forward, BizType type, String ownerName) {
        switch (type) {
            case SHAWARMA: return shawarma(w, origin, forward, ownerName);
            case MINIMARKET: return minimarket(w, origin, forward, ownerName);
            case CAR_WASH: return carWash(w, origin, forward, ownerName);
            case QUARRY: return quarry(w, origin, forward, ownerName);
            case PAWNSHOP: return pawnshop(w, origin, forward, ownerName);
            case GAS_STATION: return gasStation(w, origin, forward, ownerName);
            default: return electronics(w, origin, forward, ownerName);
        }
    }

    private static final Direction N = Direction.NORTH, S = Direction.SOUTH, E = Direction.EAST, W = Direction.WEST;

    /** Шаурмичная: зал со столиками, стойка отделяет кухню, за кухней — склад. */
    static Blueprint shawarma(Level w, BlockPos o, Direction f, String owner) {
        Style st = new Style(Blocks.DYED_TERRACOTTA.orange(), Blocks.DYED_TERRACOTTA.brown(), Blocks.SPRUCE_PLANKS, Blocks.SMOOTH_STONE,
                Blocks.GLASS, Blocks.WOOL.red());
        Blueprint b = shell(w, o, f, st, 0, BizType.SHAWARMA, owner);
        // кухня (плитка) и склад
        b.fill(-6, 0, 8, 6, 0, 10, s(Blocks.DYED_TERRACOTTA.white()));
        b.fill(-6, 0, 12, 6, 0, 13, s(Blocks.POLISHED_ANDESITE));
        // стойка-раздача по линии z=7, калитка на кухню справа
        for (int x = -6; x <= 6; x++) b.set(x, 1, 7, s(Blocks.STRIPPED_SPRUCE_LOG));
        b.set(6, 1, 7, s(Blocks.SPRUCE_FENCE_GATE).setValue(FenceGateBlock.FACING, N));
        counter(b, 0, 7, N);
        counter(b, -3, 7, N);
        // кухня: грили, холодильники, раковина, коптильня
        eq(b, -4, 10, BizEquip.GRILL, N);
        eq(b, -2, 10, BizEquip.GRILL, N);
        eq(b, 2, 10, BizEquip.FRIDGE, N);
        eq(b, 4, 10, BizEquip.FRIDGE, N);
        b.set(-6, 1, 10, water());
        b.set(6, 1, 10, facing(s(Blocks.SMOKER), N));
        // стена склада с дверью
        wallAlongX(b, -6, 6, 11, st.wall);
        door(b, 5, 11, Blocks.SPRUCE_DOOR, S);
        eq(b, -5, 13, BizEquip.RACK, N);
        b.set(-6, 1, 12, s(Blocks.BARREL));
        b.set(-6, 1, 13, s(Blocks.BARREL));
        b.set(6, 1, 13, s(Blocks.BARREL));
        // зал: 4 столика со стульями
        for (int tx : new int[]{-4, 4})
            for (int tz : new int[]{2, 5}) {
                table(b, tx, tz, Blocks.CARPET.white());
                chair(b, tx - 1, tz, W);
                chair(b, tx + 1, tz, E);
            }
        b.set(-6, 1, 1, s(Blocks.POTTED_FERN));
        b.set(6, 1, 1, s(Blocks.POTTED_FERN));
        return b;
    }

    /** Мини-маркет: полки вдоль стен, холодильники-острова, кассы, служебный проход и склад за стеной. */
    static Blueprint minimarket(Level w, BlockPos o, Direction f, String owner) {
        Style st = new Style(Blocks.CONCRETE.white(), Blocks.CONCRETE.lime(), Blocks.SMOOTH_QUARTZ, Blocks.SMOOTH_STONE,
                Blocks.GLASS, Blocks.WOOL.lime());
        Blueprint b = shell(w, o, f, st, 0, BizType.MINIMARKET, owner);
        b.fill(-6, 0, 8, 6, 0, 13, s(Blocks.POLISHED_ANDESITE));
        for (int z = 1; z <= 6; z++) {
            eq(b, -6, z, BizEquip.SHELF, E);
            eq(b, 6, z, BizEquip.SHELF, W);
        }
        eq(b, -3, 3, BizEquip.FRIDGE, E);
        eq(b, -3, 4, BizEquip.FRIDGE, E);
        eq(b, 3, 3, BizEquip.FRIDGE, W);
        eq(b, 3, 4, BizEquip.FRIDGE, W);
        // линия касс: между кассами — ограждение
        for (int x = -6; x <= 6; x++) b.set(x, 1, 7, joined(Blocks.BIRCH_FENCE, true));
        counter(b, 0, 7, N);
        counter(b, -4, 7, N);
        counter(b, 4, 7, N);
        counter(b, -6, 9, E); // стойка информации/выдачи заказов
        wallAlongX(b, -6, 6, 10, st.wall);
        door(b, 0, 10, Blocks.BIRCH_DOOR, S);
        eq(b, 5, 13, BizEquip.RACK, N);
        b.set(-6, 1, 13, s(Blocks.BARREL));
        b.set(-5, 1, 13, s(Blocks.BARREL));
        b.set(-6, 1, 12, s(Blocks.BARREL));
        b.set(6, 1, 11, water());
        b.set(-2, 1, 1, s(Blocks.POTTED_FERN));
        b.set(2, 1, 1, s(Blocks.POTTED_FERN));
        return b;
    }

    /** Автомойка: два бокса с воротами, между ними офис с кассами, склад автохимии в конце офиса. */
    static Blueprint carWash(Level w, BlockPos o, Direction f, String owner) {
        Style st = new Style(Blocks.CONCRETE.lightGray(), Blocks.CONCRETE.blue(), Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE,
                Blocks.STAINED_GLASS.lightBlue(), Blocks.WOOL.blue());
        Blueprint b = shell(w, o, f, st, 0, BizType.CAR_WASH, owner);
        // стены офиса со стеклом в боксы
        wallAlongZ(b, -2, 1, 13, st.wall);
        wallAlongZ(b, 2, 1, 13, st.wall);
        for (int y = 2; y <= 3; y++)
            for (int z : new int[]{3, 4, 5, 9, 10}) { b.set(-2, y, z, st.glass); b.set(2, y, z, st.glass); }
        // ворота боксов
        b.clear(3, 1, 0, 6, 4, 0);
        b.clear(-6, 1, 0, -3, 4, 0);
        b.fill(-1, 0, 1, 1, 0, 13, s(Blocks.POLISHED_DIORITE));
        for (int z = 2; z <= 12; z += 2) {
            b.set(4, 0, z, s(Blocks.IRON_TRAPDOOR));
            b.set(-4, 0, z, s(Blocks.IRON_TRAPDOOR));
        }
        for (int z : new int[]{3, 7, 11}) {
            eq(b, 6, z, BizEquip.WASHER, W);
            eq(b, -6, z, BizEquip.WASHER, E);
            b.set(5, 5, z, s(Blocks.IRON_CHAIN));
            b.set(-5, 5, z, s(Blocks.IRON_CHAIN));
        }
        b.set(3, 1, 13, water());
        b.set(-3, 1, 13, water());
        counter(b, 0, 7, N);
        counter(b, -1, 4, E);
        counter(b, 1, 10, W);
        wallAlongX(b, -1, 1, 12, st.wall);
        door(b, 0, 12, Blocks.SPRUCE_DOOR, S);
        eq(b, -1, 13, BizEquip.RACK, N);
        b.set(1, 1, 13, s(Blocks.BARREL));
        return b;
    }

    /** Карьер: ангар с воротами, дробилки у дальней стены, стеклянная будка диспетчера, сарай-склад, рельсы. */
    static Blueprint quarry(Level w, BlockPos o, Direction f, String owner) {
        Style st = new Style(Blocks.CONCRETE.gray(), Blocks.CONCRETE.yellow(), Blocks.POLISHED_ANDESITE, Blocks.SMOOTH_STONE,
                Blocks.GLASS, Blocks.WOOL.yellow());
        Blueprint b = shell(w, o, f, st, 0, BizType.QUARRY, owner);
        b.clear(-3, 1, 0, 3, 4, 0); // ворота
        // будка диспетчера
        b.walls(-3, 1, 5, 3, 3, 9, s(Blocks.GLASS));
        b.fill(-3, 4, 5, 3, 4, 9, s(Blocks.SMOOTH_STONE_SLAB));
        b.clear(0, 1, 5, 0, 2, 5); // окошко-проход к кассе
        door(b, 3, 7, Blocks.OAK_DOOR, E);
        counter(b, 0, 7, N);
        counter(b, -2, 7, N);
        counter(b, 2, 6, N);
        counter(b, -2, 8, E);
        // дробилки, предупредительная разметка, рельсы
        for (int x = -6; x <= 6; x++) b.set(x, 0, 11, s((x & 1) == 0 ? Blocks.CONCRETE.yellow() : Blocks.CONCRETE.black()));
        for (int x : new int[]{-5, -2, 2, 5}) eq(b, x, 12, BizEquip.CRUSHER, N);
        for (int z = 1; z <= 10; z++) b.set(-5, 1, z, s(Blocks.RAIL));
        b.set(-6, 1, 13, s(Blocks.STONE));
        b.set(-6, 2, 13, s(Blocks.ANDESITE));
        b.set(6, 1, 13, s(Blocks.STONE));
        b.set(-3, 1, 13, s(Blocks.STONECUTTER));
        // сарай-склад
        wallAlongZ(b, 3, 1, 3, st.wall);
        wallAlongX(b, 3, 6, 4, st.wall);
        b.clear(3, 1, 2, 3, 2, 2);
        door(b, 3, 2, Blocks.SPRUCE_DOOR, E);
        eq(b, 6, 1, BizEquip.RACK, W);
        b.set(5, 1, 3, s(Blocks.BARREL));
        b.set(6, 1, 3, s(Blocks.BARREL));
        return b;
    }

    /** Ломбард: зал с витринами и скамьями, окно оценщика за решёткой, кабинет, хранилище с сейфами и склад. */
    static Blueprint pawnshop(Level w, BlockPos o, Direction f, String owner) {
        Style st = new Style(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE, Blocks.DARK_OAK_PLANKS, Blocks.SMOOTH_STONE,
                Blocks.GLASS, Blocks.WOOL.yellow());
        Blueprint b = shell(w, o, f, st, 0, BizType.PAWNSHOP, owner);
        b.fill(-6, 0, 1, 6, 0, 6, s(Blocks.POLISHED_GRANITE));
        for (int z : new int[]{2, 4}) {
            eq(b, -6, z, BizEquip.DISPLAY, E);
            eq(b, 6, z, BizEquip.DISPLAY, W);
        }
        chair(b, -4, 1, N);
        chair(b, -3, 1, N);
        chair(b, 3, 1, N);
        chair(b, 4, 1, N);
        // барьер с окошками
        for (int x = -6; x <= 6; x++) {
            b.set(x, 1, 7, s(Blocks.POLISHED_BLACKSTONE));
            b.set(x, 2, 7, joined(Blocks.IRON_BARS, true));
            b.set(x, 3, 7, joined(Blocks.IRON_BARS, true));
            b.fill(x, 4, 7, x, SHOP_H - 1, 7, st.wall);
        }
        counter(b, 0, 7, N);
        counter(b, -3, 7, N);
        b.clear(0, 2, 7, 0, 2, 7);
        b.clear(-3, 2, 7, -3, 2, 7);
        // кабинет оценщика
        b.set(4, 1, 8, facing(s(Blocks.LECTERN), N));
        b.set(-5, 1, 8, s(Blocks.ANVIL));
        b.set(5, 1, 9, b.stairs(s(Blocks.DARK_OAK_STAIRS), S));
        // хранилище и склад
        wallAlongX(b, -6, 6, 10, st.wall);
        wallAlongZ(b, 0, 11, 13, st.wall);
        door(b, -3, 10, Blocks.DARK_OAK_DOOR, S);
        door(b, 3, 10, Blocks.DARK_OAK_DOOR, S);
        b.fill(-6, 0, 11, -1, 0, 13, s(Blocks.CHISELED_POLISHED_BLACKSTONE));
        eq(b, -5, 13, BizEquip.SAFE, N);
        eq(b, -3, 13, BizEquip.SAFE, N);
        eq(b, 5, 13, BizEquip.RACK, N);
        b.set(6, 1, 11, s(Blocks.BARREL));
        b.set(6, 1, 12, s(Blocks.BARREL));
        return b;
    }

    /** Заправка: площадка с колонками под навесом, магазин с кассами и полками, склад за стеной. */
    static Blueprint gasStation(Level w, BlockPos o, Direction f, String owner) {
        Style st = new Style(Blocks.CONCRETE.white(), Blocks.CONCRETE.red(), Blocks.SMOOTH_QUARTZ, Blocks.SMOOTH_STONE,
                Blocks.GLASS, null);
        int fz = 4;
        Blueprint b = shell(w, o, f, st, fz, BizType.GAS_STATION, owner);
        // асфальт, разметка, островки
        b.fill(-SHOP_X, 0, SHOP_FRONT_MIN, SHOP_X, 0, fz - 1, s(Blocks.CONCRETE.black()));
        for (int z = SHOP_FRONT_MIN; z < fz; z++) { b.set(-6, 0, z, s(Blocks.CONCRETE.white())); b.set(6, 0, z, s(Blocks.CONCRETE.white())); }
        for (int z = 0; z <= 2; z++) {
            b.set(-4, 0, z, s(Blocks.SMOOTH_STONE));
            b.set(4, 0, z, s(Blocks.SMOOTH_STONE));
            eq(b, -4, z, BizEquip.PUMP, E);
            eq(b, 4, z, BizEquip.PUMP, W);
        }
        // навес
        for (int[] p : new int[][]{{-6, -2}, {6, -2}, {-6, 2}, {6, 2}}) b.fill(p[0], 1, p[1], p[0], 4, p[1], s(Blocks.CONCRETE.white()));
        b.fill(-SHOP_X, 5, SHOP_FRONT_MIN, SHOP_X, 5, fz - 1, s(Blocks.CONCRETE.white()));
        for (int x = -SHOP_X; x <= SHOP_X; x++) b.set(x, 5, SHOP_FRONT_MIN, s(Blocks.CONCRETE.red()));
        for (int x = -4; x <= 4; x += 4) b.set(x, 5, 0, s(Blocks.SEA_LANTERN));
        b.set(-4, 5, 2, s(Blocks.SEA_LANTERN));
        b.set(4, 5, 2, s(Blocks.SEA_LANTERN));
        b.sign(0, 4, fz - 1, s(Blocks.OAK_WALL_SIGN), N, BizBuild.shortTitle(BizType.GAS_STATION), quoted(BizType.GAS_STATION.title),
                "владелец:", owner == null ? "" : owner);
        // магазин
        eq(b, -6, 5, BizEquip.SHELF, E);
        eq(b, -6, 6, BizEquip.SHELF, E);
        eq(b, 6, 5, BizEquip.SHELF, W);
        counter(b, 0, 7, N);
        counter(b, -3, 7, N);
        counter(b, 3, 7, N);
        b.set(5, 1, 9, s(Blocks.BREWING_STAND)); // кофемашина
        b.set(-5, 1, 9, s(Blocks.BARREL));
        wallAlongX(b, -6, 6, 10, st.wall);
        door(b, -4, 10, Blocks.BIRCH_DOOR, S);
        eq(b, 5, 13, BizEquip.RACK, N);
        b.set(-6, 1, 13, s(Blocks.BARREL));
        b.set(-6, 1, 12, s(Blocks.BARREL));
        return b;
    }

    /** Магазин техники: витрины-острова, полки вдоль стен, кассы и сервис, мастерская и склад. */
    static Blueprint electronics(Level w, BlockPos o, Direction f, String owner) {
        Style st = new Style(Blocks.CONCRETE.gray(), Blocks.CONCRETE.lightBlue(), Blocks.SMOOTH_QUARTZ, Blocks.SMOOTH_STONE,
                Blocks.STAINED_GLASS.lightBlue(), Blocks.WOOL.lightBlue());
        Blueprint b = shell(w, o, f, st, 0, BizType.ELECTRONICS, owner);
        for (int x : new int[]{-4, -3, -2, 2, 3, 4}) {
            eq(b, x, 3, BizEquip.DISPLAY, N);
            eq(b, x, 4, BizEquip.DISPLAY, S);
        }
        for (int z = 1; z <= 4; z++) {
            eq(b, -6, z, BizEquip.SHELF, E);
            eq(b, 6, z, BizEquip.SHELF, W);
        }
        // «телевизоры» на стенах
        for (int z = 1; z <= 2; z++) { b.set(-SHOP_X, 3, z, s(Blocks.CONCRETE.black())); b.set(SHOP_X, 3, z, s(Blocks.CONCRETE.black())); }
        counter(b, 0, 7, N);
        counter(b, -3, 7, N);
        counter(b, 3, 7, N);
        counter(b, -6, 6, E); // сервисный центр
        // мастерская
        b.set(-5, 1, 9, s(Blocks.SMITHING_TABLE));
        b.set(-4, 1, 9, s(Blocks.CRAFTING_TABLE));
        b.set(4, 1, 9, s(Blocks.NOTE_BLOCK));
        wallAlongX(b, -6, 6, 10, st.wall);
        door(b, 5, 10, Blocks.BIRCH_DOOR, S);
        eq(b, -5, 13, BizEquip.RACK, N);
        b.set(-6, 1, 13, s(Blocks.BARREL));
        b.set(-6, 1, 12, s(Blocks.BARREL));
        b.set(6, 1, 13, s(Blocks.BARREL));
        return b;
    }

    static String quoted(String title) {
        int i = title.indexOf('«');
        return i >= 0 ? title.substring(i) : "";
    }

    // =====================================================================================
    //  ТОРГОВЫЙ ЦЕНТР v1.11
    // =====================================================================================

    static Block bandColor(int area) {
        return area < 50 ? Blocks.CONCRETE.pink() : area < 100 ? Blocks.CONCRETE.lime() : area < 170 ? Blocks.CONCRETE.lightBlue() : Blocks.CONCRETE.orange();
    }

    /** Стоячая табличка (rotation 0 — лицом на локальный юг, 8 — на север). */
    static void standingSign(Blueprint b, int x, int y, int z, int rotation, String... lines) {
        b.set(x, y, z, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation));
        writeSign(b.world(), b.pos(x, y, z), lines);
    }

    /** Переписать текст таблички (если чанк загружен). */
    public static void writeSign(Level w, BlockPos p, String... lines) {
        if (!w.isLoaded(p)) return;
        BlockEntity te = w.getBlockEntity(p);
        if (!(te instanceof SignBlockEntity)) return;
        SignBlockEntity sign = (SignBlockEntity) te;
        for (int i = 0; i < 4; i++) sign.updateText(_t -> _t.setMessage(i, Component.literal(i < lines.length && lines[i] != null ? lines[i] : "")), true);
        sign.setChanged();
        BlockState st = w.getBlockState(p);
        w.sendBlockUpdated(p, st, st, 3);
    }

    /** Локальные данные помещения: вход, табличка, направление внутрь. */
    public static final class UnitGeo {
        public int ex, ez, sx, sz;
        public Direction into, signFace;
        public int width, depth;
    }

    public static UnitGeo geo(int[] l) {
        UnitGeo g = new UnitGeo();
        int x1 = l[0], z1 = l[1], x2 = l[2], z2 = l[3];
        int xc = (x1 + x2) / 2, zc = (z1 + z2) / 2;
        switch (l[4]) {
            case Mall.FRONT_XNEG: g.ex = x1 - 1; g.ez = zc; g.sx = x1 - 2; g.sz = zc; g.into = E; g.signFace = W;
                g.width = z2 - z1 + 1; g.depth = x2 - x1 + 1; break;
            case Mall.FRONT_XPOS: g.ex = x2 + 1; g.ez = zc; g.sx = x2 + 2; g.sz = zc; g.into = W; g.signFace = E;
                g.width = z2 - z1 + 1; g.depth = x2 - x1 + 1; break;
            default: g.ex = xc; g.ez = z1 - 1; g.sx = xc; g.sz = z1 - 2; g.into = S; g.signFace = N;
                g.width = x2 - x1 + 1; g.depth = z2 - z1 + 1; break;
        }
        return g;
    }

    private static boolean outer(int x, int z) {
        return x <= Mall.X1 || x >= Mall.X2 || z <= Mall.Z1 || z >= Mall.Z2;
    }

    /** Стена ТЦ по линии (не трогая наружные стены со стеклом). */
    private static void partition(Blueprint b, int x1, int z1, int x2, int z2, BlockState st) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                if (outer(x, z)) continue;
                for (int y = 1; y < Mall.H; y++) b.set(x, y, z, st);
            }
    }

    public static Blueprint buildMall(Level w, BlockPos origin, Direction forward, String ownerName) {
        Blueprint b = new Blueprint(w, origin, forward);
        int X1 = Mall.X1, X2 = Mall.X2, Z1 = Mall.Z1, Z2 = Mall.Z2, H = Mall.H;
        BlockState wall = s(Blocks.CONCRETE.white()), glass = s(Blocks.STAINED_GLASS.lightBlue()), lamp = s(Blocks.SEA_LANTERN);
        BlockState pillar = s(Blocks.QUARTZ_PILLAR);
        b.clear(X1, 0, Z1 - 5, X2, H + 2, Z2);
        b.foundation(X1, Z1 - 5, X2, Z2, s(Blocks.STONE_BRICKS));
        // пол: светлый кварц, полосы андезита в атриуме, площадь перед входом
        b.fill(X1, 0, Z1, X2, 0, Z2, s(Blocks.SMOOTH_QUARTZ));
        for (int z = 6; z <= 42; z += 6) b.fill(-17, 0, z, 19, 0, z, s(Blocks.POLISHED_ANDESITE));
        b.fill(-12, 0, Z1 - 5, 12, 0, Z1 - 1, s(Blocks.POLISHED_ANDESITE));
        for (int x = -12; x <= 12; x += 4) b.fill(x, 0, Z1 - 5, x, 0, Z1 - 1, s(Blocks.POLISHED_DIORITE));
        // наружные стены: панорамные окна, кварцевые колонны
        b.walls(X1, 1, Z1, X2, H - 1, Z2, wall);
        for (int y = 3; y <= 8; y++) {
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
            for (int x = X1; x <= X2; x += 12) { b.set(x, y, Z1, pillar); b.set(x, y, Z2, pillar); }
            for (int z = Z1; z <= Z2; z += 14) { b.set(X1, y, z, pillar); b.set(X2, y, z, pillar); }
        }
        // крыша, световой фонарь над атриумом, парапет
        b.fill(X1, H, Z1, X2, H, Z2, wall);
        b.fill(-12, H, 6, 14, H, 38, glass);
        for (int x = X1 + 2; x <= X2 - 2; x += 4)
            for (int z = Z1 + 2; z <= Z2 - 2; z += 4)
                if (!(x >= -12 && x <= 14 && z >= 6 && z <= 38)) b.set(x, H, z, lamp);
        b.walls(X1, H + 1, Z1, X2, H + 1, Z2, s(Blocks.CONCRETE.lightGray()));
        // вход: проём 7×4, козырёк, вывеска
        b.clear(-3, 1, Z1, 3, 4, Z1);
        b.fill(-5, 5, Z1 - 2, 5, 5, Z1 - 1, s(Blocks.SMOOTH_QUARTZ_SLAB));
        b.sign(0, 6, Z1 - 1, s(Blocks.OAK_WALL_SIGN), Direction.NORTH, "ТОРГОВЫЙ ЦЕНТР", ownerName == null ? "" : "«" + ownerName + "»",
                Mall.LAYOUT.length + " помещений", "добро пожаловать");
        // помещения
        for (int i = 0; i < Mall.LAYOUT.length; i++) {
            int[] l = Mall.LAYOUT[i];
            int x1 = l[0], z1 = l[1], x2 = l[2], z2 = l[3];
            b.fill(x1, 0, z1, x2, 0, z2, s(Blocks.POLISHED_ANDESITE));
            partition(b, x1 - 1, z1 - 1, x2 + 1, z1 - 1, wall);
            partition(b, x1 - 1, z2 + 1, x2 + 1, z2 + 1, wall);
            partition(b, x1 - 1, z1 - 1, x1 - 1, z2 + 1, wall);
            partition(b, x2 + 1, z1 - 1, x2 + 1, z2 + 1, wall);
            UnitGeo g = geo(l);
            BlockState band = s(bandColor(Mall.area(l)));
            // витрина: стекло до y4, цветная полоса на y5
            boolean alongZ = l[4] != Mall.FRONT_ZNEG;
            int a1 = alongZ ? z1 : x1, a2 = alongZ ? z2 : x2;
            for (int a = a1; a <= a2; a++) {
                int x = alongZ ? g.ex : a, z = alongZ ? a : g.ez;
                for (int y = 1; y <= 4; y++) b.set(x, y, z, glass);
                b.set(x, 5, z, band);
            }
            int c = alongZ ? g.ez : g.ex;
            for (int d = -1; d <= 1; d++) b.clear(alongZ ? g.ex : c + d, 1, alongZ ? c + d : g.ez, alongZ ? g.ex : c + d, 3, alongZ ? c + d : g.ez);
            b.sign(g.sx, 5, g.sz, s(Blocks.OAK_WALL_SIGN), g.signFace, "№" + (i + 1) + " " + Mall.sizeLabel(Mall.area(l)),
                    g.width + "×" + g.depth, "свободно", "ПКМ — купить");
        }
        // свет в полу (после разметки помещений)
        for (int x = X1 + 1; x < X2; x++)
            for (int z = Z1 + 1; z < Z2; z++)
                if (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0 && w.getBlockState(b.pos(x, 1, z)).isAir(w, b.pos(x, 1, z))) b.set(x, 0, z, lamp);
        // атриум: фонтан, скамейки, кадки с деревьями, колонны
        int fx = 1, fz = 24;
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                boolean ring = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                if (Math.abs(dx) == 3 && Math.abs(dz) == 3) continue;
                if (ring) b.set(fx + dx, 1, fz + dz, s(Blocks.QUARTZ_BRICKS));
                else b.set(fx + dx, 0, fz + dz, s(Blocks.WATER));
            }
        b.fill(fx, 0, fz, fx, 3, fz, pillar);
        b.set(fx, 4, fz, lamp);
        b.set(fx + 2, 1, fz - 1, s(Blocks.LILY_PAD));
        b.set(fx - 2, 1, fz + 2, s(Blocks.LILY_PAD));
        for (int d = -1; d <= 1; d++) {
            b.set(fx + d, 1, fz - 5, b.stairs(s(Blocks.SPRUCE_STAIRS), N));
            b.set(fx + d, 1, fz + 5, b.stairs(s(Blocks.SPRUCE_STAIRS), S));
            b.set(fx - 5, 1, fz + d, b.stairs(s(Blocks.SPRUCE_STAIRS), W));
            b.set(fx + 5, 1, fz + d, b.stairs(s(Blocks.SPRUCE_STAIRS), E));
        }
        BlockState leaves = s(Blocks.OAK_LEAVES).setValue(LeavesBlock.PERSISTENT, true);
        for (int[] p : new int[][]{{-12, 10}, {14, 10}, {-12, 37}, {14, 37}}) {
            int px = p[0], pz = p[1];
            b.fill(px - 1, 1, pz - 1, px + 1, 1, pz + 1, s(Blocks.SPRUCE_PLANKS));
            b.set(px, 1, pz, s(Blocks.GRASS_BLOCK));
            b.fill(px - 1, 4, pz - 1, px + 1, 5, pz + 1, leaves);
            b.set(px, 6, pz, leaves);
            b.fill(px, 2, pz, px, 5, pz, s(Blocks.OAK_LOG));
        }
        for (int[] p : new int[][]{{-12, 18}, {14, 18}, {-12, 30}, {14, 30}}) b.fill(p[0], 1, p[1], p[0], H - 1, p[1], pillar);
        standingSign(b, 6, 1, 3, 8, "Схема ТЦ", Mall.LAYOUT.length + " помещений", "табличка над", "входом: ПКМ");
        return b;
    }

    // =====================================================================================
    //  БОТЫ-КОНКУРЕНТЫ
    // =====================================================================================

    static Block kioskColor(BizType t) {
        switch (t) {
            case SHAWARMA: return Blocks.CONCRETE.orange();
            case MINIMARKET: return Blocks.CONCRETE.green();
            case CAR_WASH: return Blocks.CONCRETE.cyan();
            case QUARRY: return Blocks.CONCRETE.gray();
            case PAWNSHOP: return Blocks.CONCRETE.yellow();
            case GAS_STATION: return Blocks.CONCRETE.red();
            default: return Blocks.CONCRETE.black();
        }
    }

    static Block[] kioskDecor(BizType t) {
        switch (t) {
            case SHAWARMA: return new Block[]{Blocks.SMOKER, Blocks.BARREL};
            case MINIMARKET: return new Block[]{Blocks.BARREL, Blocks.BARREL};
            case CAR_WASH: return new Block[]{Blocks.CAULDRON, Blocks.BARREL};
            case QUARRY: return new Block[]{Blocks.STONECUTTER, Blocks.STONE};
            case PAWNSHOP: return new Block[]{Blocks.LECTERN, Blocks.BARREL};
            case GAS_STATION: return new Block[]{Blocks.BARREL, Blocks.ANVIL};
            default: return new Block[]{Blocks.JUKEBOX, Blocks.NOTE_BLOCK};
        }
    }

    /** Две строки для таблички из названия. */
    public static String[] signLines(String name) {
        if (name.length() <= 15) return new String[]{name, ""};
        int i = name.lastIndexOf(' ', 15);
        if (i <= 0) i = Math.min(15, name.length());
        return new String[]{name.substring(0, i).trim(), name.substring(i).trim()};
    }

    /** Ларёк конкурента 7×7 (с записью исходных блоков — при закрытии всё вернётся). */
    public static Blueprint buildKiosk(Level w, BlockPos origin, Direction forward, BizType type, String name) {
        Blueprint b = new Blueprint(w, origin, forward).recordSnapshot();
        BlockState wall = s(kioskColor(type));
        b.clear(-3, 1, -1, 3, 5, 6);
        b.fill(-3, -1, 0, 3, -1, 6, s(Blocks.STONE_BRICKS));
        b.fill(-3, 0, 0, 3, 0, 6, s(Blocks.SMOOTH_STONE));
        b.walls(-3, 1, 0, 3, 3, 6, wall);
        b.fill(-3, 4, -1, 3, 4, 6, wall);
        for (int x = -2; x <= 2; x++) b.set(x, 2, 0, s(Blocks.GLASS));
        b.clear(0, 1, 0, 0, 2, 0);
        for (int x = -3; x <= 3; x++) b.set(x, 4, -1, s(Blocks.WOOL.white()));
        b.set(0, 4, 3, s(Blocks.SEA_LANTERN));
        for (int x = -1; x <= 1; x++) b.set(x, 1, 3, s(Blocks.SPRUCE_PLANKS));
        Block[] d = kioskDecor(type);
        b.set(-2, 1, 5, facing(s(d[0]), Direction.NORTH));
        b.set(2, 1, 5, facing(s(d[1]), Direction.NORTH));
        String[] nl = signLines(name);
        b.sign(0, 3, -1, s(Blocks.OAK_WALL_SIGN), Direction.NORTH, nl[0], nl[1], "(конкурент)", "ПКМ — выкупить");
        return b;
    }

    /** Бот заехал в помещение ТЦ: прилавок и немного мебели (с записью исходных блоков). */
    public static Blueprint decorateUnit(Level w, Mall.Unit u, BizType type) {
        BlockPos origin = u.entrance.relative(u.facing).below();
        Blueprint b = new Blueprint(w, origin, u.facing).recordSnapshot();
        int half = (u.width - 1) / 2;
        int cz = Math.min(2, Math.max(0, u.depth - 3));
        int span = Math.min(2, half);
        for (int x = -span; x <= span; x++) b.set(x, 1, cz, s(Blocks.SPRUCE_PLANKS));
        Block[] d = kioskDecor(type);
        int back = u.depth - 1;
        if (back > cz) {
            b.set(-Math.min(1, half), 1, back, facing(s(d[0]), Direction.NORTH));
            b.set(Math.min(1, half), 1, back, facing(s(d[1]), Direction.NORTH));
        }
        b.set(0, 2, cz, s(Blocks.LANTERN));
        for (int x = -half; x <= half; x++) if (x != 0 || cz != 0) b.set(x, 1, 0, s(kioskColorCarpet(type)));
        return b;
    }

    static Block kioskColorCarpet(BizType t) {
        switch (t) {
            case SHAWARMA: return Blocks.CARPET.orange();
            case MINIMARKET: return Blocks.CARPET.green();
            case PAWNSHOP: return Blocks.CARPET.yellow();
            default: return Blocks.CARPET.gray();
        }
    }
}
