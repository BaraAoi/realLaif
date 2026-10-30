package com.baraaoi.reallife.registry;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.tile.CryptoRigTileEntity;
import com.baraaoi.reallife.tile.FridgeTileEntity;
import com.baraaoi.reallife.tile.PrinterTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModTileEntities {
    public static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, RealLife.MODID);

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<TileEntityType<FridgeTileEntity>> FRIDGE = TILES.register("fridge",
            () -> TileEntityType.Builder.create(FridgeTileEntity::new, ModBlocks.FRIDGE.get(), ModBlocks.BUSINESS_CHEST.get()).build(null));

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<TileEntityType<PrinterTileEntity>> PRINTER = TILES.register("money_printer",
            () -> TileEntityType.Builder.create(PrinterTileEntity::new, ModBlocks.MONEY_PRINTER.get()).build(null));

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<TileEntityType<CryptoRigTileEntity>> CRYPTO_RIG = TILES.register("crypto_rig",
            () -> TileEntityType.Builder.create(CryptoRigTileEntity::new, ModBlocks.CRYPTO_RIG.get()).build(null));
}
