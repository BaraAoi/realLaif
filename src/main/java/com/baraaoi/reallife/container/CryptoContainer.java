package com.baraaoi.reallife.container;

import com.baraaoi.reallife.registry.ModContainers;
import com.baraaoi.reallife.tile.CryptoRigTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/** Окно майнинг-фермы: 9 слотов комплектующих + инвентарь игрока. */
public class CryptoContainer extends Container {
    /** Координаты слотов: плата, процессор, память, БП, кулер, 4 видеокарты. */
    public static final int[][] SLOTS = {{20, 30}, {44, 30}, {68, 30}, {20, 56}, {44, 56}, {100, 30}, {124, 30}, {100, 56}, {124, 56}};
    public static final int INV_Y = 112;

    private final IInventory rig;
    private final BlockPos pos;

    public CryptoContainer(int id, PlayerInventory inv, BlockPos pos) {
        this(id, inv, new Inventory(CryptoRigTileEntity.SIZE) {
            @Override
            public int getInventoryStackLimit() {
                return 1;
            }
        }, pos);
    }

    public CryptoContainer(int id, PlayerInventory inv, IInventory rig, BlockPos pos) {
        super(ModContainers.CRYPTO.get(), id);
        this.rig = rig;
        this.pos = pos;
        for (int i = 0; i < CryptoRigTileEntity.SIZE; i++) {
            final int slot = i;
            this.addSlot(new Slot(rig, i, SLOTS[i][0], SLOTS[i][1]) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return CryptoRigTileEntity.fits(slot, stack);
                }

                @Override
                public int getSlotStackLimit() {
                    return 1;
                }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(inv, col, 8 + col * 18, INV_Y + 58));
    }

    public BlockPos getPos() {
        return pos;
    }

    public IInventory getRig() {
        return rig;
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
            int n = CryptoRigTileEntity.SIZE;
            if (index < n) {
                if (!this.mergeItemStack(stack, n, this.inventorySlots.size(), true)) return ItemStack.EMPTY;
            } else {
                boolean moved = false;
                for (int i = 0; i < n && !moved; i++) {
                    Slot s = this.inventorySlots.get(i);
                    if (!s.getHasStack() && CryptoRigTileEntity.fits(i, stack)) {
                        s.putStack(stack.split(1));
                        moved = true;
                    }
                }
                if (!moved) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }
}
