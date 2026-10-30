package com.baraaoi.reallife.block;

import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

/** Приёмник добычи в подвале Шайкина: ПКМ — сдать всю добычу из инвентаря в счёт долга. */
public class PitReceiverBlock extends DecorBlock {
    public PitReceiverBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (!world.isRemote && player instanceof ServerPlayerEntity && hand == Hand.MAIN_HAND) {
            DebtManager.pitDeliver((ServerPlayerEntity) player);
        }
        return ActionResultType.SUCCESS;
    }
}
