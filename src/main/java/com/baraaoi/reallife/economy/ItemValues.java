package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.item.BanknoteItem;
import com.baraaoi.reallife.item.DirtyMoneyItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Сколько рублей банк даёт за ресурсы.
 */
public class ItemValues {
    private static final Map<Item, Double> VALUES = new HashMap<>();
    private static final Map<Block, Double> ORE_VALUES = new HashMap<>();

    static {
        put(Items.COAL, 2); put(Items.CHARCOAL, 1); put(Items.COAL_BLOCK, 18);
        put(Items.IRON_NUGGET, 1); put(Items.IRON_INGOT, 10); put(Items.IRON_BLOCK, 90); put(Items.IRON_ORE, 8);
        put(Items.GOLD_NUGGET, 3); put(Items.GOLD_INGOT, 25); put(Items.GOLD_BLOCK, 225); put(Items.GOLD_ORE, 20);
        put(Items.REDSTONE, 3); put(Items.REDSTONE_BLOCK, 27);
        put(Items.LAPIS_LAZULI, 5); put(Items.LAPIS_BLOCK, 45);
        put(Items.QUARTZ, 4); put(Items.QUARTZ_BLOCK, 16);
        put(Items.DIAMOND, 150); put(Items.DIAMOND_BLOCK, 1350);
        put(Items.EMERALD, 60); put(Items.EMERALD_BLOCK, 540);
        put(Items.NETHERITE_SCRAP, 250); put(Items.NETHERITE_INGOT, 1200); put(Blocks.NETHERITE_BLOCK.asItem(), 10800);
        put(Items.ANCIENT_DEBRIS, 250);
        // фермерство / прочее
        put(Items.WHEAT, 1); put(Items.BREAD, 3); put(Items.CARROT, 1); put(Items.POTATO, 1); put(Items.BAKED_POTATO, 2);
        put(Items.BEETROOT, 1); put(Items.SUGAR_CANE, 1); put(Items.PUMPKIN, 2); put(Items.MELON, 3);
        put(Items.COOKED_BEEF, 4); put(Items.COOKED_PORKCHOP, 4); put(Items.COOKED_CHICKEN, 3); put(Items.COOKED_MUTTON, 3);
        put(Items.LEATHER, 3); put(Items.WHITE_WOOL, 2); put(Items.EGG, 1); put(Items.HONEY_BOTTLE, 4);
        put(Items.OAK_LOG, 2); put(Items.SPRUCE_LOG, 2); put(Items.BIRCH_LOG, 2); put(Items.JUNGLE_LOG, 2);
        put(Items.ACACIA_LOG, 2); put(Items.DARK_OAK_LOG, 2);
        put(Items.STRING, 1); put(Items.BONE, 1); put(Items.GUNPOWDER, 3); put(Items.ENDER_PEARL, 15);
        put(Items.BLAZE_ROD, 15); put(Items.GHAST_TEAR, 30); put(Items.SLIME_BALL, 4); put(Items.SPIDER_EYE, 2);
        put(Items.ROTTEN_FLESH, 0.5); put(Items.OBSIDIAN, 5); put(Items.GLOWSTONE_DUST, 3);

        // руда для отработки долга (Александр / подвал)
        ore(Blocks.COAL_ORE, 3); ore(Blocks.IRON_ORE, 12); ore(Blocks.GOLD_ORE, 30); ore(Blocks.NETHER_GOLD_ORE, 10);
        ore(Blocks.REDSTONE_ORE, 5); ore(Blocks.LAPIS_ORE, 8); ore(Blocks.DIAMOND_ORE, 180); ore(Blocks.EMERALD_ORE, 70);
        ore(Blocks.NETHER_QUARTZ_ORE, 5); ore(Blocks.ANCIENT_DEBRIS, 300);
    }

    /** v4: все предметы подорожали вдвое (купюры и грязные деньги — по номиналу). */
    public static final double VALUE_MULT = 2.0;

    private static void put(Item item, double v) { if (item != null && v > 0) VALUES.put(item, v * VALUE_MULT); }
    private static void ore(Block b, double v) { ORE_VALUES.put(b, v); }

    /** Стоимость одной единицы предмета в рублях. */
    public static double valueOf(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() instanceof BanknoteItem) return ((BanknoteItem) stack.getItem()).getValue();
        if (stack.getItem() instanceof DirtyMoneyItem) return DirtyMoneyItem.VALUE;
        Double v = VALUES.get(stack.getItem());
        return v == null ? 0 : v;
    }

    /** Наличные (купюры или грязные деньги) — МФО и братки берут по номиналу. */
    public static boolean isCash(ItemStack stack) {
        return stack.getItem() instanceof BanknoteItem || stack.getItem() instanceof DirtyMoneyItem;
    }

    public static boolean isDirty(ItemStack stack) {
        return stack.getItem() instanceof DirtyMoneyItem;
    }

    public static double stackValue(ItemStack stack) {
        return valueOf(stack) * stack.getCount();
    }

    /** Стоимость блока при «отработке» (копании) долга. */
    public static double laborValue(BlockState state) {
        Double v = ORE_VALUES.get(state.getBlock());
        return v == null ? 0 : v;
    }
}
