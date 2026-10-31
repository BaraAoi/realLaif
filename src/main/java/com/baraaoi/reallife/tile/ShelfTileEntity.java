package com.baraaoi.reallife.tile;

import com.baraaoi.reallife.block.ShelfBlock;
import com.baraaoi.reallife.registry.ModTileEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ShelfTileEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    public ShelfTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntities.SHELF.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!this.trySaveLootTable(out)) ContainerHelper.saveAllItems(out, this.items);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(in)) ContainerHelper.loadAllItems(in, this.items);
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> itemsIn) {
        this.items = itemsIn;
    }

    /** По занятым слотам (коробки не стакаются): 0 — пусто, 1 — 1–8 слотов, 2 — 9–17, 3 — 18 и больше. */
    public int fillLevel() {
        int n = 0;
        for (ItemStack s : items) if (!s.isEmpty()) n++;
        return fillFor(n);
    }

    public static int fillFor(int usedSlots) {
        if (usedSlots <= 0) return 0;
        return usedSlots <= 8 ? 1 : usedSlots <= 17 ? 2 : 3;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState st = this.getBlockState();
            if (st.hasProperty(ShelfBlock.FILL)) {
                int f = fillLevel();
                if (st.getValue(ShelfBlock.FILL) != f) this.level.setBlock(this.worldPosition, st.setValue(ShelfBlock.FILL, f), 3);
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return this.getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory player) {
        return ChestMenu.threeRows(id, player, this);
    }
}
