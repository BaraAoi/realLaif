package com.baraaoi.reallife.registry;

import com.baraaoi.reallife.item.CloverItem;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.item.BanknoteItem;
import com.baraaoi.reallife.item.BuildPermitItem;
import com.baraaoi.reallife.item.DebtorPickaxeItem;
import com.baraaoi.reallife.item.DirtyMoneyItem;
import com.baraaoi.reallife.item.HouseOrderItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RealLife.MODID);

    public static final ItemGroup TAB = new ItemGroup(RealLife.MODID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(BANKNOTE_1000.get());
        }
    };

    private static Item.Properties props() {
        return new Item.Properties().group(TAB);
    }

    public static final RegistryObject<Item> BANKNOTE_10 = ITEMS.register("banknote_10", () -> new BanknoteItem(10, props()));
    public static final RegistryObject<Item> BANKNOTE_100 = ITEMS.register("banknote_100", () -> new BanknoteItem(100, props()));
    public static final RegistryObject<Item> BANKNOTE_500 = ITEMS.register("banknote_500", () -> new BanknoteItem(500, props()));
    public static final RegistryObject<Item> BANKNOTE_1000 = ITEMS.register("banknote_1000", () -> new BanknoteItem(1000, props()));
    public static final RegistryObject<Item> BANKNOTE_5000 = ITEMS.register("banknote_5000", () -> new BanknoteItem(5000, props().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> BANK_PERMIT = ITEMS.register("bank_permit",
            () -> new BuildPermitItem(BuildPermitItem.Kind.BANK, props().maxStackSize(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> MFO_PERMIT = ITEMS.register("mfo_permit",
            () -> new BuildPermitItem(BuildPermitItem.Kind.MFO, props().maxStackSize(1)));
    public static final RegistryObject<Item> HOUSE_ORDER = ITEMS.register("house_order",
            () -> new HouseOrderItem(new Item.Properties().maxStackSize(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> DEBTOR_PICKAXE = ITEMS.register("debtor_pickaxe",
            () -> new DebtorPickaxeItem(new Item.Properties().maxStackSize(1)));

    public static final RegistryObject<Item> BANK_COUNTER = ITEMS.register("bank_counter",
            () -> new BlockItem(ModBlocks.BANK_COUNTER.get(), props()));
    public static final RegistryObject<Item> MFO_COUNTER = ITEMS.register("mfo_counter",
            () -> new BlockItem(ModBlocks.MFO_COUNTER.get(), props()));
    public static final RegistryObject<Item> JAIL_WALL = ITEMS.register("jail_wall",
            () -> new BlockItem(ModBlocks.JAIL_WALL.get(), props()));

    public static final RegistryObject<Item> DIRTY_MONEY = ITEMS.register("dirty_money",
            () -> new DirtyMoneyItem(props()));

    public static final RegistryObject<Item> FRIDGE = ITEMS.register("fridge",
            () -> new BlockItem(ModBlocks.FRIDGE.get(), props()));
    public static final RegistryObject<Item> ATM = ITEMS.register("atm",
            () -> new BlockItem(ModBlocks.ATM.get(), props()));
    public static final RegistryObject<Item> VENDING_FOOD = ITEMS.register("vending_food",
            () -> new BlockItem(ModBlocks.VENDING_FOOD.get(), props()));
    public static final RegistryObject<Item> VENDING_RESOURCES = ITEMS.register("vending_resources",
            () -> new BlockItem(ModBlocks.VENDING_RESOURCES.get(), props()));
    public static final RegistryObject<Item> MONEY_PRINTER = ITEMS.register("money_printer",
            () -> new BlockItem(ModBlocks.MONEY_PRINTER.get(), props()));

    public static final RegistryObject<Item> CASINO_PERMIT = ITEMS.register("casino_permit",
            () -> new BuildPermitItem(BuildPermitItem.Kind.CASINO, props().maxStackSize(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> CLOVER = ITEMS.register("clover",
            () -> new CloverItem(props().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> PIT_RECEIVER = ITEMS.register("pit_receiver",
            () -> new BlockItem(ModBlocks.PIT_RECEIVER.get(), props()));
    public static final RegistryObject<Item> BUSINESS_CHEST = ITEMS.register("business_chest",
            () -> new BlockItem(ModBlocks.BUSINESS_CHEST.get(), props()));
    public static final RegistryObject<Item> LAUNDER_MACHINE = ITEMS.register("launder_machine",
            () -> new BlockItem(ModBlocks.LAUNDER_MACHINE.get(), props()));
    public static final RegistryObject<Item> COIN_TABLE = ITEMS.register("coin_table",
            () -> new BlockItem(ModBlocks.COIN_TABLE.get(), props()));
    public static final RegistryObject<Item> SLOT_MACHINE = ITEMS.register("slot_machine",
            () -> new BlockItem(ModBlocks.SLOT_MACHINE.get(), props()));
    public static final RegistryObject<Item> ANARCHY_MACHINE = ITEMS.register("anarchy_machine",
            () -> new BlockItem(ModBlocks.ANARCHY_MACHINE.get(), props()));
    public static final RegistryObject<Item> ROULETTE_TABLE = ITEMS.register("roulette_table",
            () -> new BlockItem(ModBlocks.ROULETTE_TABLE.get(), props()));

    public static final RegistryObject<Item> BIZ_PERMIT = ITEMS.register("biz_permit",
            () -> new com.baraaoi.reallife.item.BizPermitItem(props().maxStackSize(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> BIZ_COUNTER = ITEMS.register("biz_counter",
            () -> new BlockItem(ModBlocks.BIZ_COUNTER.get(), props()));
    public static final RegistryObject<Item> SHOP_SHELF = ITEMS.register("shop_shelf", () -> new BlockItem(ModBlocks.SHOP_SHELF.get(), props()));
    public static final RegistryObject<Item> STORAGE_RACK = ITEMS.register("storage_rack", () -> new BlockItem(ModBlocks.STORAGE_RACK.get(), props()));
    public static final RegistryObject<Item> DISPLAY_CASE = ITEMS.register("display_case", () -> new BlockItem(ModBlocks.DISPLAY_CASE.get(), props()));
    public static final RegistryObject<Item> SAFE = ITEMS.register("safe", () -> new BlockItem(ModBlocks.SAFE.get(), props()));
    public static final RegistryObject<Item> SHAWARMA_GRILL = ITEMS.register("shawarma_grill", () -> new BlockItem(ModBlocks.SHAWARMA_GRILL.get(), props()));
    public static final RegistryObject<Item> PRESSURE_WASHER = ITEMS.register("pressure_washer", () -> new BlockItem(ModBlocks.PRESSURE_WASHER.get(), props()));
    public static final RegistryObject<Item> STONE_CRUSHER = ITEMS.register("stone_crusher", () -> new BlockItem(ModBlocks.STONE_CRUSHER.get(), props()));
    public static final RegistryObject<Item> FUEL_PUMP = ITEMS.register("fuel_pump", () -> new BlockItem(ModBlocks.FUEL_PUMP.get(), props()));
    /** Сырьё для заведений: по одному виду на тип. */
    public static final java.util.Map<com.baraaoi.reallife.economy.BizType, RegistryObject<Item>> SUPPLY = new java.util.EnumMap<>(com.baraaoi.reallife.economy.BizType.class);

    static {
        for (com.baraaoi.reallife.economy.BizType t : com.baraaoi.reallife.economy.BizType.values()) {
            SUPPLY.put(t, ITEMS.register("supply_" + t.id(), () -> new com.baraaoi.reallife.item.SupplyItem(t, props().maxStackSize(1))));
        }
    }
    public static final RegistryObject<Item> CRYPTO_RIG = ITEMS.register("crypto_rig",
            () -> new BlockItem(ModBlocks.CRYPTO_RIG.get(), props()));
    public static final RegistryObject<Item> MOTHERBOARD = ITEMS.register("motherboard",
            () -> new com.baraaoi.reallife.item.ComponentItem(com.baraaoi.reallife.item.ComponentItem.MOTHERBOARD, props().maxStackSize(16)));
    public static final RegistryObject<Item> CPU = ITEMS.register("cpu",
            () -> new com.baraaoi.reallife.item.ComponentItem(com.baraaoi.reallife.item.ComponentItem.CPU, props().maxStackSize(16)));
    public static final RegistryObject<Item> RAM = ITEMS.register("ram",
            () -> new com.baraaoi.reallife.item.ComponentItem(com.baraaoi.reallife.item.ComponentItem.RAM, props().maxStackSize(16)));
    public static final RegistryObject<Item> PSU = ITEMS.register("psu",
            () -> new com.baraaoi.reallife.item.ComponentItem(com.baraaoi.reallife.item.ComponentItem.PSU, props().maxStackSize(16)));
    public static final RegistryObject<Item> COOLER = ITEMS.register("cooler",
            () -> new com.baraaoi.reallife.item.ComponentItem(com.baraaoi.reallife.item.ComponentItem.COOLER, props().maxStackSize(16)));
    public static final RegistryObject<Item> GPU = ITEMS.register("gpu",
            () -> new com.baraaoi.reallife.item.ComponentItem(com.baraaoi.reallife.item.ComponentItem.GPU, props().maxStackSize(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> PHONE = ITEMS.register("phone",
            () -> new com.baraaoi.reallife.item.PhoneItem(props().maxStackSize(1)));
}
