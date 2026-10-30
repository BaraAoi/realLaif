package com.baraaoi.reallife.container;

import com.baraaoi.reallife.registry.ModContainers;
import com.baraaoi.reallife.tile.CryptoRigTileEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

/** Окно майнинг-фермы: 9 слотов комплектующих + инвентарь игрока. */
public class CryptoContainer extends AbstractContainerMenu {
    /** Координаты слотов: плата, процессор, память, БП, кулер, 4 видеокарты. */
    public static final int[][] SLOTS = {{20, 30}, {44, 30}, {68, 30}, {20, 56}, {44, 56}, {100, 30}, {124, 30}, {100, 56}, {124, 56}};
    public static final int INV_Y = 112;

    private final Container rig;
    private final BlockPos pos;

    public CryptoContainer(int id, Inventory inv, BlockPos pos) {
        this(id, inv, new SimpleContainer(CryptoRigTileEntity.SIZE) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        }, pos);
    }

    public CryptoContainer(int id, Inventory inv, Container rig, BlockPos pos) {
        super(ModContainers.CRYPTO.get(), id);
        this.rig = rig;
        this.pos = pos;
        for (int i = 0; i < CryptoRigTileEntity.SIZE; i++) {
            final int slot = i;
            this.addSlot(new Slot(rig, i, SLOTS[i][0], SLOTS[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return CryptoRigTileEntity.fits(slot, stack);
                }

                @Override
                public int getMaxStackSize() {
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

    public Container getRig() {
        return rig;
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
            int n = CryptoRigTileEntity.SIZE;
            if (index < n) {
                if (!this.moveItemStackTo(stack, n, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                boolean moved = false;
                for (int i = 0; i < n && !moved; i++) {
                    Slot s = this.slots.get(i);
                    if (!s.hasItem() && CryptoRigTileEntity.fits(i, stack)) {
                        s.set(stack.split(1));
                        moved = true;
                    }
                }
                if (!moved) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }
}
