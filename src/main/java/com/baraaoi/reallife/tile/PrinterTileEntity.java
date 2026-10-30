package com.baraaoi.reallife.tile;

import com.baraaoi.reallife.container.PrinterContainer;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.item.DirtyMoneyItem;
import com.baraaoi.reallife.registry.ModTileEntities;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Печатный станок v5: печатает грязные купюры выбранного номинала с выбранной скоростью.
 * Готовые пачки уходят в «Сундук цеха» хозяина, а если он не стоит/полон — во встроенный лоток на 9 слотов.
 * Работает, только если станок подключён (зарегистрирован в собственности хозяина).
 */
public class PrinterTileEntity extends BlockEntity implements Container, MenuProvider {
    public static final int[] DENOMS = DirtyMoneyItem.DENOMS;
    /** Тиков на одну купюру: 10₽ — 5 с, 100₽ — 1 мин, 500₽ — 5 мин, 1000₽ — 10 мин, 5000₽ — 50 мин. */
    public static final int[] TICKS = {100, 1200, 6000, 12000, 60000};
    public static final int SIZE = 9;
    public static final int WORKING = 0, OFFLINE = 1, FULL = 2;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    
    private UUID owner;
    private int denomIdx = 1;
    private int progress;
    private int status = OFFLINE;
    private boolean active;
    private int check;
    /** Сколько всего напечатано (для статистики в окне). */
    private long printed;

    private final ContainerData data = new ContainerData() {
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

    public PrinterTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntities.PRINTER.get(), pos, state);
    }

    
    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.check = 0;
        setChanged();
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
        setChanged();
    }

    public int getStatus() {
        return status;
    }

    public long getPrinted() {
        return printed;
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;
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
            if (progress % 200 == 0) setChanged();
        }
        if (progress >= TICKS[denomIdx]) {
            if (status == FULL && level.getGameTime() % 40 != 0) return;
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
        if (owner == null || level == null) return false;
        MinecraftServer server = level.getServer();
        if (server == null) return false;
        Account a = EconomyData.get(server).get(owner);
        return a != null && a.captivity == Account.FREE && a.printerList.contains(worldPosition);
    }

    /** Напечатать одну купюру прямо сейчас (для тестов и тика). */
    public boolean printOne() {
        if (level == null) return false;
        ItemStack note = DirtyMoneyItem.of(denom(), 1);
        MinecraftServer server = level.getServer();
        if (server != null && owner != null) {
            Account a = EconomyData.get(server).get(owner);
            if (a != null && !a.businessChest.equals(BlockPos.ZERO) && level.isLoaded(a.businessChest)) {
                BlockEntity te = level.getBlockEntity(a.businessChest);
                if (te instanceof Container && te != this && insert((Container) te, note)) {
                    te.setChanged();
                    printed += denom();
                    setChanged();
                    return true;
                }
            }
        }
        if (insert(this, note)) {
            printed += denom();
            setChanged();
            return true;
        }
        return false;
    }

    /** Положить стопку в инвентарь (с объединением одинаковых). true — влезло целиком. */
    public static boolean insert(Container inv, ItemStack stack) {
        int size = inv.getContainerSize();
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            ItemStack cur = inv.getItem(i);
            if (!cur.isEmpty() && ItemStack.isSameItemSameComponents(cur, stack)) {
                int max = Math.min(cur.getMaxStackSize(), inv.getMaxStackSize());
                int n = Math.min(max - cur.getCount(), stack.getCount());
                if (n > 0) {
                    cur.grow(n);
                    stack.shrink(n);
                    inv.setItem(i, cur);
                }
            }
        }
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            if (inv.getItem(i).isEmpty() && inv.canPlaceItem(i, stack)) {
                int n = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), inv.getMaxStackSize()));
                ItemStack put = stack.copy();
                put.setCount(n);
                inv.setItem(i, put);
                stack.shrink(n);
            }
        }
        return stack.isEmpty();
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        owner = in.read("Owner", UUIDUtil.CODEC).orElse(null);
        denomIdx = Math.max(0, Math.min(DENOMS.length - 1, in.getIntOr("Denom", 1)));
        progress = in.getIntOr("Progress", 0);
        printed = in.getLongOr("Printed", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.storeNullable("Owner", UUIDUtil.CODEC, owner);
        out.putInt("Denom", denomIdx);
        out.putInt("Progress", progress);
        out.putLong("Printed", printed);
    }

    // ------------------------------------------------------------------ Container (лоток)

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack st : items) if (!st.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack r = ContainerHelper.removeItem(items, index, count);
        if (!r.isEmpty()) setChanged();
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(items, index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return stack.getItem() instanceof DirtyMoneyItem;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    // ------------------------------------------------------------------ окно

    @Override
    public Component getDisplayName() {
        return Component.literal("Печатный станок");
    }

    
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PrinterContainer(id, inv, this, data, worldPosition);
    }
}
