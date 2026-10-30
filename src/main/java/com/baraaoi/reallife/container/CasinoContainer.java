package com.baraaoi.reallife.container;

import com.baraaoi.reallife.registry.ModContainers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/** Окно игры в казино (без слотов: ставки — со счёта, затем наличными). */
public class CasinoContainer extends Container {
    private final BlockPos pos;
    private final int kind;

    public CasinoContainer(int id, PlayerInventory inv, BlockPos pos, int kind) {
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
    public boolean canInteractWith(PlayerEntity player) {
        return player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }
}
