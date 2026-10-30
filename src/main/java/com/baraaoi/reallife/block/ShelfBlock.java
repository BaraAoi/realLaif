package com.baraaoi.reallife.block;

import com.baraaoi.reallife.tile.ShelfTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Полка/стеллаж/витрина/сейф: хранилище на 27 слотов, чем больше товара — тем «полнее» выглядит. */
public class ShelfBlock extends DecorBlock implements EntityBlock {
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 3);

    public ShelfBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.defaultBlockState().setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FILL);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShelfTileEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof MenuProvider mp) {
            player.openMenu(mp);
            world.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.5F, 1.1F);
        }
        return InteractionResult.CONSUME;
    }
}
