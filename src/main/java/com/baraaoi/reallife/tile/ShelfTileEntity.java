package com.baraaoi.reallife.tile;

import com.baraaoi.reallife.block.ShelfBlock;
import com.baraaoi.reallife.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

/** Полка / стеллаж / витрина / сейф заведения: 27 слотов; заполненность видна на блоке (FILL 0..3). */
public class ShelfTileEntity extends LockableLootTileEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    public ShelfTileEntity() {
        super(ModTileEntities.SHELF.get());
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        if (!this.checkLootAndWrite(compound)) ItemStackHelper.saveAllItems(compound, this.items);
        return compound;
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        this.items = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        if (!this.checkLootAndRead(nbt)) ItemStackHelper.loadAllItems(nbt, this.items);
    }

    @Override
    public int getSizeInventory() {
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

    /** 0 — пусто, 1 — немного (до 15 шт.), 2 — наполовину (до 47), 3 — забито. */
    public int fillLevel() {
        int n = 0;
        for (ItemStack s : items) if (!s.isEmpty()) n += s.getCount();
        if (n == 0) return 0;
        return n < 16 ? 1 : n < 48 ? 2 : 3;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (this.world != null && !this.world.isRemote) {
            BlockState st = this.getBlockState();
            if (st.hasProperty(ShelfBlock.FILL)) {
                int f = fillLevel();
                if (st.get(ShelfBlock.FILL) != f) this.world.setBlockState(this.pos, st.with(ShelfBlock.FILL, f), 3);
            }
        }
    }

    @Override
    protected ITextComponent getDefaultName() {
        if (this.world != null) return new net.minecraft.util.text.TranslationTextComponent(this.getBlockState().getBlock().getTranslationKey());
        return new StringTextComponent("Полка");
    }

    @Override
    protected Container createMenu(int id, PlayerInventory player) {
        return ChestContainer.createGeneric9X3(id, player, this);
    }
}
