package com.baraaoi.reallife.registry;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.tile.FridgeTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModTileEntities {
    public static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, RealLife.MODID);

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<TileEntityType<FridgeTileEntity>> FRIDGE = TILES.register("fridge",
            () -> TileEntityType.Builder.create(FridgeTileEntity::new, ModBlocks.FRIDGE.get()).build(null));
}
