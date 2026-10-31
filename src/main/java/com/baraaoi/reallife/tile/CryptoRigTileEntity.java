package com.baraaoi.reallife.tile;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.container.CryptoContainer;
import com.baraaoi.reallife.item.ComponentItem;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Майнинг-ферма: 0 — материнская плата, 1 — процессор, 2 — память, 3 — блок питания, 4 — кулер,
 * 5..8 — видеокарты (нужна хотя бы одна). Добытое копится внутри, кнопка в окне переводит в кошелёк.
 */
public class CryptoRigTileEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SIZE = 9;
    public static final int FIRST_GPU = 5;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private double mined;
    private int timer;

    public CryptoRigTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntities.CRYPTO_RIG.get(), pos, state);
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
        setChanged();
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
        setChanged();
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;
        if (++timer >= 20) {
            timer = 0;
            work(1);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
        mined = in.getDoubleOr("Mined", 0.0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        out.putDouble("Mined", mined);
    }

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
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return fits(index, stack);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Майнинг-ферма");
    }

    
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CryptoContainer(id, inv, this, worldPosition);
    }
}
