package com.baraaoi.reallife.block;

import com.baraaoi.reallife.tile.PrinterTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

/** Печатный станок: ПКМ — окно выбора номинала и лоток с напечатанным. */
public class MoneyPrinterBlock extends DecorBlock {
    public MoneyPrinterBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new PrinterTileEntity();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (world.isRemote) return ActionResultType.SUCCESS;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof PrinterTileEntity && player instanceof ServerPlayerEntity) {
            NetworkHooks.openGui((ServerPlayerEntity) player, (PrinterTileEntity) te, pos);
        }
        return ActionResultType.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.isIn(newState.getBlock())) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof IInventory) InventoryHelper.dropInventoryItems(world, pos, (IInventory) te);
            super.onReplaced(state, world, pos, newState, isMoving);
        }
    }
}
