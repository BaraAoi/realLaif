package com.baraaoi.reallife.block;

import com.baraaoi.reallife.container.FinanceContainer;
import com.baraaoi.reallife.economy.BankService;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/**
 * Стойка, открывающая финансовый интерфейс (банк или МФО).
 */
public class FinanceBlock extends HorizontalBlock {
    private final boolean mfo;
    private final String title;

    public FinanceBlock(Properties props, boolean mfo) {
        this(props, mfo, mfo ? "МФО «Шайкин и Ко»" : "«Реал Банк»");
    }

    public FinanceBlock(Properties props, boolean mfo, String title) {
        super(props);
        this.mfo = mfo;
        this.title = title;
        this.setDefaultState(this.stateContainer.getBaseState().with(HORIZONTAL_FACING, net.minecraft.util.Direction.NORTH));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(HORIZONTAL_FACING, context.getPlacementHorizontalFacing().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (!world.isRemote && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity sp = (ServerPlayerEntity) player;
            NetworkHooks.openGui(sp, new SimpleNamedContainerProvider(
                    (id, inv, p) -> new FinanceContainer(id, inv, pos, mfo),
                    new StringTextComponent(title)), pos);
            BankService.sync(sp);
        }
        return ActionResultType.SUCCESS;
    }
}
