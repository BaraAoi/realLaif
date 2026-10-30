package com.baraaoi.reallife.container;

import com.baraaoi.reallife.item.DirtyMoneyItem;
import com.baraaoi.reallife.registry.ModContainers;
import com.baraaoi.reallife.tile.PrinterTileEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.core.BlockPos;

/** Окно печатного станка: лоток 9 слотов (только забрать) + инвентарь игрока. */
public class PrinterContainer extends AbstractContainerMenu {
    public static final int SLOT_Y = 72;
    public static final int INV_Y = 104;

    private final Container tray;
    private final ContainerData data;
    private final BlockPos pos;

    public PrinterContainer(int id, Inventory inv, BlockPos pos) {
        this(id, inv, new SimpleContainer(PrinterTileEntity.SIZE), new SimpleContainerData(5), pos);
    }

    public PrinterContainer(int id, Inventory inv, Container tray, ContainerData data, BlockPos pos) {
        super(ModContainers.PRINTER.get(), id);
        this.tray = tray;
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < PrinterTileEntity.SIZE; i++) {
            this.addSlot(new Slot(tray, i, 8 + i * 18, SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(inv, col, 8 + col * 18, INV_Y + 58));
        this.addDataSlots(data);
    }

    public BlockPos getPos() {
        return pos;
    }

    public Container getTray() {
        return tray;
    }

    /** Сменить номинал печати (только владелец станка). */
    public void setDenom(Player player, int idx) {
        if (!(tray instanceof PrinterTileEntity)) return;
        PrinterTileEntity te = (PrinterTileEntity) tray;
        if (te.getOwner() != null && !te.getOwner().equals(player.getUUID())) {
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
            if (index < PrinterTileEntity.SIZE) {
                if (!this.moveItemStackTo(stack, PrinterTileEntity.SIZE, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
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
