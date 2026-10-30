package com.baraaoi.reallife.registry;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.tile.CryptoRigTileEntity;
import com.baraaoi.reallife.tile.FridgeTileEntity;
import com.baraaoi.reallife.tile.PrinterTileEntity;
import com.baraaoi.reallife.tile.ShelfTileEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class ModTileEntities {
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RealLife.MODID);

    public static final RegistryObject<BlockEntityType<FridgeTileEntity>> FRIDGE = TILES.register("fridge",
            () -> new BlockEntityType<>(FridgeTileEntity::new, Set.of(ModBlocks.FRIDGE.get(), ModBlocks.BUSINESS_CHEST.get())));

    public static final RegistryObject<BlockEntityType<PrinterTileEntity>> PRINTER = TILES.register("money_printer",
            () -> new BlockEntityType<>(PrinterTileEntity::new, Set.of(ModBlocks.MONEY_PRINTER.get())));

    public static final RegistryObject<BlockEntityType<ShelfTileEntity>> SHELF = TILES.register("shelf",
            () -> new BlockEntityType<>(ShelfTileEntity::new, Set.of(ModBlocks.SHOP_SHELF.get(),
                    ModBlocks.STORAGE_RACK.get(), ModBlocks.DISPLAY_CASE.get(), ModBlocks.SAFE.get())));

    public static final RegistryObject<BlockEntityType<CryptoRigTileEntity>> CRYPTO_RIG = TILES.register("crypto_rig",
            () -> new BlockEntityType<>(CryptoRigTileEntity::new, Set.of(ModBlocks.CRYPTO_RIG.get())));
}
