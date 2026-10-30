package com.baraaoi.reallife.block;

import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.registry.ModTileEntities;
import com.baraaoi.reallife.tile.CryptoRigTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Майнинг-ферма: ПКМ — окно со слотами комплектующих. */
public class CryptoRigBlock extends DecorBlock implements EntityBlock {
    public CryptoRigBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CryptoRigTileEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModTileEntities.CRYPTO_RIG.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<CryptoRigTileEntity>) (l, p, s, be) -> be.tick();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof CryptoRigTileEntity rig && player instanceof ServerPlayer sp) {
            sp.openMenu(rig, pos);
            BankService.sync(sp);
        }
        return InteractionResult.CONSUME;
    }
}
