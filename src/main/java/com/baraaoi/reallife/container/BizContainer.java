package com.baraaoi.reallife.container;

import com.baraaoi.reallife.item.DirtyMoneyItem;
import com.baraaoi.reallife.registry.ModContainers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

/** Касса заведения: слот для грязных денег + инвентарь игрока. */
public class BizContainer extends AbstractContainerMenu {
    public static final int WIDTH = 230;
    public static final int HEIGHT = 222;
    public static final int SLOT_X = 200;
    public static final int SLOT_Y = 112;
    public static final int INV_X = 35;
    public static final int INV_Y = 140;

    private final SimpleContainer input = new SimpleContainer(1);
    private final BlockPos pos;

    public BizContainer(int id, Inventory inv, BlockPos pos) {
        super(ModContainers.BIZ.get(), id);
        this.pos = pos;
        this.addSlot(new Slot(input, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
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
                if (!(stack.getItem() instanceof DirtyMoneyItem) || !this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
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
