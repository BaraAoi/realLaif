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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RealLife.MODID);

    public static final RegistryObject<Block> BANK_COUNTER = BLOCKS.register("bank_counter",
            () -> new BankBlock(props("bank_counter").strength(3.0F, 1200.0F).sound(SoundType.STONE)));

    public static final RegistryObject<Block> MFO_COUNTER = BLOCKS.register("mfo_counter",
            () -> new MfoBlock(props("mfo_counter").strength(2.0F, 6.0F).sound(SoundType.WOOD)));

    /** Неразрушимая стена тюрьмы / подвала. */
    public static final RegistryObject<Block> JAIL_WALL = BLOCKS.register("jail_wall",
            () -> new Block(props("jail_wall").strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.METAL)));

    public static final RegistryObject<Block> FRIDGE = BLOCKS.register("fridge",
            () -> new FridgeBlock(props("fridge").strength(2.0F, 6.0F).sound(SoundType.METAL)));

    public static final RegistryObject<Block> ATM = BLOCKS.register("atm",
            () -> new FinanceBlock(props("atm").strength(3.0F, 1200.0F).sound(SoundType.METAL), false, "Банкомат «Реал Банк»"));

    public static final RegistryObject<Block> VENDING_FOOD = BLOCKS.register("vending_food",
            () -> new VendingBlock(props("vending_food").strength(3.0F, 1200.0F).sound(SoundType.METAL).lightLevel(s -> 7), Trades.VENDING_FOOD));

    public static final RegistryObject<Block> VENDING_RESOURCES = BLOCKS.register("vending_resources",
            () -> new VendingBlock(props("vending_resources").strength(3.0F, 1200.0F).sound(SoundType.METAL).lightLevel(s -> 7), Trades.VENDING_RESOURCES));

    public static final RegistryObject<Block> MONEY_PRINTER = BLOCKS.register("money_printer",
            () -> new com.baraaoi.reallife.block.MoneyPrinterBlock(props("money_printer").strength(3.0F, 6.0F).sound(SoundType.METAL)));

    /** «Сундук цеха»: сюда все станки игрока складывают грязные деньги. Выдаётся один раз. */
    public static final RegistryObject<Block> BUSINESS_CHEST = BLOCKS.register("business_chest",
            () -> new FridgeBlock(props("business_chest").strength(2.5F, 6.0F).sound(SoundType.WOOD)));

    /** Приёмник добычи в подвале Шайкина (неразрушимый). */
    public static final RegistryObject<Block> PIT_RECEIVER = BLOCKS.register("pit_receiver",
            () -> new PitReceiverBlock(props("pit_receiver").strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.METAL)));

    public static final RegistryObject<Block> LAUNDER_MACHINE = BLOCKS.register("launder_machine",
            () -> new LaunderMachineBlock(props("launder_machine").strength(3.0F, 6.0F).sound(SoundType.METAL)));

    public static final RegistryObject<Block> COIN_TABLE = BLOCKS.register("coin_table",
            () -> new CasinoBlock(casino("coin_table"), CasinoService.COIN));
    public static final RegistryObject<Block> SLOT_MACHINE = BLOCKS.register("slot_machine",
            () -> new CasinoBlock(casino("slot_machine").lightLevel(s -> 9), CasinoService.SLOTS));
    public static final RegistryObject<Block> ANARCHY_MACHINE = BLOCKS.register("anarchy_machine",
            () -> new CasinoBlock(casino("anarchy_machine").lightLevel(s -> 9), CasinoService.ANARCHY));
    public static final RegistryObject<Block> ROULETTE_TABLE = BLOCKS.register("roulette_table",
            () -> new CasinoBlock(casino("roulette_table"), CasinoService.ROULETTE));

    /** Касса легального заведения: главная ставится лицензией, дополнительные покупаются. */
    public static final RegistryObject<Block> BIZ_COUNTER = BLOCKS.register("biz_counter",
            () -> new com.baraaoi.reallife.block.BizCounterBlock(props("biz_counter").strength(2.5F, 6.0F).sound(SoundType.WOOD)));

    // ---- оборудование заведений (v6)
    public static final RegistryObject<Block> SHOP_SHELF = BLOCKS.register("shop_shelf",
            () -> new com.baraaoi.reallife.block.ShelfBlock(props("shop_shelf").strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final RegistryObject<Block> STORAGE_RACK = BLOCKS.register("storage_rack",
            () -> new com.baraaoi.reallife.block.ShelfBlock(props("storage_rack").strength(2.5F, 6.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> DISPLAY_CASE = BLOCKS.register("display_case",
            () -> new com.baraaoi.reallife.block.ShelfBlock(props("display_case").noOcclusion().strength(1.5F, 3.0F).sound(SoundType.GLASS)));
    public static final RegistryObject<Block> SAFE = BLOCKS.register("safe",
            () -> new com.baraaoi.reallife.block.ShelfBlock(props("safe").strength(6.0F, 1200.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> SHAWARMA_GRILL = BLOCKS.register("shawarma_grill",
            () -> new DecorBlock(props("shawarma_grill").strength(2.5F, 6.0F).sound(SoundType.METAL).lightLevel(s -> 8)));
    public static final RegistryObject<Block> PRESSURE_WASHER = BLOCKS.register("pressure_washer",
            () -> new DecorBlock(props("pressure_washer").strength(2.5F, 6.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> STONE_CRUSHER = BLOCKS.register("stone_crusher",
            () -> new DecorBlock(props("stone_crusher").strength(4.0F, 6.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> FUEL_PUMP = BLOCKS.register("fuel_pump",
            () -> new DecorBlock(props("fuel_pump").strength(3.0F, 6.0F).sound(SoundType.METAL).lightLevel(s -> 5)));

    /** Майнинг-ферма: корпус под комплектующие. */
    public static final RegistryObject<Block> CRYPTO_RIG = BLOCKS.register("crypto_rig",
            () -> new com.baraaoi.reallife.block.CryptoRigBlock(props("crypto_rig").strength(3.0F, 6.0F).sound(SoundType.METAL).lightLevel(s -> 4)));

    private static BlockBehaviour.Properties props(String name) {
        return BlockBehaviour.Properties.of().setId(BLOCKS.key(name));
    }

    private static BlockBehaviour.Properties casino(String name) {
        return props(name).strength(2.5F, 1200.0F).sound(SoundType.WOOD);
    }
}
