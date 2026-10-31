package com.baraaoi.reallife.container;

import com.baraaoi.reallife.registry.ModContainers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

/** Окно игры в казино (без слотов: ставки — со счёта, затем наличными). */
public class CasinoContainer extends AbstractContainerMenu {
    private final BlockPos pos;
    private final int kind;

    public CasinoContainer(int id, Inventory inv, BlockPos pos, int kind) {
        super(ModContainers.CASINO.get(), id);
        this.pos = pos;
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
