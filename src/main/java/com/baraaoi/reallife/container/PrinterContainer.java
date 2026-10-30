package com.baraaoi.reallife.container;

import com.baraaoi.reallife.item.DirtyMoneyItem;
import com.baraaoi.reallife.registry.ModContainers;
import com.baraaoi.reallife.tile.PrinterTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;
import net.minecraft.util.math.BlockPos;

/** Окно печатного станка: лоток 9 слотов (только забрать) + инвентарь игрока. */
public class PrinterContainer extends Container {
    public static final int SLOT_Y = 72;
    public static final int INV_Y = 104;

    private final IInventory tray;
    private final IIntArray data;
    private final BlockPos pos;

    public PrinterContainer(int id, PlayerInventory inv, BlockPos pos) {
        this(id, inv, new Inventory(PrinterTileEntity.SIZE), new IntArray(5), pos);
    }

    public PrinterContainer(int id, PlayerInventory inv, IInventory tray, IIntArray data, BlockPos pos) {
        super(ModContainers.PRINTER.get(), id);
        this.tray = tray;
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < PrinterTileEntity.SIZE; i++) {
            this.addSlot(new Slot(tray, i, 8 + i * 18, SLOT_Y) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(inv, col, 8 + col * 18, INV_Y + 58));
        this.trackIntArray(data);
    }

    public BlockPos getPos() {
        return pos;
    }

    public IInventory getTray() {
        return tray;
    }

    /** Сменить номинал печати (только владелец станка). */
    public void setDenom(PlayerEntity player, int idx) {
        if (!(tray instanceof PrinterTileEntity)) return;
        PrinterTileEntity te = (PrinterTileEntity) tray;
        if (te.getOwner() != null && !te.getOwner().equals(player.getUniqueID())) {
            com.baraaoi.reallife.economy.BankService.msg(player, "§8[Станок] §cЧужой станок — настройки заблокированы.");
            return;
        }
        te.setDenomIdx(idx);
    }

    public int denomIdx() { return data.get(0); }
    public int permille() { return data.get(1); }
    public int secondsLeft() { return data.get(2); }
    public int status() { return data.get(3); }
    public int printedHundreds() { return data.get(4); }

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
            if (index < PrinterTileEntity.SIZE) {
                if (!this.mergeItemStack(stack, PrinterTileEntity.SIZE, this.inventorySlots.size(), true)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }

    @SuppressWarnings("unused")
    private static boolean dirty(ItemStack st) {
        return st.getItem() instanceof DirtyMoneyItem;
    }
}
