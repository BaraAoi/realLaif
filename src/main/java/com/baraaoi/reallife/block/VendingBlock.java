package com.baraaoi.reallife.block;

import com.baraaoi.reallife.economy.Trades;
import com.baraaoi.reallife.economy.VendingMerchant;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

/** Торговый автомат: интерфейс торговли за купюры (как у жителя). */
public class VendingBlock extends DecorBlock {
    private final int kind;

    public VendingBlock(Properties props, int kind) {
        super(props);
        this.kind = kind;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (!world.isRemote) {
            VendingMerchant m = new VendingMerchant(player, Trades.vending(kind));
            m.setCustomer(player);
            m.openMerchantContainer(player, new StringTextComponent(kind == Trades.VENDING_FOOD
                    ? "Автомат «Перекус»" : "Автомат «Всё для дома»"), 1);
        }
        return ActionResultType.SUCCESS;
    }
}
