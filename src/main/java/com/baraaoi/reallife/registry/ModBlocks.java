package com.baraaoi.reallife.registry;

import com.baraaoi.reallife.block.PitReceiverBlock;
import com.baraaoi.reallife.block.LaunderMachineBlock;
import com.baraaoi.reallife.block.CasinoBlock;
import com.baraaoi.reallife.economy.CasinoService;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.block.BankBlock;
import com.baraaoi.reallife.block.DecorBlock;
import com.baraaoi.reallife.block.FinanceBlock;
import com.baraaoi.reallife.block.FridgeBlock;
import com.baraaoi.reallife.block.MfoBlock;
import com.baraaoi.reallife.block.VendingBlock;
import com.baraaoi.reallife.economy.Trades;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RealLife.MODID);

    public static final RegistryObject<Block> BANK_COUNTER = BLOCKS.register("bank_counter",
            () -> new BankBlock(AbstractBlock.Properties.create(Material.ROCK).hardnessAndResistance(3.0F, 1200.0F).sound(SoundType.STONE)));

    public static final RegistryObject<Block> MFO_COUNTER = BLOCKS.register("mfo_counter",
            () -> new MfoBlock(AbstractBlock.Properties.create(Material.WOOD).hardnessAndResistance(2.0F, 6.0F).sound(SoundType.WOOD)));

    /** Неразрушимая стена тюрьмы / подвала. */
    public static final RegistryObject<Block> JAIL_WALL = BLOCKS.register("jail_wall",
            () -> new Block(AbstractBlock.Properties.create(Material.ROCK).hardnessAndResistance(-1.0F, 3600000.0F).noDrops().sound(SoundType.METAL)));

    public static final RegistryObject<Block> FRIDGE = BLOCKS.register("fridge",
            () -> new FridgeBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(2.0F, 6.0F).sound(SoundType.METAL)));

    public static final RegistryObject<Block> ATM = BLOCKS.register("atm",
            () -> new FinanceBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(3.0F, 1200.0F).sound(SoundType.METAL), false, "Банкомат «Реал Банк»"));

    public static final RegistryObject<Block> VENDING_FOOD = BLOCKS.register("vending_food",
            () -> new VendingBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(3.0F, 1200.0F).sound(SoundType.METAL).setLightLevel(s -> 7), Trades.VENDING_FOOD));

    public static final RegistryObject<Block> VENDING_RESOURCES = BLOCKS.register("vending_resources",
            () -> new VendingBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(3.0F, 1200.0F).sound(SoundType.METAL).setLightLevel(s -> 7), Trades.VENDING_RESOURCES));

    public static final RegistryObject<Block> MONEY_PRINTER = BLOCKS.register("money_printer",
            () -> new com.baraaoi.reallife.block.MoneyPrinterBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(3.0F, 6.0F).sound(SoundType.METAL)));

    /** «Сундук цеха»: сюда все станки игрока складывают грязные деньги. Выдаётся один раз. */
    public static final RegistryObject<Block> BUSINESS_CHEST = BLOCKS.register("business_chest",
            () -> new FridgeBlock(AbstractBlock.Properties.create(Material.WOOD).hardnessAndResistance(2.5F, 6.0F).sound(SoundType.WOOD)));

    /** Приёмник добычи в подвале Шайкина (неразрушимый). */
    public static final RegistryObject<Block> PIT_RECEIVER = BLOCKS.register("pit_receiver",
            () -> new PitReceiverBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(-1.0F, 3600000.0F).noDrops().sound(SoundType.METAL)));

    public static final RegistryObject<Block> LAUNDER_MACHINE = BLOCKS.register("launder_machine",
            () -> new LaunderMachineBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(3.0F, 6.0F).sound(SoundType.METAL)));

    public static final RegistryObject<Block> COIN_TABLE = BLOCKS.register("coin_table",
            () -> new CasinoBlock(casino(), CasinoService.COIN));
    public static final RegistryObject<Block> SLOT_MACHINE = BLOCKS.register("slot_machine",
            () -> new CasinoBlock(casino().setLightLevel(s -> 9), CasinoService.SLOTS));
    public static final RegistryObject<Block> ANARCHY_MACHINE = BLOCKS.register("anarchy_machine",
            () -> new CasinoBlock(casino().setLightLevel(s -> 9), CasinoService.ANARCHY));
    public static final RegistryObject<Block> ROULETTE_TABLE = BLOCKS.register("roulette_table",
            () -> new CasinoBlock(casino(), CasinoService.ROULETTE));

    /** Касса легального заведения: главная ставится лицензией, дополнительные покупаются. */
    public static final RegistryObject<Block> BIZ_COUNTER = BLOCKS.register("biz_counter",
            () -> new com.baraaoi.reallife.block.BizCounterBlock(AbstractBlock.Properties.create(Material.WOOD).hardnessAndResistance(2.5F, 6.0F).sound(SoundType.WOOD)));

    // ---- оборудование заведений (v6)
    public static final RegistryObject<Block> SHOP_SHELF = BLOCKS.register("shop_shelf",
            () -> new com.baraaoi.reallife.block.ShelfBlock(AbstractBlock.Properties.create(Material.WOOD).hardnessAndResistance(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final RegistryObject<Block> STORAGE_RACK = BLOCKS.register("storage_rack",
            () -> new com.baraaoi.reallife.block.ShelfBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(2.5F, 6.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> DISPLAY_CASE = BLOCKS.register("display_case",
            () -> new com.baraaoi.reallife.block.ShelfBlock(AbstractBlock.Properties.create(Material.GLASS).hardnessAndResistance(1.5F, 3.0F).sound(SoundType.GLASS)));
    public static final RegistryObject<Block> SAFE = BLOCKS.register("safe",
            () -> new com.baraaoi.reallife.block.ShelfBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(6.0F, 1200.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> SHAWARMA_GRILL = BLOCKS.register("shawarma_grill",
            () -> new DecorBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(2.5F, 6.0F).sound(SoundType.METAL).setLightLevel(s -> 8)));
    public static final RegistryObject<Block> PRESSURE_WASHER = BLOCKS.register("pressure_washer",
            () -> new DecorBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(2.5F, 6.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> STONE_CRUSHER = BLOCKS.register("stone_crusher",
            () -> new DecorBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(4.0F, 6.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> FUEL_PUMP = BLOCKS.register("fuel_pump",
            () -> new DecorBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(3.0F, 6.0F).sound(SoundType.METAL).setLightLevel(s -> 5)));

    /** Майнинг-ферма: корпус под комплектующие. */
    public static final RegistryObject<Block> CRYPTO_RIG = BLOCKS.register("crypto_rig",
            () -> new com.baraaoi.reallife.block.CryptoRigBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(3.0F, 6.0F).sound(SoundType.METAL).setLightLevel(s -> 4)));

    private static AbstractBlock.Properties casino() {
        return AbstractBlock.Properties.create(Material.WOOD).hardnessAndResistance(2.5F, 1200.0F).sound(SoundType.WOOD);
    }
}
