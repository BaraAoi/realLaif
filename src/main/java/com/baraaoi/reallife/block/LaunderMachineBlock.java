package com.baraaoi.reallife.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Аппарат ООО «Отмыв денег» в подвале цеха: ПКМ — отмыть все грязные деньги из инвентаря. */
public class LaunderMachineBlock extends DecorBlock {

    public LaunderMachineBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            com.baraaoi.reallife.economy.BankService.launderAtMachine(sp, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
