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
}
