package com.baraaoi.reallife.block;

import com.baraaoi.reallife.tile.FridgeTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Холодильник (и «Сундук цеха»): хранилище на 27 слотов. */
public class FridgeBlock extends DecorBlock implements EntityBlock {
    public FridgeBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FridgeTileEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof MenuProvider mp) {
            player.openMenu(mp);
            if (this == com.baraaoi.reallife.registry.ModBlocks.BUSINESS_CHEST.get()) {
                world.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.6F, 0.9F);
            } else {
                world.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.4F);
            }
        }
        return InteractionResult.CONSUME;
    }
}
