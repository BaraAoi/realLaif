package com.baraaoi.reallife.registry;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.block.BankBlock;
import com.baraaoi.reallife.block.MfoBlock;
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
}
