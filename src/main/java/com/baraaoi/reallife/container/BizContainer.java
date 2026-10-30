package com.baraaoi.reallife.container;

import com.baraaoi.reallife.item.DirtyMoneyItem;
import com.baraaoi.reallife.registry.ModContainers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/** Касса заведения: слот для грязных денег + инвентарь игрока. */
public class BizContainer extends Container {
    public static final int WIDTH = 230;
    public static final int HEIGHT = 222;
    public static final int SLOT_X = 200;
    public static final int SLOT_Y = 112;
    public static final int INV_X = 35;
    public static final int INV_Y = 140;

    private final Inventory input = new Inventory(1);
    private final BlockPos pos;

    public BizContainer(int id, PlayerInventory inv, BlockPos pos) {
        super(ModContainers.BIZ.get(), id);
        this.pos = pos;
        this.addSlot(new Slot(input, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof DirtyMoneyItem;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(inv, col, INV_X + col * 18, INV_Y + 58));
    }

    public BlockPos getPos() {
        return pos;
    }

    public Inventory getInput() {
        return input;
    }

    @Override
    public boolean canInteractWith(PlayerEntity player) {
        return player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index == 0) {
                if (!this.mergeItemStack(stack, 1, 37, true)) return ItemStack.EMPTY;
            } else {
                if (!(stack.getItem() instanceof DirtyMoneyItem) || !this.mergeItemStack(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public void onContainerClosed(PlayerEntity player) {
        super.onContainerClosed(player);
        this.clearContainer(player, player.world, input);
    }
}
