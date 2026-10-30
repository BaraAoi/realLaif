package com.baraaoi.reallife.block;

import com.baraaoi.reallife.container.FinanceContainer;
import com.baraaoi.reallife.economy.BankService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Стойка, открывающая финансовый интерфейс (банк или МФО).
 */
public class FinanceBlock extends DecorBlock {
    private final boolean mfo;
    private final String title;

    public FinanceBlock(Properties props, boolean mfo) {
        this(props, mfo, mfo ? "МФО «Шайкин и Ко»" : "«Реал Банк»");
    }

    public FinanceBlock(Properties props, boolean mfo, String title) {
        super(props);
        this.mfo = mfo;
        this.title = title;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new FinanceContainer(id, inv, pos, mfo),
                    Component.literal(title)), pos);
            BankService.sync(sp);
        }
        return InteractionResult.SUCCESS;
    }
}
