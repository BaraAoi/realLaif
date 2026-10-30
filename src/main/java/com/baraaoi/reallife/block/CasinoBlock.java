package com.baraaoi.reallife.block;

import com.baraaoi.reallife.economy.CasinoService;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

/** Игровой автомат / стол казино. kind — см. {@link CasinoService}. */
public class CasinoBlock extends DecorBlock {
    private final int kind;

    public CasinoBlock(Properties props, int kind) {
        super(props);
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (!world.isRemote && player instanceof ServerPlayerEntity && hand == Hand.MAIN_HAND) {
            CasinoService.open((ServerPlayerEntity) player, pos, kind);
        }
        return ActionResultType.SUCCESS;
    }
}
