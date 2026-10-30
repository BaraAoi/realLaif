package com.baraaoi.reallife.tile;

import com.baraaoi.reallife.container.PrinterContainer;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.item.DirtyMoneyItem;
import com.baraaoi.reallife.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Печатный станок v5: печатает грязные купюры выбранного номинала с выбранной скоростью.
 * Готовые пачки уходят в «Сундук цеха» хозяина, а если он не стоит/полон — во встроенный лоток на 9 слотов.
 * Работает, только если станок подключён (зарегистрирован в собственности хозяина).
 */
public class PrinterTileEntity extends TileEntity implements ITickableTileEntity, IInventory, INamedContainerProvider {
    public static final int[] DENOMS = DirtyMoneyItem.DENOMS;
    /** Тиков на одну купюру: 10₽ — 5 с, 100₽ — 1 мин, 500₽ — 5 мин, 1000₽ — 10 мин, 5000₽ — 50 мин. */
    public static final int[] TICKS = {100, 1200, 6000, 12000, 60000};
    public static final int SIZE = 9;
    public static final int WORKING = 0, OFFLINE = 1, FULL = 2;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    @Nullable
    private UUID owner;
    private int denomIdx = 1;
    private int progress;
    private int status = OFFLINE;
    private boolean active;
    private int check;
    /** Сколько всего напечатано (для статистики в окне). */
    private long printed;

    private final IIntArray data = new IIntArray() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return denomIdx;
                case 1: return (int) Math.min(1000, (long) progress * 1000L / TICKS[denomIdx]);
                case 2: return Math.max(0, (TICKS[denomIdx] - progress) / 20);
                case 3: return status;
                default: return (int) Math.min(30000, printed / 100);
            }
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int size() {
            return 5;
        }
    };

    public PrinterTileEntity() {
        super(ModTileEntities.PRINTER.get());
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        this.check = 0;
        markDirty();
    }

    public int getDenomIdx() {
        return denomIdx;
    }

    public int denom() {
        return DENOMS[denomIdx];
    }

    public void setDenomIdx(int idx) {
        if (idx < 0 || idx >= DENOMS.length || idx == denomIdx) return;
        denomIdx = idx;
        progress = 0;
        markDirty();
    }

    public int getStatus() {
        return status;
    }

    public long getPrinted() {
        return printed;
    }

    @Override
    public void tick() {
        if (world == null || world.isRemote) return;
        if (--check <= 0) {
            check = 40;
            active = isRegistered();
        }
        if (!active) {
            status = OFFLINE;
            return;
        }
        if (progress < TICKS[denomIdx]) {
            progress++;
            status = WORKING;
            if (progress % 200 == 0) markDirty();
        }
        if (progress >= TICKS[denomIdx]) {
            if (status == FULL && world.getGameTime() % 40 != 0) return;
            if (printOne()) {
                progress = 0;
                status = WORKING;
            } else {
                status = FULL;
            }
        }
    }

    /** Подключён ли станок: у хозяина есть счёт, станок в его списке, хозяин на свободе. */
    private boolean isRegistered() {
        if (owner == null || world == null) return false;
        MinecraftServer server = world.getServer();
        if (server == null) return false;
        Account a = EconomyData.get(server).get(owner);
        return a != null && a.captivity == Account.FREE && a.printerList.contains(pos);
    }

    /** Напечатать одну купюру прямо сейчас (для тестов и тика). */
    public boolean printOne() {
        if (world == null) return false;
        ItemStack note = DirtyMoneyItem.of(denom(), 1);
        MinecraftServer server = world.getServer();
        if (server != null && owner != null) {
            Account a = EconomyData.get(server).get(owner);
            if (a != null && !a.businessChest.equals(BlockPos.ZERO) && world.isBlockLoaded(a.businessChest)) {
                TileEntity te = world.getTileEntity(a.businessChest);
                if (te instanceof IInventory && te != this && insert((IInventory) te, note)) {
                    te.markDirty();
                    printed += denom();
                    markDirty();
                    return true;
                }
            }
        }
        if (insert(this, note)) {
            printed += denom();
            markDirty();
            return true;
        }
        return false;
    }

    /** Положить стопку в инвентарь (с объединением одинаковых). true — влезло целиком. */
    public static boolean insert(IInventory inv, ItemStack stack) {
        int size = inv.getSizeInventory();
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            ItemStack cur = inv.getStackInSlot(i);
            if (!cur.isEmpty() && ItemStack.areItemsEqual(cur, stack) && ItemStack.areItemStackTagsEqual(cur, stack)) {
                int max = Math.min(cur.getMaxStackSize(), inv.getInventoryStackLimit());
                int n = Math.min(max - cur.getCount(), stack.getCount());
                if (n > 0) {
                    cur.grow(n);
                    stack.shrink(n);
                    inv.setInventorySlotContents(i, cur);
                }
            }
        }
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            if (inv.getStackInSlot(i).isEmpty() && inv.isItemValidForSlot(i, stack)) {
                inv.setInventorySlotContents(i, stack.copy());
                stack.setCount(0);
            }
        }
        return stack.isEmpty();
    }

    // ------------------------------------------------------------------ NBT

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(nbt, items);
        owner = nbt.hasUniqueId("Owner") ? nbt.getUniqueId("Owner") : null;
        denomIdx = Math.max(0, Math.min(DENOMS.length - 1, nbt.contains("Denom") ? nbt.getInt("Denom") : 1));
        progress = nbt.getInt("Progress");
        printed = nbt.getLong("Printed");
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        super.write(nbt);
        ItemStackHelper.saveAllItems(nbt, items);
        if (owner != null) nbt.putUniqueId("Owner", owner);
        nbt.putInt("Denom", denomIdx);
        nbt.putInt("Progress", progress);
        nbt.putLong("Printed", printed);
        return nbt;
    }

    // ------------------------------------------------------------------ IInventory (лоток)

    @Override
    public int getSizeInventory() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack st : items) if (!st.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack r = ItemStackHelper.getAndSplit(items, index, count);
        if (!r.isEmpty()) markDirty();
        return r;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStackHelper.getAndRemove(items, index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        items.set(index, stack);
        markDirty();
    }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player) {
        return world != null && world.getTileEntity(pos) == this
                && player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return stack.getItem() instanceof DirtyMoneyItem;
    }

    @Override
    public void clear() {
        items.clear();
        markDirty();
    }

    // ------------------------------------------------------------------ окно

    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent("Печатный станок");
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
        return new PrinterContainer(id, inv, this, data, pos);
    }
}
