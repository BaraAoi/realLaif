package com.baraaoi.reallife.container;

import com.baraaoi.reallife.economy.ItemValues;
import com.baraaoi.reallife.registry.ModContainers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

/**
 * Контейнер банка / МФО: один слот для внесения ресурсов + инвентарь игрока.
 */
public class FinanceContainer extends AbstractContainerMenu {
    public static final int WIDTH = 256;
    public static final int HEIGHT = 232;
    public static final int SLOT_X = 224;
    public static final int SLOT_Y = 120;
    public static final int INV_X = 48;
    public static final int INV_Y = 150;

    private final SimpleContainer input = new SimpleContainer(1);
    private final BlockPos pos;
    private final boolean mfo;

    public FinanceContainer(int id, Inventory inv, BlockPos pos, boolean mfo) {
        super(mfo ? ModContainers.MFO.get() : ModContainers.BANK.get(), id);
        this.pos = pos;
        this.mfo = mfo;
        this.addSlot(new Slot(input, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ItemValues.valueOf(stack) > 0;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(inv, col, INV_X + col * 18, INV_Y + 58));
    }

    public boolean isMfo() {
        return mfo;
    }

    public SimpleContainer getInput() {
        return input;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
            } else {
                if (ItemValues.valueOf(stack) <= 0 || !this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, input);
    }
}
