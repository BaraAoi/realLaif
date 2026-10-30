package com.baraaoi.reallife.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Торговый автомат: интерфейс торговли за купюры (как у жителя). */
public class VendingBlock extends DecorBlock {
    private final int kind;

    public VendingBlock(Properties props, int kind) {
        super(props);
        this.kind = kind;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            com.baraaoi.reallife.economy.VendingMerchant m = new com.baraaoi.reallife.economy.VendingMerchant(sp, com.baraaoi.reallife.economy.Trades.vending(kind));
            m.setTradingPlayer(sp);
            m.openTradingScreen(sp, net.minecraft.network.chat.Component.literal(kind == com.baraaoi.reallife.economy.Trades.VENDING_FOOD
                    ? "Автомат «Перекус»" : "Автомат «Всё для дома»"), 1);
        }
        return InteractionResult.SUCCESS;
    }
}
