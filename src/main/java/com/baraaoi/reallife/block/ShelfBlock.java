package com.baraaoi.reallife.block;

import com.baraaoi.reallife.tile.ShelfTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** Полка/стеллаж/витрина/сейф: хранилище на 27 слотов, чем больше товара — тем «полнее» выглядит. */
public class ShelfBlock extends DecorBlock {
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 3);

    public ShelfBlock(Properties props) {
        super(props);
        this.setDefaultState(this.getDefaultState().with(FILL, 0));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(FILL);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new ShelfTileEntity();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (world.isRemote) return ActionResultType.SUCCESS;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof INamedContainerProvider) {
            player.openContainer((INamedContainerProvider) te);
            world.playSound(null, pos, SoundEvents.BLOCK_BARREL_OPEN, SoundCategory.BLOCKS, 0.5F, 1.1F);
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
