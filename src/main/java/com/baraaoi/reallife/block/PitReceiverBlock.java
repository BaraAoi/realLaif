package com.baraaoi.reallife.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Приёмник добычи в подвале Шайкина: ПКМ — сдать всю добычу из инвентаря в счёт долга. */
public class PitReceiverBlock extends DecorBlock {

    public PitReceiverBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            com.baraaoi.reallife.event.DebtManager.pitDeliver(sp);
        }
        return InteractionResult.SUCCESS;
    }
}
