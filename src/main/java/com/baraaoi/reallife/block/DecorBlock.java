package com.baraaoi.reallife.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;

/** Простой блок с лицевой стороной (печатный станок и т.п.). */
public class DecorBlock extends HorizontalBlock {
    public DecorBlock(Properties props) {
        super(props);
        this.setDefaultState(this.stateContainer.getBaseState().with(HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(HORIZONTAL_FACING, context.getPlacementHorizontalFacing().getOpposite());
    }
}
