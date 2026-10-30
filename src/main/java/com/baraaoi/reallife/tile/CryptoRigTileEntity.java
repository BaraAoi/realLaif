package com.baraaoi.reallife.tile;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.container.CryptoContainer;
import com.baraaoi.reallife.item.ComponentItem;
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
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Майнинг-ферма: 0 — материнская плата, 1 — процессор, 2 — память, 3 — блок питания, 4 — кулер,
 * 5..8 — видеокарты (нужна хотя бы одна). Добытое копится внутри, кнопка в окне переводит в кошелёк.
 */
public class CryptoRigTileEntity extends TileEntity implements ITickableTileEntity, IInventory, INamedContainerProvider {
    public static final int SIZE = 9;
    public static final int FIRST_GPU = 5;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private double mined;
    private int timer;

    public CryptoRigTileEntity() {
        super(ModTileEntities.CRYPTO_RIG.get());
    }

    /** Какой вид комплектующей подходит в слот. */
    public static int kindForSlot(int slot) {
        return slot >= FIRST_GPU ? ComponentItem.GPU : slot;
    }

    public static boolean fits(int slot, ItemStack stack) {
        return stack.getItem() instanceof ComponentItem && ((ComponentItem) stack.getItem()).getKind() == kindForSlot(slot);
    }

    public int gpus() {
        int n = 0;
        for (int i = FIRST_GPU; i < SIZE; i++) if (fits(i, items.get(i))) n++;
        return n;
    }

    /** Чего не хватает для запуска (пусто — всё на месте). */
    public List<String> missing() {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < FIRST_GPU; i++) if (!fits(i, items.get(i))) l.add(ComponentItem.NAMES[i]);
        if (gpus() == 0) l.add(ComponentItem.NAMES[ComponentItem.GPU]);
        return l;
    }

    public boolean complete() {
        return missing().isEmpty();
    }

    public double getMined() {
        return mined;
    }

    public double takeMined() {
        double m = mined;
        mined = 0;
        markDirty();
        return m;
    }

    /** Сколько BTC в секунду добывает ферма сейчас. */
    public double ratePerSecond() {
        return complete() ? gpus() * Config.BTC_PER_GPU_SECOND.get() : 0;
    }

    /** Отработать n секунд (для тика и тестов). */
    public void work(int seconds) {
        double r = ratePerSecond();
        if (r <= 0) return;
        mined += r * seconds;
        markDirty();
    }

    @Override
    public void tick() {
        if (world == null || world.isRemote) return;
        if (++timer >= 20) {
            timer = 0;
            work(1);
        }
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(nbt, items);
        mined = nbt.getDouble("Mined");
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        super.write(nbt);
        ItemStackHelper.saveAllItems(nbt, items);
        nbt.putDouble("Mined", mined);
        return nbt;
    }

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
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player) {
        return world != null && world.getTileEntity(pos) == this
                && player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return fits(index, stack);
    }

    @Override
    public void clear() {
        items.clear();
        markDirty();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent("Майнинг-ферма");
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
        return new CryptoContainer(id, inv, this, pos);
    }
}
