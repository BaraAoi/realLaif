package com.baraaoi.reallife.item;

import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.LegalBiz;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** Лицензия на заведение (вид — в NBT "BizType"). ПКМ по блоку — ставится главная касса заведения. */
public class BizPermitItem extends Item {
    public BizPermitItem(Properties props) {
        super(props);
    }

    public static ItemStack create(Item item, BizType type) {
        ItemStack s = new ItemStack(item);
        s.getOrCreateTag().putInt("BizType", type.ordinal());
        return s;
    }

    @Nullable
    public static BizType typeOf(ItemStack stack) {
        CompoundNBT t = stack.getTag();
        return t == null || !t.contains("BizType") ? null : BizType.byId(t.getInt("BizType"));
    }

    @Override
    public void fillItemGroup(net.minecraft.item.ItemGroup group, net.minecraft.util.NonNullList<ItemStack> items) {
        if (this.isInGroup(group)) for (BizType t : BizType.values()) items.add(create(this, t));
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        BizType t = typeOf(stack);
        return new StringTextComponent(t == null ? "Лицензия на заведение" : "Лицензия: " + t.title);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext ctx) {
        World world = ctx.getWorld();
        PlayerEntity player = ctx.getPlayer();
        if (world.isRemote || !(player instanceof ServerPlayerEntity)) return ActionResultType.SUCCESS;
        BizType type = typeOf(ctx.getItem());
        if (type == null) {
            player.sendStatusMessage(new StringTextComponent("§cРазрешение испорчено."), false);
            return ActionResultType.FAIL;
        }
        Direction facing = ctx.getPlacementHorizontalFacing().getOpposite();
        BlockPos pos = world.getBlockState(ctx.getPos()).getMaterial().isReplaceable() ? ctx.getPos() : ctx.getPos().offset(ctx.getFace());
        if (LegalBiz.place((ServerPlayerEntity) player, pos, facing, type)) {
            ctx.getItem().shrink(1);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.FAIL;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        BizType t = typeOf(stack);
        if (t != null) {
            tooltip.add(new StringTextComponent("§7Выручка: §a" + t.base + "₽§7/день на работника, до §e" + t.maxWorkers + "§7 работников"));
            tooltip.add(new StringTextComponent("§7Зарплата работника: §c" + t.wage() + "₽§7/день"));
            tooltip.add(new StringTextComponent("§7Комплект на работника: §e" + LegalBiz.kitText(t)));
        }
        tooltip.add(new StringTextComponent("§7ПКМ по блоку на своей земле — там встанет главная касса."));
        tooltip.add(new StringTextComponent("§7Здание строишь сам; оборудование ставь в ±8 блоках от кассы."));
        tooltip.add(new StringTextComponent("§8Сломаешь главную кассу — заведение закроется, лицензия вернётся."));
    }
}
