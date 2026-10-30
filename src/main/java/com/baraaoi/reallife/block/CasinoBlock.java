package com.baraaoi.reallife.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Игровой автомат / стол казино. kind — см. CasinoService. */
public class CasinoBlock extends DecorBlock {
    private final int kind;

    public int getKind() {
        return kind;
    }

    public CasinoBlock(Properties props, int kind) {
        super(props);
        this.kind = kind;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            com.baraaoi.reallife.economy.CasinoService.open(sp, pos, kind);
        }
        return InteractionResult.SUCCESS;
    }
}
