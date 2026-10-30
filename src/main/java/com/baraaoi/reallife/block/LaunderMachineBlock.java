package com.baraaoi.reallife.block;

import com.baraaoi.reallife.economy.BankService;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

/** Аппарат ООО «Отмыв денег» в подвале цеха: ПКМ — отмыть все грязные деньги из инвентаря. */
public class LaunderMachineBlock extends DecorBlock {
    public LaunderMachineBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (!world.isRemote && player instanceof ServerPlayerEntity && hand == Hand.MAIN_HAND) {
            BankService.launderAtMachine((ServerPlayerEntity) player, pos);
        }
        return ActionResultType.SUCCESS;
    }
}
