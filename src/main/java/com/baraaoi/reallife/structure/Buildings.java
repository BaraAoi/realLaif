package com.baraaoi.reallife.structure;

import com.baraaoi.reallife.block.FinanceBlock;
import com.baraaoi.reallife.economy.HouseType;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.Trades;
import com.baraaoi.reallife.entity.SellerEntity;
import com.baraaoi.reallife.registry.ModBlocks;
import com.baraaoi.reallife.registry.ModEntities;
import com.baraaoi.reallife.tile.FridgeTileEntity;
import net.minecraft.block.CauldronBlock;
import net.minecraft.block.TrapDoorBlock;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.properties.Half;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import java.util.UUID;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Все постройки мода: банк, контора МФО, дома для ипотеки, тюрьма и подвал.
 */
public class Buildings {

    static BlockState s(Block b) {
        return b.getDefaultState();
    }

    static BlockState topSlab(Block b) {
        return b.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);
    }

    static BlockState furnace(Block b, Direction d) {
        return b.getDefaultState().with(AbstractFurnaceBlock.FACING, d);
    }

    static BlockState chest(Direction d) {
        return Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, d);
    }

    static BlockState ladder(Direction d) {
        return Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, d);
    }

    static BlockState leaves() {
        return Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
    }

    static BlockState counter(Block b, Direction d) {
        return b.getDefaultState().with(FinanceBlock.HORIZONTAL_FACING, d);
    }

    // =====================================================================================
    //  БАНК
    // =====================================================================================
    public static Blueprint buildBank(World w, BlockPos origin, Direction facing) {
        Blueprint b = new Blueprint(w, origin, facing);
        b.clear(-7, 0, -3, 7, 9, 10);
        b.foundation(-7, -3, 7, 10, s(Blocks.SMOOTH_STONE));
        // ступени и площадка перед входом
        for (int x = -2; x <= 2; x++) b.set(x, 0, -3, b.stairs(s(Blocks.STONE_BRICK_STAIRS), Direction.SOUTH));
        b.fill(-6, 0, -2, 6, 0, -1, s(Blocks.POLISHED_ANDESITE));
        // пол
        b.fill(-5, 0, 1, 5, 0, 8, s(Blocks.POLISHED_DIORITE));
        b.fill(-5, -1, 1, 5, -1, 8, s(Blocks.SMOOTH_STONE));
        // стены
        b.walls(-6, 0, 0, 6, 6, 9, s(Blocks.STONE_BRICKS));
        b.fill(-6, 0, 0, 6, 0, 9, s(Blocks.POLISHED_ANDESITE));
        b.fill(-5, 0, 1, 5, 0, 8, s(Blocks.POLISHED_DIORITE));
        for (int[] c : new int[][]{{-6, 0}, {6, 0}, {-6, 9}, {6, 9}})
            b.fill(c[0], 0, c[1], c[0], 6, c[1], s(Blocks.QUARTZ_PILLAR));
        // колоннада
        for (int x : new int[]{-5, -3, 3, 5}) b.fill(x, 1, -2, x, 5, -2, s(Blocks.QUARTZ_PILLAR));
        b.fill(-6, 6, -2, 6, 6, -1, s(Blocks.QUARTZ_BLOCK));
        b.fill(-6, 7, -2, 6, 7, -2, s(Blocks.SMOOTH_QUARTZ_SLAB));
        // окна
        for (int x : new int[]{-5, -4, 4, 5}) b.fill(x, 2, 0, x, 4, 0, s(Blocks.GLASS));
        for (int z : new int[]{2, 3, 6, 7}) {
            b.fill(-6, 2, z, -6, 4, z, s(Blocks.GLASS));
            b.fill(6, 2, z, 6, 4, z, s(Blocks.GLASS));
        }
        // вход
        b.fill(-1, 1, 0, 1, 3, 0, s(Blocks.AIR));
        b.door(0, 1, 0, s(Blocks.DARK_OAK_DOOR), Direction.NORTH);
        b.set(-1, 1, 0, s(Blocks.GLASS));
        b.set(1, 1, 0, s(Blocks.GLASS));
        b.set(-1, 2, 0, s(Blocks.GLASS));
        b.set(1, 2, 0, s(Blocks.GLASS));
        b.set(-1, 3, 0, s(Blocks.STONE_BRICKS));
        b.set(1, 3, 0, s(Blocks.STONE_BRICKS));
        b.set(0, 3, 0, s(Blocks.CHISELED_STONE_BRICKS));
        b.sign(0, 4, -1, s(Blocks.DARK_OAK_WALL_SIGN), Direction.NORTH, "", "§lБАНК", "«Реал Банк»", "вклады • кредиты");
        b.sign(-2, 2, -1, s(Blocks.DARK_OAK_WALL_SIGN), Direction.NORTH, "Ипотека", "от землянки", "до пентхауса", "");
        b.sign(2, 2, -1, s(Blocks.DARK_OAK_WALL_SIGN), Direction.NORTH, "Часы работы:", "круглосуточно", "", "Без выходных");
        // крыша
        b.fill(-6, 7, 0, 6, 7, 9, s(Blocks.STONE_BRICKS));
        b.walls(-6, 8, 0, 6, 8, 9, s(Blocks.STONE_BRICK_SLAB));
        for (int x : new int[]{-3, 0, 3}) for (int z : new int[]{2, 6}) b.set(x, 7, z, s(Blocks.SEA_LANTERN));
        b.set(-2, 8, 0, s(Blocks.YELLOW_GLAZED_TERRACOTTA));
        b.set(2, 8, 0, s(Blocks.YELLOW_GLAZED_TERRACOTTA));
        // ковровая дорожка
        for (int z = 1; z <= 4; z++) b.set(0, 1, z, s(Blocks.RED_CARPET));
        // стойки кассиров со стеклом
        for (int x = -5; x <= 5; x++) {
            b.set(x, 1, 5, s(Blocks.POLISHED_ANDESITE));
            b.set(x, 2, 5, s(Blocks.GLASS));
        }
        for (int x : new int[]{-3, 0, 3}) {
            b.set(x, 1, 5, counter(ModBlocks.BANK_COUNTER.get(), Direction.NORTH));
            b.set(x, 2, 5, s(Blocks.AIR));
        }
        // хранилище за стойкой
        b.fill(-5, 1, 8, 5, 3, 8, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
        b.fill(-1, 1, 8, 1, 2, 8, s(Blocks.IRON_BARS));
        b.set(-4, 1, 7, s(Blocks.BARREL));
        b.set(4, 1, 7, s(Blocks.BARREL));
        // зал ожидания
        for (int x = -5; x <= -3; x++) b.set(x, 1, 1, b.stairs(s(Blocks.DARK_OAK_STAIRS), Direction.NORTH));
        for (int x = 3; x <= 5; x++) b.set(x, 1, 1, b.stairs(s(Blocks.DARK_OAK_STAIRS), Direction.NORTH));
        // торговые автоматы и банкомат
        b.set(-5, 1, 3, counter(ModBlocks.VENDING_FOOD.get(), Direction.EAST));
        b.set(5, 1, 3, counter(ModBlocks.VENDING_RESOURCES.get(), Direction.WEST));
        b.set(-2, 1, 1, counter(ModBlocks.ATM.get(), Direction.SOUTH));
        b.set(2, 1, 1, s(Blocks.POTTED_FERN));
        // окна продавцов (по краям стойки)
        for (int x : new int[]{-5, 5}) {
            b.set(x, 1, 5, topSlab(Blocks.SMOOTH_QUARTZ_SLAB).with(SlabBlock.TYPE, SlabType.BOTTOM));
            b.set(x, 2, 5, s(Blocks.AIR));
            b.set(x, 1, 6, s(Blocks.AIR));
            b.set(x, 2, 6, s(Blocks.AIR));
        }
        b.set(-4, 1, 6, s(Blocks.BARREL));
        b.set(4, 1, 6, s(Blocks.BARREL));
        spawnSeller(w, b.pos(-5, 1, 6), Trades.SELLER_FOOD);
        spawnSeller(w, b.pos(5, 1, 6), Trades.SELLER_GOODS);
        claimCity(w, b);
        return b;
    }

    // =====================================================================================
    //  КОНТОРА МФО «Шайкин и Ко»
    // =====================================================================================
    public static Blueprint buildMfo(World w, BlockPos origin, Direction facing) {
        Blueprint b = new Blueprint(w, origin, facing);
        Random r = new Random(origin.toLong());
        b.clear(-4, 0, -1, 4, 6, 7);
        b.foundation(-4, -1, 4, 7, s(Blocks.COBBLESTONE));
        b.fill(-2, 0, 1, 2, 0, 5, s(Blocks.COARSE_DIRT));
        // кривые стены
        BlockState[] mix = {s(Blocks.COBBLESTONE), s(Blocks.MOSSY_COBBLESTONE), s(Blocks.OAK_PLANKS), s(Blocks.CRACKED_STONE_BRICKS)};
        for (int y = 0; y <= 3; y++) {
            for (int x = -3; x <= 3; x++) {
                b.set(x, y, 0, mix[r.nextInt(mix.length)]);
                b.set(x, y, 6, mix[r.nextInt(mix.length)]);
            }
            for (int z = 0; z <= 6; z++) {
                b.set(-3, y, z, mix[r.nextInt(mix.length)]);
                b.set(3, y, z, mix[r.nextInt(mix.length)]);
            }
        }
        b.fill(-3, 0, 1, 3, 0, 5, s(Blocks.COARSE_DIRT));
        b.fill(-2, 0, 1, 2, 0, 5, s(Blocks.COARSE_DIRT));
        b.fill(-2, 1, 1, 2, 3, 5, s(Blocks.AIR));
        // окна с решётками
        b.set(-2, 2, 0, s(Blocks.IRON_BARS));
        b.set(2, 2, 0, s(Blocks.IRON_BARS));
        b.set(-3, 2, 3, s(Blocks.IRON_BARS));
        b.set(3, 2, 3, s(Blocks.IRON_BARS));
        // дверь
        b.door(0, 1, 0, s(Blocks.OAK_DOOR), Direction.NORTH);
        // крыша из плит с дырками
        for (int x = -4; x <= 4; x++)
            for (int z = -1; z <= 7; z++)
                b.set(x, 4, z, r.nextInt(12) == 0 ? s(Blocks.AIR) : (r.nextBoolean() ? s(Blocks.OAK_SLAB) : s(Blocks.COBBLESTONE_SLAB)));
        b.fill(-2, 4, 1, 2, 4, 5, s(Blocks.OAK_PLANKS));
        // вывески
        b.sign(-2, 3, -1, s(Blocks.OAK_WALL_SIGN), Direction.NORTH, "§4§lЗАЙМЫ", "§0БЕЗ ОТКАЗА", "§0ЗА 5 МИНУТ!", "§20%*");
        b.sign(2, 3, -1, s(Blocks.OAK_WALL_SIGN), Direction.NORTH, "§8*в первую", "§8секунду. Далее", "§810%/день", "§8сложный %");
        b.sign(1, 1, -1, s(Blocks.OAK_WALL_SIGN), Direction.NORTH, "", "Шайкин и Ко", "§8паспорт", "§8не нужен");
        // внутри
        b.set(0, 1, 5, counter(ModBlocks.MFO_COUNTER.get(), Direction.NORTH));
        b.set(-1, 1, 5, s(Blocks.SPRUCE_PLANKS));
        b.set(1, 1, 5, s(Blocks.SPRUCE_PLANKS));
        b.set(-2, 1, 5, s(Blocks.CAULDRON));
        b.set(2, 1, 5, s(Blocks.LANTERN));
        b.set(-2, 3, 5, s(Blocks.COBWEB));
        b.set(2, 3, 1, s(Blocks.COBWEB));
        b.set(-2, 1, 1, s(Blocks.BARREL));
        b.set(2, 1, 2, chest(Direction.WEST));
        b.set(-1, 1, 1, counter(ModBlocks.MONEY_PRINTER.get(), Direction.SOUTH));
        claimCity(w, b);
        if (w instanceof ServerWorld && w.getServer() != null && w.getDimensionKey() == World.OVERWORLD) {
            EconomyData data = EconomyData.get(w.getServer());
            BlockPos c = b.pos(0, 0, 3);
            boolean known = false;
            for (BlockPos m : data.mfoCenters) if (m.distanceSq(c) < 9) known = true;
            if (!known) {
                data.mfoCenters.add(c);
                data.markDirty();
            }
        }
        return b;
    }

    // =====================================================================================
    //  ДОМА ПО ИПОТЕКЕ
    // =====================================================================================
    public static Blueprint buildHouse(World w, BlockPos origin, Direction facing, HouseType type) {
        Blueprint b;
        switch (type) {
            case DIRT_HUT: b = dirtHut(w, origin, facing); break;
            case WOODEN_HUT: b = woodenHut(w, origin, facing); break;
            case STONE_HOUSE: b = stoneHouse(w, origin, facing); break;
            case BRICK_COTTAGE: b = brickCottage(w, origin, facing); break;
            case MANSION: b = mansion(w, origin, facing); break;
            default: b = penthouse(w, origin, facing); break;
        }
        furnish(b, type.ordinal(), new Random(origin.toLong() ^ 0x5EEDL));
        return b;
    }

    /** Землянка из грязи 5×5. */
    static Blueprint dirtHut(World w, BlockPos o, Direction f) {
        Blueprint b = new Blueprint(w, o, f);
        b.clear(-2, 0, 0, 2, 4, 4);
        b.foundation(-2, 0, 2, 4, s(Blocks.DIRT));
        b.walls(-2, 0, 0, 2, 2, 4, s(Blocks.DIRT));
        b.set(-2, 1, 2, s(Blocks.COARSE_DIRT));
        b.set(2, 1, 2, s(Blocks.COARSE_DIRT));
        b.fill(-2, 3, 0, 2, 3, 4, s(Blocks.GRASS_BLOCK));
        b.set(0, 4, 2, s(Blocks.GRASS));
        b.door(0, 0, 0, s(Blocks.OAK_DOOR), Direction.NORTH);
        b.set(-1, 0, 3, s(Blocks.CRAFTING_TABLE));
        b.set(1, 0, 3, s(Blocks.HAY_BLOCK));
        b.set(1, 0, 1, s(Blocks.TORCH));
        b.set(2, 1, 2, s(Blocks.DIRT));
        b.set(0, 0, 3, chest(Direction.NORTH));
        return b;
    }

    /** Деревянная изба 7×7. */
    static Blueprint woodenHut(World w, BlockPos o, Direction f) {
        Blueprint b = new Blueprint(w, o, f);
        b.clear(-4, 0, -1, 4, 9, 7);
        b.foundation(-3, 0, 3, 6, s(Blocks.COBBLESTONE));
        b.fill(-2, -1, 1, 2, -1, 5, s(Blocks.OAK_PLANKS));
        b.walls(-3, 0, 0, 3, 3, 6, s(Blocks.OAK_PLANKS));
        for (int[] c : new int[][]{{-3, 0}, {3, 0}, {-3, 6}, {3, 6}}) b.fill(c[0], 0, c[1], c[0], 3, c[1], s(Blocks.OAK_LOG));
        for (int y = 1; y <= 2; y++) {
            b.set(-2, y, 0, s(Blocks.GLASS)); b.set(2, y, 0, s(Blocks.GLASS));
            b.set(-3, y, 3, s(Blocks.GLASS)); b.set(3, y, 3, s(Blocks.GLASS));
            b.set(0, y, 6, s(Blocks.GLASS));
        }
        b.door(0, 0, 0, s(Blocks.OAK_DOOR), Direction.NORTH);
        b.gableRoof(-4, 4, -1, 7, 4, s(Blocks.SPRUCE_STAIRS), s(Blocks.OAK_PLANKS), s(Blocks.SPRUCE_PLANKS));
        b.bed(-2, 0, 4, s(Blocks.RED_BED), Direction.SOUTH);
        b.set(2, 0, 5, chest(Direction.WEST));
        b.set(2, 0, 4, s(Blocks.CRAFTING_TABLE));
        b.set(2, 0, 3, furnace(Blocks.FURNACE, Direction.WEST));
        b.set(-2, 0, 1, s(Blocks.TORCH));
        b.set(2, 0, 1, s(Blocks.TORCH));
        b.set(0, 0, 3, s(Blocks.BROWN_CARPET));
        b.set(-2, 0, 2, counter(ModBlocks.FRIDGE.get(), Direction.EAST));
        b.set(-2, 0, 3, chest(Direction.EAST));
        return b;
    }

    /** Каменный дом 9×8. */
    static Blueprint stoneHouse(World w, BlockPos o, Direction f) {
        Blueprint b = new Blueprint(w, o, f);
        b.clear(-5, 0, -1, 5, 10, 8);
        b.foundation(-4, 0, 4, 7, s(Blocks.COBBLESTONE));
        b.fill(-3, -1, 1, 3, -1, 6, s(Blocks.SPRUCE_PLANKS));
        b.walls(-4, 0, 0, 4, 3, 7, s(Blocks.STONE_BRICKS));
        b.walls(-4, 0, 0, 4, 0, 7, s(Blocks.COBBLESTONE));
        for (int[] c : new int[][]{{-4, 0}, {4, 0}, {-4, 7}, {4, 7}}) b.fill(c[0], 0, c[1], c[0], 3, c[1], s(Blocks.POLISHED_ANDESITE));
        for (int y = 1; y <= 2; y++) {
            for (int x : new int[]{-3, -2, 2, 3}) b.set(x, y, 0, s(Blocks.GLASS));
            for (int z : new int[]{2, 3, 5}) { b.set(-4, y, z, s(Blocks.GLASS)); b.set(4, y, z, s(Blocks.GLASS)); }
        }
        for (int x = -1; x <= 1; x++) b.set(x, 2, 7, s(Blocks.GLASS));
        b.door(0, 0, 0, s(Blocks.SPRUCE_DOOR), Direction.NORTH);
        b.gableRoof(-5, 5, -1, 8, 4, s(Blocks.SPRUCE_STAIRS), s(Blocks.STONE_BRICKS), s(Blocks.SPRUCE_PLANKS));
        b.bed(-3, 0, 5, s(Blocks.BLUE_BED), Direction.SOUTH);
        b.set(-3, 0, 1, s(Blocks.BOOKSHELF));
        b.set(-3, 1, 1, s(Blocks.BOOKSHELF));
        b.set(-3, 0, 3, s(Blocks.POTTED_POPPY));
        b.set(3, 0, 6, chest(Direction.WEST));
        b.set(3, 0, 5, chest(Direction.WEST));
        b.set(3, 0, 4, furnace(Blocks.FURNACE, Direction.WEST));
        b.set(3, 0, 3, s(Blocks.CRAFTING_TABLE));
        b.set(0, 0, 4, s(Blocks.OAK_FENCE));
        b.set(0, 1, 4, s(Blocks.OAK_PRESSURE_PLATE));
        b.set(-1, 0, 4, b.stairs(s(Blocks.OAK_STAIRS), Direction.WEST));
        b.set(1, 0, 4, b.stairs(s(Blocks.OAK_STAIRS), Direction.EAST));
        for (int x = -1; x <= 1; x++) b.set(x, 0, 2, s(Blocks.RED_CARPET));
        // тумбочка с лампой у изголовья (раньше лампа стояла прямо в кровати)
        b.set(-2, 0, 6, s(Blocks.SPRUCE_PLANKS));
        b.set(-2, 1, 6, s(Blocks.LANTERN));
        b.set(3, 0, 1, s(Blocks.LANTERN));
        b.set(2, 0, 6, counter(ModBlocks.FRIDGE.get(), Direction.NORTH));
        b.set(2, 1, 6, counter(ModBlocks.FRIDGE.get(), Direction.NORTH));
        b.set(1, 0, 6, chest(Direction.NORTH));
        b.set(-3, 0, 2, s(Blocks.BARREL));
        return b;
    }

    /** Двухэтажный кирпичный коттедж 11×10. */
    static Blueprint brickCottage(World w, BlockPos o, Direction f) {
        Blueprint b = new Blueprint(w, o, f);
        b.clear(-6, 0, -2, 6, 15, 10);
        b.foundation(-5, -2, 5, 9, s(Blocks.STONE_BRICKS));
        b.fill(-4, -1, 1, 4, -1, 8, s(Blocks.OAK_PLANKS));
        b.walls(-5, 0, 0, 5, 7, 9, s(Blocks.BRICKS));
        b.walls(-5, 4, 0, 5, 4, 9, s(Blocks.STONE_BRICKS));
        for (int[] c : new int[][]{{-5, 0}, {5, 0}, {-5, 9}, {5, 9}}) b.fill(c[0], 0, c[1], c[0], 7, c[1], s(Blocks.DARK_OAK_LOG));
        for (int y : new int[]{1, 2, 5, 6}) {
            for (int x : new int[]{-3, -2, 2, 3}) b.set(x, y, 0, s(Blocks.GLASS));
            for (int z : new int[]{3, 6}) { b.set(-5, y, z, s(Blocks.GLASS)); b.set(5, y, z, s(Blocks.GLASS)); }
            for (int x : new int[]{-2, 2}) b.set(x, y, 9, s(Blocks.GLASS));
        }
        b.set(0, 5, 0, s(Blocks.GLASS)); b.set(0, 6, 0, s(Blocks.GLASS));
        b.door(0, 0, 0, s(Blocks.DARK_OAK_DOOR), Direction.NORTH);
        // козырёк над входом
        for (int x = -1; x <= 1; x++) b.set(x, 3, -1, topSlab(Blocks.DARK_OAK_SLAB));
        b.set(-1, 0, -1, s(Blocks.POTTED_RED_TULIP));
        b.set(1, 0, -1, s(Blocks.POTTED_RED_TULIP));
        // второй этаж
        b.fill(-4, 4, 1, 4, 4, 8, s(Blocks.OAK_PLANKS));
        b.set(4, 4, 8, s(Blocks.AIR));
        for (int y = 0; y <= 5; y++) b.set(4, y, 8, ladder(Direction.NORTH));
        b.set(4, 4, 7, s(Blocks.OAK_PLANKS));
        b.gableRoof(-6, 6, -1, 10, 8, s(Blocks.DARK_OAK_STAIRS), s(Blocks.BRICKS), s(Blocks.DARK_OAK_PLANKS));
        b.fill(-3, 8, 7, -3, 13, 7, s(Blocks.BRICKS));
        b.set(-3, 14, 7, s(Blocks.BRICK_WALL));
        // 1 этаж: кухня и гостиная
        b.set(4, 0, 1, furnace(Blocks.SMOKER, Direction.WEST));
        b.set(4, 0, 2, furnace(Blocks.FURNACE, Direction.WEST));
        b.set(4, 0, 3, s(Blocks.CRAFTING_TABLE));
        b.set(4, 0, 4, s(Blocks.BARREL));
        b.set(-4, 0, 8, chest(Direction.NORTH));
        b.set(-3, 0, 8, chest(Direction.NORTH));
        for (int z = 3; z <= 5; z++) for (int y = 0; y <= 2; y++) b.set(-4, y, z, s(Blocks.BOOKSHELF));
        b.set(0, 0, 5, s(Blocks.DARK_OAK_FENCE));
        b.set(0, 1, 5, s(Blocks.DARK_OAK_PRESSURE_PLATE));
        b.set(-1, 0, 5, b.stairs(s(Blocks.SPRUCE_STAIRS), Direction.WEST));
        b.set(1, 0, 5, b.stairs(s(Blocks.SPRUCE_STAIRS), Direction.EAST));
        for (int x = -2; x <= 2; x++) for (int z = 2; z <= 3; z++) b.set(x, 0, z, s(Blocks.GREEN_CARPET));
        b.set(-4, 0, 1, s(Blocks.LANTERN));
        b.set(2, 0, 8, s(Blocks.LANTERN));
        // 2 этаж: спальни
        b.bed(-3, 5, 6, s(Blocks.WHITE_BED), Direction.SOUTH);
        b.bed(-1, 5, 6, s(Blocks.WHITE_BED), Direction.SOUTH);
        b.set(-4, 5, 8, chest(Direction.EAST));
        b.set(2, 5, 8, s(Blocks.BOOKSHELF));
        b.set(1, 5, 8, s(Blocks.POTTED_BLUE_ORCHID));
        for (int x = -2; x <= 2; x++) for (int z = 2; z <= 4; z++) b.set(x, 5, z, s(Blocks.LIGHT_GRAY_CARPET));
        b.set(-4, 5, 1, s(Blocks.LANTERN));
        b.set(4, 5, 1, s(Blocks.LANTERN));
        // кухня: холодильник и ещё сундук
        b.set(4, 0, 5, counter(ModBlocks.FRIDGE.get(), Direction.WEST));
        b.set(4, 1, 5, counter(ModBlocks.FRIDGE.get(), Direction.WEST));
        b.set(-2, 0, 8, chest(Direction.NORTH));
        // 2 этаж: гардероб, ванная, кабинет, растения
        b.set(-4, 5, 3, s(Blocks.BARREL)); b.set(-4, 6, 3, s(Blocks.BARREL));
        b.set(-4, 5, 4, s(Blocks.BARREL)); b.set(-4, 6, 4, s(Blocks.BARREL));
        b.set(3, 5, 6, s(Blocks.CAULDRON).with(CauldronBlock.LEVEL, 3));
        b.set(3, 5, 5, s(Blocks.QUARTZ_SLAB));
        b.set(2, 5, 6, s(Blocks.WHITE_CARPET));
        b.set(4, 5, 2, s(Blocks.LECTERN));
        b.set(3, 5, 2, b.stairs(s(Blocks.SPRUCE_STAIRS), Direction.WEST));
        b.set(4, 5, 3, s(Blocks.BOOKSHELF));
        b.set(-1, 5, 1, s(Blocks.POTTED_FERN));
        b.set(1, 5, 1, s(Blocks.POTTED_CACTUS));
        b.set(0, 5, 8, chest(Direction.NORTH));
        return b;
    }

    /** Двухэтажный особняк 15×13 с садом и балконом. */
    static Blueprint mansion(World w, BlockPos o, Direction f) {
        Blueprint b = new Blueprint(w, o, f);
        b.clear(-8, 0, -4, 8, 19, 13);
        b.foundation(-8, -4, 8, 13, s(Blocks.STONE_BRICKS));
        // сад
        b.fill(-8, -1, -4, 8, -1, -1, s(Blocks.GRASS_BLOCK));
        b.fill(-1, -1, -4, 1, -1, -1, s(Blocks.SMOOTH_STONE));
        Random r = new Random(o.toLong());
        BlockState[] flowers = {s(Blocks.POPPY), s(Blocks.DANDELION), s(Blocks.AZURE_BLUET), s(Blocks.OXEYE_DAISY), s(Blocks.CORNFLOWER), leaves()};
        for (int x = -7; x <= 7; x++) {
            if (Math.abs(x) <= 2) continue;
            for (int z = -4; z <= -2; z++) if (r.nextInt(3) == 0) b.set(x, 0, z, flowers[r.nextInt(flowers.length)]);
        }
        for (int x : new int[]{-8, 8}) for (int z = -4; z <= -1; z++) b.set(x, 0, z, leaves());
        b.fill(-6, -1, 1, 6, -1, 11, s(Blocks.DARK_OAK_PLANKS));
        // стены
        b.walls(-7, 0, 0, 7, 9, 12, s(Blocks.QUARTZ_BLOCK));
        b.walls(-7, 5, 0, 7, 5, 12, s(Blocks.DARK_OAK_PLANKS));
        for (int x : new int[]{-7, -4, 4, 7}) {
            b.fill(x, 0, 0, x, 9, 0, s(Blocks.DARK_OAK_LOG));
            b.fill(x, 0, 12, x, 9, 12, s(Blocks.DARK_OAK_LOG));
        }
        for (int z : new int[]{4, 8}) { b.fill(-7, 0, z, -7, 9, z, s(Blocks.DARK_OAK_LOG)); b.fill(7, 0, z, 7, 9, z, s(Blocks.DARK_OAK_LOG)); }
        for (int y : new int[]{1, 2, 3, 6, 7, 8}) {
            for (int x : new int[]{-6, -5, -2, 2, 5, 6}) b.set(x, y, 0, s(Blocks.GLASS));
            for (int z : new int[]{2, 3, 6, 10}) { b.set(-7, y, z, s(Blocks.GLASS)); b.set(7, y, z, s(Blocks.GLASS)); }
            for (int x : new int[]{-5, -1, 0, 1, 5}) b.set(x, y, 12, s(Blocks.GLASS));
        }
        // балконная дверь — проём на втором этаже
        b.fill(-1, 6, 0, 1, 8, 0, s(Blocks.GLASS));
        b.set(0, 6, 0, s(Blocks.AIR)); b.set(0, 7, 0, s(Blocks.AIR));
        b.door(0, 0, 0, s(Blocks.DARK_OAK_DOOR), Direction.NORTH);
        // портик и балкон
        for (int x : new int[]{-3, 3}) b.fill(x, 0, -3, x, 4, -3, s(Blocks.QUARTZ_PILLAR));
        b.fill(-3, 5, -3, 3, 5, -1, s(Blocks.SMOOTH_QUARTZ));
        for (int x = -3; x <= 3; x++) b.set(x, 6, -3, s(Blocks.DARK_OAK_FENCE));
        for (int z = -2; z <= -1; z++) { b.set(-3, 6, z, s(Blocks.DARK_OAK_FENCE)); b.set(3, 6, z, s(Blocks.DARK_OAK_FENCE)); }
        b.set(-2, 6, -2, s(Blocks.POTTED_RED_TULIP));
        b.set(-1, 4, -1, s(Blocks.LANTERN));
        // второй этаж с лестницей
        b.fill(-6, 5, 1, 6, 5, 11, s(Blocks.DARK_OAK_PLANKS));
        for (int x : new int[]{-4, 0, 4}) for (int z : new int[]{3, 8}) b.set(x, 5, z, s(Blocks.GLOWSTONE));
        for (int z = 6; z <= 11; z++) b.set(6, z - 6, z, b.stairs(s(Blocks.DARK_OAK_STAIRS), Direction.SOUTH));
        for (int z = 8; z <= 10; z++) { b.set(6, 5, z, s(Blocks.AIR)); b.set(5, 6, z, s(Blocks.DARK_OAK_FENCE)); }
        b.gableRoof(-8, 8, -1, 13, 10, s(Blocks.DARK_OAK_STAIRS), s(Blocks.QUARTZ_BLOCK), s(Blocks.DARK_OAK_PLANKS));
        // 1 этаж: гостиная, камин, столовая, кухня, библиотека
        for (int x = -5; x <= -1; x++) for (int z = 2; z <= 6; z++) b.set(x, 0, z, s(Blocks.RED_CARPET));
        for (int x = -5; x <= -3; x++) b.set(x, 0, 7, b.stairs(s(Blocks.DARK_OAK_STAIRS), Direction.SOUTH));
        b.fill(-5, 0, 11, -3, 2, 11, s(Blocks.BRICKS));
        b.set(-4, 0, 11, s(Blocks.CAMPFIRE));
        b.set(-4, 1, 11, s(Blocks.AIR));
        for (int x = 2; x <= 4; x++) for (int z = 3; z <= 4; z++) { b.set(x, 0, z, s(Blocks.DARK_OAK_FENCE)); b.set(x, 1, z, s(Blocks.WHITE_CARPET)); }
        for (int x = 2; x <= 4; x++) { b.set(x, 0, 2, b.stairs(s(Blocks.SPRUCE_STAIRS), Direction.SOUTH)); b.set(x, 0, 5, b.stairs(s(Blocks.SPRUCE_STAIRS), Direction.NORTH)); }
        b.set(6, 0, 1, furnace(Blocks.SMOKER, Direction.WEST));
        b.set(6, 0, 2, furnace(Blocks.FURNACE, Direction.WEST));
        b.set(6, 0, 3, s(Blocks.CRAFTING_TABLE));
        b.set(6, 0, 4, s(Blocks.BARREL));
        for (int z = 8; z <= 11; z++) for (int y = 0; y <= 2; y++) b.set(-6, y, z, s(Blocks.BOOKSHELF));
        b.set(-1, 0, 9, s(Blocks.ENCHANTING_TABLE));
        b.set(1, 0, 11, chest(Direction.NORTH));
        b.set(2, 0, 11, chest(Direction.NORTH));
        // 2 этаж: спальни
        b.bed(-5, 6, 9, s(Blocks.LIGHT_BLUE_BED), Direction.SOUTH);
        b.bed(-3, 6, 9, s(Blocks.PINK_BED), Direction.SOUTH);
        b.bed(1, 6, 9, s(Blocks.RED_BED), Direction.SOUTH);
        b.bed(2, 6, 9, s(Blocks.RED_BED), Direction.SOUTH);
        for (int x = 0; x <= 3; x++) for (int z = 6; z <= 8; z++) b.set(x, 6, z, s(Blocks.WHITE_CARPET));
        b.set(-6, 6, 11, chest(Direction.EAST));
        b.set(-6, 6, 10, chest(Direction.EAST));
        b.set(4, 6, 11, s(Blocks.BOOKSHELF));
        b.set(-6, 6, 1, s(Blocks.LANTERN));
        b.set(6, 6, 1, s(Blocks.LANTERN));
        b.set(-1, 6, 11, s(Blocks.JUKEBOX));
        // 1 этаж: банкомат, холодильник, ещё сундуки
        b.set(-6, 0, 1, counter(ModBlocks.ATM.get(), Direction.EAST));
        b.set(6, 0, 5, counter(ModBlocks.FRIDGE.get(), Direction.WEST));
        b.set(6, 1, 5, counter(ModBlocks.FRIDGE.get(), Direction.WEST));
        b.set(3, 0, 11, chest(Direction.NORTH));
        // 2 этаж (передняя часть): бильярдная, гардероб, ванная, зимний сад
        for (int x = -1; x <= 1; x++) for (int z = 2; z <= 3; z++) {
            b.set(x, 6, z, s(Blocks.DARK_OAK_FENCE));
            b.set(x, 7, z, s(Blocks.GREEN_CARPET));
        }
        b.set(-3, 6, 2, s(Blocks.DARK_OAK_FENCE));
        b.set(-6, 6, 3, s(Blocks.BARREL)); b.set(-6, 7, 3, s(Blocks.BARREL));
        b.set(-6, 6, 4, s(Blocks.BARREL)); b.set(-6, 7, 4, s(Blocks.BARREL));
        b.set(-6, 6, 5, chest(Direction.EAST));
        b.set(4, 6, 1, s(Blocks.CAULDRON).with(CauldronBlock.LEVEL, 3));
        b.set(5, 6, 1, s(Blocks.CAULDRON).with(CauldronBlock.LEVEL, 3));
        for (int x = 3; x <= 5; x++) b.set(x, 6, 2, s(Blocks.WHITE_CARPET));
        b.set(5, 6, 3, s(Blocks.POTTED_BAMBOO));
        b.set(3, 6, 4, s(Blocks.POTTED_ORANGE_TULIP));
        b.set(-5, 6, 1, s(Blocks.POTTED_FERN));
        // кабинет
        b.set(-2, 6, 7, s(Blocks.LECTERN));
        b.set(-3, 6, 7, b.stairs(s(Blocks.DARK_OAK_STAIRS), Direction.EAST));
        b.set(-6, 6, 7, s(Blocks.BOOKSHELF)); b.set(-6, 7, 7, s(Blocks.BOOKSHELF));
        b.set(-6, 6, 8, s(Blocks.BOOKSHELF)); b.set(-6, 7, 8, s(Blocks.BOOKSHELF));
        return b;
    }

    /** Башня 13×13 на 4 уровня, пентхаус наверху с террасой и бассейном. */
    static Blueprint penthouse(World w, BlockPos o, Direction f) {
        Blueprint b = new Blueprint(w, o, f);
        BlockState frame = s(Blocks.BLACK_CONCRETE);
        BlockState glass = s(Blocks.LIGHT_BLUE_STAINED_GLASS);
        BlockState floor = s(Blocks.QUARTZ_BLOCK);
        b.clear(-7, 0, -2, 7, 21, 13);
        b.foundation(-7, -2, 7, 13, s(Blocks.SMOOTH_STONE));
        b.fill(-1, -1, -2, 1, -1, -1, s(Blocks.POLISHED_BLACKSTONE));
        // перекрытия
        b.fill(-5, -1, 1, 5, -1, 11, floor);
        for (int y : new int[]{4, 9, 14}) b.fill(-6, y, 0, 6, y, 12, floor);
        // фасад (3 этажа + пентхаус сзади)
        for (int lvl = 0; lvl < 3; lvl++) {
            int y0 = lvl * 5;
            b.walls(-6, y0, 0, 6, y0 + 3, 12, glass);
            b.walls(-6, y0 + 4, 0, 6, y0 + 4, 12, frame);
        }
        for (int x : new int[]{-6, -2, 2, 5, 6}) {
            b.fill(x, 0, 0, x, 14, 0, frame);
            b.fill(x, 0, 12, x, 18, 12, frame);
        }
        for (int z : new int[]{0, 4, 8, 12}) { b.fill(-6, 0, z, -6, 14, z, frame); b.fill(6, 0, z, 6, 14, z, frame); }
        // вход
        b.fill(-1, 0, 0, 1, 2, 0, s(Blocks.AIR));
        b.door(0, 0, 0, s(Blocks.SPRUCE_DOOR), Direction.NORTH);
        b.set(-1, 0, 0, glass); b.set(1, 0, 0, glass);
        b.set(-1, 1, 0, glass); b.set(1, 1, 0, glass);
        b.fill(-1, 2, 0, 1, 2, 0, frame);
        b.fill(-2, 3, -1, 2, 3, -1, topSlab(Blocks.SMOOTH_STONE_SLAB));
        b.sign(0, 3, -2, s(Blocks.SPRUCE_WALL_SIGN), Direction.NORTH, "", "§lPENTHOUSE", "residence", "");
        // «лифт» — лестница в углу
        b.fill(5, 0, 12, 5, 18, 12, frame);
        for (int y = 0; y <= 15; y++) b.set(5, y, 11, ladder(Direction.NORTH));
        b.set(4, 14, 11, floor);
        // освещение этажей
        for (int lvl = 0; lvl < 3; lvl++) {
            int yc = lvl * 5 + 4;
            for (int x : new int[]{-3, 0, 3}) for (int z : new int[]{3, 9}) if (!(lvl == 2 && z == 3)) b.set(x, yc, z, s(Blocks.SEA_LANTERN));
        }
        // лобби
        for (int x = -2; x <= 2; x++) b.set(x, 0, 7, b.stairs(s(Blocks.QUARTZ_STAIRS), Direction.NORTH));
        b.set(-5, 0, 1, s(Blocks.POTTED_BAMBOO)); b.set(5, 0, 1, s(Blocks.POTTED_BAMBOO));
        b.set(-5, 0, 11, s(Blocks.POTTED_FERN));
        for (int x = -4; x <= -2; x++) b.set(x, 0, 3, b.stairs(s(Blocks.BLACKSTONE_STAIRS), Direction.NORTH));
        // 2 этаж — апартаменты
        b.bed(-5, 5, 9, s(Blocks.GRAY_BED), Direction.SOUTH);
        b.bed(-4, 5, 9, s(Blocks.GRAY_BED), Direction.SOUTH);
        for (int x = -5; x <= -1; x++) for (int z = 5; z <= 8; z++) b.set(x, 5, z, s(Blocks.GRAY_CARPET));
        b.set(5, 5, 1, furnace(Blocks.SMOKER, Direction.WEST));
        b.set(5, 5, 2, s(Blocks.CRAFTING_TABLE));
        b.set(5, 5, 3, s(Blocks.BARREL));
        b.set(-5, 5, 1, chest(Direction.EAST));
        b.set(-5, 5, 2, chest(Direction.EAST));
        for (int x = 0; x <= 2; x++) b.set(x, 5, 5, b.stairs(s(Blocks.QUARTZ_STAIRS), Direction.SOUTH));
        // 3 этаж — кабинет и спортзал
        for (int z = 6; z <= 10; z++) for (int y = 10; y <= 12; y++) b.set(-5, y, z, s(Blocks.BOOKSHELF));
        b.set(-3, 10, 8, s(Blocks.ENCHANTING_TABLE));
        b.set(3, 10, 8, s(Blocks.ANVIL));
        b.set(3, 10, 10, s(Blocks.SMITHING_TABLE));
        b.set(2, 10, 10, s(Blocks.BREWING_STAND));
        // ===== ПЕНТХАУС (y 15..18, задняя половина) =====
        b.walls(-6, 15, 6, 6, 18, 12, glass);
        for (int x : new int[]{-6, -2, 2, 6}) b.fill(x, 15, 6, x, 18, 6, frame);
        b.fill(-6, 15, 12, -6, 18, 12, frame);
        b.fill(6, 15, 12, 6, 18, 12, frame);
        b.fill(-6, 15, 9, -6, 18, 9, frame);
        b.fill(6, 15, 9, 6, 18, 9, frame);
        b.fill(-6, 19, 6, 6, 19, 12, frame);
        for (int x : new int[]{-3, 0, 3}) b.set(x, 19, 9, s(Blocks.SEA_LANTERN));
        b.fill(-1, 15, 6, 1, 16, 6, s(Blocks.AIR));
        // интерьер пентхауса
        b.bed(-4, 15, 10, s(Blocks.WHITE_BED), Direction.SOUTH);
        b.bed(-3, 15, 10, s(Blocks.WHITE_BED), Direction.SOUTH);
        for (int x = -5; x <= -2; x++) for (int z = 7; z <= 9; z++) b.set(x, 15, z, s(Blocks.BLACK_CARPET));
        for (int x = 1; x <= 3; x++) b.set(x, 15, 9, b.stairs(s(Blocks.QUARTZ_STAIRS), Direction.SOUTH));
        b.set(1, 15, 8, s(Blocks.WHITE_CARPET));
        b.set(3, 15, 11, s(Blocks.JUKEBOX));
        b.set(2, 15, 11, s(Blocks.BARREL));
        b.set(1, 15, 11, s(Blocks.BARREL));
        b.set(-5, 15, 11, chest(Direction.EAST));
        b.set(5, 15, 7, s(Blocks.POTTED_BAMBOO));
        // ===== ТЕРРАСА С БАССЕЙНОМ (y 14, передняя половина) =====
        b.fill(-6, 15, 0, -6, 15, 5, glass);
        b.fill(6, 15, 0, 6, 15, 5, glass);
        b.fill(-6, 15, 0, 6, 15, 0, glass);
        // чаша бассейна: дно на y=13, вода y=14..15, бортик y=15
        b.fill(-4, 13, 1, 4, 13, 4, s(Blocks.PRISMARINE_BRICKS));
        b.fill(-3, 13, 2, 3, 13, 3, s(Blocks.SEA_LANTERN));
        b.fill(-4, 14, 1, 4, 14, 4, s(Blocks.PRISMARINE_BRICKS));
        b.walls(-4, 15, 0, 4, 15, 5, s(Blocks.WHITE_STAINED_GLASS));
        b.fill(-3, 14, 1, 3, 15, 4, s(Blocks.WATER));
        b.set(0, 15, 5, b.stairs(s(Blocks.QUARTZ_STAIRS), Direction.SOUTH));
        // шезлонги
        for (int z = 1; z <= 3; z += 2) {
            b.set(-5, 15, z, b.stairs(s(Blocks.QUARTZ_STAIRS), Direction.SOUTH));
            b.set(5, 15, z, b.stairs(s(Blocks.QUARTZ_STAIRS), Direction.SOUTH));
        }
        b.set(-5, 15, 5, s(Blocks.POTTED_OAK_SAPLING));
        b.set(5, 15, 5, s(Blocks.POTTED_OAK_SAPLING));
        // лобби: банкомат и автомат
        b.set(5, 0, 5, counter(ModBlocks.ATM.get(), Direction.WEST));
        b.set(5, 0, 6, counter(ModBlocks.VENDING_FOOD.get(), Direction.WEST));
        // 2 этаж: холодильник, домашний кинотеатр
        b.set(5, 5, 4, counter(ModBlocks.FRIDGE.get(), Direction.WEST));
        b.set(5, 6, 4, counter(ModBlocks.FRIDGE.get(), Direction.WEST));
        for (int x = 0; x <= 3; x++) for (int y = 6; y <= 7; y++) b.set(x, y, 11, s(Blocks.BLACK_CONCRETE));
        for (int x = 0; x <= 3; x++) b.set(x, 5, 8, b.stairs(s(Blocks.DARK_OAK_STAIRS), Direction.NORTH));
        b.set(-5, 5, 3, chest(Direction.EAST));
        b.set(-1, 5, 11, s(Blocks.POTTED_BAMBOO));
        // 3 этаж: бильярд, гардероб, растения
        for (int x = -1; x <= 1; x++) for (int z = 2; z <= 3; z++) {
            b.set(x, 10, z, s(Blocks.DARK_OAK_FENCE));
            b.set(x, 11, z, s(Blocks.GREEN_CARPET));
        }
        b.set(-5, 10, 1, s(Blocks.BARREL)); b.set(-5, 11, 1, s(Blocks.BARREL));
        b.set(-5, 10, 2, s(Blocks.BARREL)); b.set(-5, 11, 2, s(Blocks.BARREL));
        b.set(5, 10, 1, s(Blocks.POTTED_BAMBOO));
        b.set(-5, 10, 11, chest(Direction.EAST));
        // пентхаус: холодильник, ванна
        b.set(4, 15, 8, counter(ModBlocks.FRIDGE.get(), Direction.WEST));
        b.set(4, 16, 8, counter(ModBlocks.FRIDGE.get(), Direction.WEST));
        b.set(5, 15, 10, s(Blocks.CAULDRON).with(CauldronBlock.LEVEL, 3));
        return b;
    }

    // =====================================================================================
    //  ПРОДАВЦЫ, ЗЕМЛЯ, НАПОЛНЕНИЕ ДОМОВ
    // =====================================================================================

    public static final UUID CITY = new UUID(0L, 0L);

    static void spawnSeller(World w, BlockPos pos, int variant) {
        if (!(w instanceof ServerWorld)) return;
        for (SellerEntity old : w.getEntitiesWithinAABB(SellerEntity.class, new AxisAlignedBB(pos).grow(1.5D))) old.remove();
        SellerEntity e = ModEntities.SELLER.get().create(w);
        if (e == null) return;
        e.setVariant(variant);
        e.setLocationAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
        w.addEntity(e);
    }

    /** Земля под банком / МФО — городская, строить на ней нельзя. */
    static void claimCity(World w, Blueprint b) {
        if (!(w instanceof ServerWorld) || w.getServer() == null || w.getDimensionKey() != World.OVERWORLD) return;
        EconomyData data = EconomyData.get(w.getServer());
        claimArea(data, CITY, b.min().add(-2, 0, -2), b.max().add(2, 0, 2), false);
    }

    /** Закрепить за владельцем все ничейные чанки в прямоугольнике. */
    public static int claimArea(EconomyData data, UUID owner, BlockPos a, BlockPos b, boolean onlyFree) {
        int n = 0;
        for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++)
            for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) {
                long key = net.minecraft.util.math.ChunkPos.asLong(cx, cz);
                UUID cur = data.owner(key);
                if (cur == null || (!onlyFree && !cur.equals(owner) && owner.equals(CITY))) {
                    data.claim(owner, key);
                    n++;
                }
            }
        return n;
    }

    private static ItemStack it(Item i, int n) {
        return new ItemStack(i, n);
    }

    private static final Item[] FOOD = {Items.BREAD, Items.APPLE, Items.COOKED_BEEF, Items.CARROT, Items.BAKED_POTATO,
            Items.COOKIE, Items.MELON_SLICE, Items.PUMPKIN_PIE, Items.COOKED_CHICKEN, Items.COOKED_COD, Items.SWEET_BERRIES};
    private static final Item[] HOUSEHOLD = {Items.TORCH, Items.OAK_PLANKS, Items.STRING, Items.PAPER, Items.BOOK,
            Items.WHEAT_SEEDS, Items.BONE_MEAL, Items.STICK, Items.COBBLESTONE, Items.GLASS_PANE, Items.WHITE_WOOL, Items.LADDER};
    private static final Item[] RICH = {Items.IRON_INGOT, Items.GOLD_NUGGET, Items.COAL, Items.REDSTONE, Items.LAPIS_LAZULI,
            Items.LEATHER, Items.FEATHER, Items.EXPERIENCE_BOTTLE, Items.NAME_TAG, Items.EMERALD};

    private static void put(IInventory inv, Random r, ItemStack st) {
        for (int tries = 0; tries < 10; tries++) {
            int slot = r.nextInt(inv.getSizeInventory());
            if (inv.getStackInSlot(slot).isEmpty()) {
                inv.setInventorySlotContents(slot, st);
                return;
            }
        }
    }

    /** Заполнить все пустые сундуки, бочки и холодильники дома. tier 0..5 — чем дороже дом, тем богаче. */
    public static void furnish(Blueprint b, int tier, Random r) {
        World w = b.world();
        BlockPos min = b.min(), max = b.max();
        for (BlockPos pos : BlockPos.getAllInBoxMutable(min, max)) {
            TileEntity te = w.getTileEntity(pos);
            if (!(te instanceof IInventory)) continue;
            IInventory inv = (IInventory) te;
            if (!inv.isEmpty()) continue;
            if (te instanceof FridgeTileEntity) {
                int n = 4 + r.nextInt(3) + tier;
                for (int i = 0; i < n; i++) put(inv, r, it(FOOD[r.nextInt(FOOD.length)], 2 + r.nextInt(6)));
                put(inv, r, it(Items.MILK_BUCKET, 1));
                if (tier >= 3) put(inv, r, it(Items.CAKE, 1));
                if (tier >= 4) put(inv, r, it(Items.GOLDEN_CARROT, 2 + r.nextInt(4)));
            } else {
                int n = 3 + r.nextInt(3) + tier / 2;
                for (int i = 0; i < n; i++) put(inv, r, it(HOUSEHOLD[r.nextInt(HOUSEHOLD.length)], 1 + r.nextInt(12)));
                put(inv, r, it(FOOD[r.nextInt(FOOD.length)], 2 + r.nextInt(4)));
                for (int i = 0; i < tier; i++) {
                    if (r.nextInt(3) == 0) put(inv, r, it(RICH[r.nextInt(RICH.length)], 1 + r.nextInt(3)));
                }
                if (tier >= 4 && r.nextInt(3) == 0) put(inv, r, it(Items.DIAMOND, 1));
            }
            te.markDirty();
        }
    }

    // =====================================================================================
    //  СИЗО (в небе) и ПОДВАЛ (под МФО) — временные, со снимком исходных блоков
    // =====================================================================================

    /** Габарит камеры относительно центра: x/z −4..4, y 0..5. */
    public static boolean cellFits(World w, BlockPos c, int margin) {
        for (int x = -4 - margin; x <= 4 + margin; x++)
            for (int z = -4 - margin; z <= 4 + margin; z++)
                for (int y = -margin; y <= 5 + margin; y++) {
                    BlockPos p = c.add(x, y, z);
                    if (!w.isAirBlock(p)) return false;
                }
        return true;
    }

    public static Blueprint buildJail(World w, BlockPos center) {
        Blueprint b = new Blueprint(w, center, Direction.SOUTH).recordSnapshot();
        BlockState wall = s(ModBlocks.JAIL_WALL.get());
        b.fill(-4, 0, -4, 4, 5, 4, wall);
        b.clear(-3, 1, -3, 3, 4, 3);
        b.set(0, 5, 0, s(Blocks.GLOWSTONE));
        b.set(-2, 5, 2, s(Blocks.GLOWSTONE));
        b.set(2, 5, -2, s(Blocks.GLOWSTONE));
        // окна в небо
        b.set(-4, 2, 0, s(Blocks.IRON_BARS));
        b.set(4, 2, 0, s(Blocks.IRON_BARS));
        for (int z = 1; z <= 3; z++) b.set(-3, 1, z, topSlab(Blocks.SPRUCE_SLAB));
        b.set(3, 1, 3, s(Blocks.CAULDRON));
        b.sign(0, 2, -3, s(Blocks.SPRUCE_WALL_SIGN), Direction.SOUTH, "§lСИЗО №1", "Для должников", "«Реал Банка»", "§8ждите срок");
        return b;
    }

    public static Blueprint buildPit(World w, BlockPos center) {
        Blueprint b = new Blueprint(w, center, Direction.SOUTH).recordSnapshot();
        BlockState wall = s(ModBlocks.JAIL_WALL.get());
        b.fill(-4, 0, -4, 4, 5, 4, wall);
        b.clear(-3, 1, -3, 3, 4, 3);
        b.set(0, 5, 2, s(Blocks.GLOWSTONE));
        b.set(0, 5, -2, s(Blocks.GLOWSTONE));
        Random r = new Random();
        for (int x = -3; x <= 3; x++)
            for (int y = 1; y <= 4; y++)
                for (int z = -3; z <= 0; z++)
                    b.set(x, y, z, pitBlock(r));
        b.sign(-3, 2, 3, s(Blocks.OAK_WALL_SIGN), Direction.EAST, "§4ПОДВАЛ", "Шайкина", "Копай, пока", "не отработаешь");
        b.set(3, 1, 3, s(Blocks.CAULDRON));
        b.set(2, 1, 3, s(Blocks.CHAIN));
        return b;
    }

    // =====================================================================================
    //  ПОДПОЛЬНЫЙ ЦЕХ (подвал под домом)
    // =====================================================================================

    public static class Basement {
        public final Blueprint bp;
        public final BlockPos chest;
        public final BlockPos hatch;

        Basement(Blueprint bp, BlockPos chest, BlockPos hatch) {
            this.bp = bp;
            this.chest = chest;
            this.hatch = hatch;
        }
    }

    /**
     * Построить подвал под домом. houseMin.y — уровень фундамента (пол дома = houseMin.y + 1).
     * Люк ставится в свободное место пола не дальше 3 блоков от центра дома.
     */
    public static Basement buildBasement(World w, BlockPos houseMin, BlockPos houseMax) {
        int floorY = houseMin.getY() + 1;
        int cx = (houseMin.getX() + houseMax.getX()) / 2;
        int cz = (houseMin.getZ() + houseMax.getZ()) / 2;
        BlockPos hatch = null;
        double best = Double.MAX_VALUE;
        for (int dx = -3; dx <= 2; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos p = new BlockPos(cx + dx, floorY, cz + dz);
                if (!w.isAirBlock(p) || !w.isAirBlock(p.up())) continue;
                if (!w.getBlockState(p.down()).isSolid()) continue;
                if (!w.isAirBlock(p.east()) && !w.getBlockState(p.east()).isSolid()) continue;
                boolean roofed = false;
                for (int y = 2; y <= 8; y++) if (!w.isAirBlock(p.up(y))) { roofed = true; break; }
                if (!roofed) continue;
                double d = dx * dx + dz * dz + (dx == 0 && dz == 0 ? 0.5 : 0);
                if (d < best) { best = d; hatch = p; }
            }
        if (hatch == null) return null;
        int by = floorY - 7; // пол подвала
        Blueprint b = new Blueprint(w, new BlockPos(cx, by, cz), Direction.SOUTH);
        BlockState brick = s(Blocks.STONE_BRICKS);
        // комната 7x4x7 (внутри), стены из камня
        b.fill(-4, 0, -4, 4, 5, 4, brick);
        b.clear(-3, 1, -3, 3, 4, 3);
        b.fill(-3, 0, -3, 3, 0, 3, s(Blocks.POLISHED_ANDESITE));
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) b.set(c[0], 5, c[1], s(Blocks.SEA_LANTERN));
        // шахта с лестницей к люку (локальные координаты = мировые смещения)
        int hx = hatch.getX() - cx, hz = hatch.getZ() - cz;
        for (int y = 1; y <= floorY - 2 - by; y++) {
            if (y >= 5) {
                b.set(hx - 1, y, hz, brick);
                b.set(hx, y, hz - 1, brick);
                b.set(hx, y, hz + 1, brick);
            }
            b.set(hx + 1, y, hz, brick);
            b.set(hx, y, hz, ladder(Direction.WEST));
        }
        b.set(hx, floorY - 1 - by, hz, s(Blocks.SPRUCE_TRAPDOOR).with(TrapDoorBlock.HALF, Half.TOP)
                .with(TrapDoorBlock.HORIZONTAL_FACING, Direction.WEST));
        // станки и стол
        int[][] printers = {{-3, -3}, {-2, -3}, {-1, -3}, {1, -3}, {2, -3}};
        for (int[] pr : printers) if (!(pr[0] == hx && pr[1] == hz) && !(pr[0] == hx + 1 && pr[1] == hz)) b.set(pr[0], 1, pr[1], counter(ModBlocks.MONEY_PRINTER.get(), Direction.SOUTH));
        BlockPos chest = null;
        int[][] chestSpots = {{-3, 3}, {3, 3}, {-3, 0}, {3, 0}, {0, 3}};
        for (int[] cs : chestSpots) {
            if (Math.abs(cs[0] - hx) <= 1 && Math.abs(cs[1] - hz) <= 1) continue;
            b.set(cs[0], 1, cs[1], chest(Direction.NORTH));
            chest = b.pos(cs[0], 1, cs[1]);
            break;
        }
        if (chest == null) {
            b.set(-3, 1, 1, chest(Direction.EAST));
            chest = b.pos(-3, 1, 1);
        }
        if (!(Math.abs(hx) <= 1 && Math.abs(hz) <= 1)) {
            b.set(0, 1, 0, s(Blocks.DARK_OAK_FENCE));
            b.set(0, 2, 0, s(Blocks.DARK_OAK_PRESSURE_PLATE));
        }
        if (!(Math.abs(hx - 3) <= 1 && Math.abs(hz + 1) <= 1)) {
            b.set(3, 1, -1, s(Blocks.CRAFTING_TABLE));
            b.set(3, 2, -1, s(Blocks.LANTERN));
        }
        if (!(Math.abs(hx + 3) <= 1 && Math.abs(hz - 1) <= 1) && !chest.equals(b.pos(-3, 1, 1))) {
            b.set(-3, 1, 1, s(Blocks.BARREL));
        }
        return new Basement(b, chest, hatch);
    }

    /** Засыпать подвал (при изъятии дома). */
    public static void fillBasement(World w, BlockPos min, BlockPos max) {
        long volume = (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        if (volume <= 0 || volume > 4000) return;
        for (BlockPos p : BlockPos.getAllInBoxMutable(min, max)) {
            TileEntity te = w.getTileEntity(p);
            if (te instanceof IInventory) ((IInventory) te).clear();
            w.setBlockState(p.toImmutable(), s(Blocks.STONE), Blueprint.FLAGS);
        }
    }

    public static BlockState pitBlock(Random r) {
        int n = r.nextInt(100);
        if (n < 70) return s(Blocks.STONE);
        if (n < 80) return s(Blocks.ANDESITE);
        if (n < 88) return s(Blocks.COAL_ORE);
        if (n < 95) return s(Blocks.IRON_ORE);
        if (n < 98) return s(Blocks.GOLD_ORE);
        return s(Blocks.DIAMOND_ORE);
    }

    /** Зона забоя в подвале: внутри неё блоки отрабатывают долг и восстанавливаются. */
    public static boolean inPitMine(BlockPos center, BlockPos p) {
        int dx = p.getX() - center.getX(), dy = p.getY() - center.getY(), dz = p.getZ() - center.getZ();
        return dx >= -3 && dx <= 3 && dy >= 1 && dy <= 4 && dz >= -3 && dz <= 0;
    }
}
