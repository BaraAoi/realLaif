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
            () -> new DecorBlock(AbstractBlock.Properties.create(Material.IRON).hardnessAndResistance(3.0F, 6.0F).sound(SoundType.METAL)));

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

    private static AbstractBlock.Properties casino() {
        return AbstractBlock.Properties.create(Material.WOOD).hardnessAndResistance(2.5F, 1200.0F).sound(SoundType.WOOD);
    }
}
