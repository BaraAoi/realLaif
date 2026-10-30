package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MerchantOffer;
import net.minecraft.item.MerchantOffers;

/**
 * Ассортимент продавцов в банке и торговых автоматов.
 * Все цены выше, чем банк платит за те же предметы — перепродавать невыгодно.
 */
public class Trades {
    public static final int SELLER_FOOD = 0;
    public static final int SELLER_GOODS = 1;
    public static final int SELLER_CASINO = 2;
    public static final int VENDING_FOOD = 0;
    public static final int VENDING_RESOURCES = 1;
    private static final int UNLIMITED = 100000;

    /** v4: цены в магазинах тоже выросли вдвое — вслед за стоимостью предметов. */
    public static final int PRICE_MULT = 2;

    private static ItemStack rub(int amount) {
        amount *= PRICE_MULT;
        if (amount >= 1000 && amount % 1000 == 0) return new ItemStack(ModItems.BANKNOTE_1000.get(), amount / 1000);
        if (amount >= 100 && amount % 100 == 0) return new ItemStack(ModItems.BANKNOTE_100.get(), amount / 100);
        return new ItemStack(ModItems.BANKNOTE_10.get(), Math.max(1, amount / 10));
    }

    private static void sell(MerchantOffers o, int price, Item item, int count) {
        o.add(new MerchantOffer(rub(price), new ItemStack(item, count), UNLIMITED, 0, 0.0F));
    }

    private static void swap(MerchantOffers o, Item in, int inCount, Item out, int outCount) {
        o.add(new MerchantOffer(new ItemStack(in, inCount), new ItemStack(out, outCount), UNLIMITED, 0, 0.0F));
    }

    public static MerchantOffers seller(int variant) {
        MerchantOffers o = new MerchantOffers();
        if (variant == SELLER_CASINO) {
            net.minecraft.potion.Potion[] drinks = {net.minecraft.potion.Potions.SWIFTNESS, net.minecraft.potion.Potions.STRENGTH,
                    net.minecraft.potion.Potions.LEAPING, net.minecraft.potion.Potions.NIGHT_VISION, net.minecraft.potion.Potions.FIRE_RESISTANCE,
                    net.minecraft.potion.Potions.REGENERATION, net.minecraft.potion.Potions.WATER_BREATHING, net.minecraft.potion.Potions.LUCK};
            for (net.minecraft.potion.Potion pt : drinks) {
                o.add(new MerchantOffer(rub(100), net.minecraft.potion.PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), pt), UNLIMITED, 0, 0.0F));
            }
            sell(o, 10, Items.HONEY_BOTTLE, 2);
            sell(o, 10, Items.MILK_BUCKET, 1);
            o.add(new MerchantOffer(rub(10000), new ItemStack(ModItems.CLOVER.get()), UNLIMITED, 0, 0.0F));
            return o;
        }
        if (variant == SELLER_FOOD) {
            sell(o, 10, Items.BREAD, 3);
            sell(o, 10, Items.BAKED_POTATO, 4);
            sell(o, 10, Items.COOKED_BEEF, 2);
            sell(o, 10, Items.COOKED_PORKCHOP, 2);
            sell(o, 10, Items.COOKED_CHICKEN, 3);
            sell(o, 10, Items.APPLE, 5);
            sell(o, 10, Items.PUMPKIN_PIE, 2);
            sell(o, 20, Items.CAKE, 1);
            sell(o, 10, Items.HONEY_BOTTLE, 2);
            sell(o, 100, Items.GOLDEN_CARROT, 4);
            sell(o, 100, Items.GOLDEN_APPLE, 1);
            sell(o, 10, Items.WHEAT_SEEDS, 8);
        } else {
            sell(o, 10, Items.TORCH, 16);
            sell(o, 10, Items.OAK_LOG, 4);
            sell(o, 10, Items.COBBLESTONE, 32);
            sell(o, 10, Items.COAL, 4);
            sell(o, 100, Items.IRON_INGOT, 8);
            sell(o, 100, Items.IRON_PICKAXE, 1);
            sell(o, 100, Items.IRON_AXE, 1);
            sell(o, 100, Items.IRON_SHOVEL, 1);
            sell(o, 100, Items.IRON_SWORD, 1);
            sell(o, 100, Items.BUCKET, 1);
            sell(o, 100, Items.SHEARS, 1);
            sell(o, 1000, Items.DIAMOND_PICKAXE, 1);
            sell(o, 1000, Items.DIAMOND, 5);
            sell(o, 500, ModItems.PHONE.get(), 1);
        }
        return o;
    }

    public static MerchantOffers vending(int kind) {
        MerchantOffers o = new MerchantOffers();
        if (kind == VENDING_FOOD) {
            sell(o, 10, Items.COOKIE, 8);
            sell(o, 10, Items.BREAD, 3);
            sell(o, 10, Items.MELON_SLICE, 8);
            sell(o, 10, Items.SWEET_BERRIES, 8);
            sell(o, 10, Items.MUSHROOM_STEW, 1);
            sell(o, 10, Items.BEETROOT_SOUP, 1);
            sell(o, 10, Items.MILK_BUCKET, 1);
            sell(o, 10, Items.DRIED_KELP, 16);
            // обмен
            swap(o, Items.WHEAT, 10, Items.BREAD, 3);
            swap(o, Items.POTATO, 8, Items.BAKED_POTATO, 3);
        } else {
            sell(o, 10, Items.GLASS, 16);
            sell(o, 10, Items.WHITE_WOOL, 4);
            sell(o, 10, Items.SAND, 32);
            sell(o, 10, Items.BRICKS, 8);
            sell(o, 10, Items.LADDER, 8);
            sell(o, 10, Items.LANTERN, 2);
            sell(o, 10, Items.WHITE_BED, 1);
            sell(o, 10, Items.CHEST, 2);
            // обмен ресурсов
            swap(o, Items.ROTTEN_FLESH, 16, Items.LEATHER, 1);
            swap(o, Items.GRAVEL, 4, Items.FLINT, 1);
            swap(o, Items.COBBLESTONE, 32, Items.STONE_BRICKS, 16);
            swap(o, Items.BONE, 3, Items.BONE_MEAL, 12);
            swap(o, Items.STRING, 4, Items.WHITE_WOOL, 1);
        }
        return o;
    }
}
