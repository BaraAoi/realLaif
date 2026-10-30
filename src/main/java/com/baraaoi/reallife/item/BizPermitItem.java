package com.baraaoi.reallife.item;

import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.LegalBiz;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

/** Лицензия на заведение (вид — в NBT "BizType"). ПКМ по блоку — ставится главная касса заведения. */
public class BizPermitItem extends Item {
    public BizPermitItem(Properties props) {
        super(props);
    }

    public static ItemStack create(Item item, BizType type) {
        ItemStack s = new ItemStack(item);
        com.baraaoi.reallife.util.Nbt.editItem(s, _t -> _t.putInt("BizType", type.ordinal()));
        return s;
    }

    /** v1.10: лицензия + стройка «под ключ». */
    public static ItemStack createTurnkey(Item item, BizType type) {
        ItemStack s = create(item, type);
        com.baraaoi.reallife.util.Nbt.editItem(s, _t -> _t.putBoolean("Turnkey", true));
        return s;
    }

    /** v1.10: разрешение на строительство торгового центра. */
    public static ItemStack createMall(Item item) {
        ItemStack s = new ItemStack(item);
        com.baraaoi.reallife.util.Nbt.editItem(s, _t -> _t.putBoolean("Mall", true));
        return s;
    }

    public static boolean isTurnkey(ItemStack stack) {
        return com.baraaoi.reallife.util.Nbt.itemTag(stack) != null && com.baraaoi.reallife.util.Nbt.itemTag(stack).getBooleanOr("Turnkey", false);
    }

    public static boolean isMall(ItemStack stack) {
        return com.baraaoi.reallife.util.Nbt.itemTag(stack) != null && com.baraaoi.reallife.util.Nbt.itemTag(stack).getBooleanOr("Mall", false);
    }

    
    public static BizType typeOf(ItemStack stack) {
        CompoundTag t = com.baraaoi.reallife.util.Nbt.itemTag(stack);
        return t == null || !t.contains("BizType") ? null : BizType.byId(t.getIntOr("BizType", 0));
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isMall(stack)) return Component.literal("Разрешение на строительство ТЦ");
        BizType t = typeOf(stack);
        if (t != null && isTurnkey(stack)) return Component.literal("Под ключ: " + t.title);
        return Component.literal(t == null ? "Лицензия на заведение" : "Лицензия: " + t.title);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (world.isClientSide() || !(player instanceof ServerPlayer)) return InteractionResult.SUCCESS;
        if (isMall(ctx.getItemInHand())) {
            if (com.baraaoi.reallife.economy.BizBuild.buildMall((ServerPlayer) player, ctx.getClickedPos(), ctx.getHorizontalDirection())) {
                ctx.getItemInHand().shrink(1);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        BizType type = typeOf(ctx.getItemInHand());
        if (type != null && isTurnkey(ctx.getItemInHand())) {
            if (com.baraaoi.reallife.economy.BizBuild.buildTurnkey((ServerPlayer) player, ctx.getClickedPos(), ctx.getHorizontalDirection(), type)) {
                ctx.getItemInHand().shrink(1);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        if (type == null) {
            player.sendSystemMessage(Component.literal("§cРазрешение испорчено."));
            return InteractionResult.FAIL;
        }
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos pos = world.getBlockState(ctx.getClickedPos()).canBeReplaced() ? ctx.getClickedPos() : ctx.getClickedPos().relative(ctx.getClickedFace());
        if (LegalBiz.place((ServerPlayer) player, pos, facing, type)) {
            ctx.getItemInHand().shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        if (isMall(stack)) {
            tooltip.accept(Component.literal("§7ПКМ по земле — бригада построит перед вами зал 71×45"));
            tooltip.accept(Component.literal("§7с 8 размеченными местами под заведения."));
            tooltip.accept(Component.literal("§aВ ТЦ прибыль +50%, безопасный отмыв 25%."));
            tooltip.accept(Component.literal("§8Нужна ваша или ничья земля без чужих построек."));
            return;
        }
        BizType t = typeOf(stack);
        if (t != null && isTurnkey(stack)) {
            tooltip.accept(Component.literal("§7ПКМ по земле — бригада построит здание 13×13 перед вами"));
            tooltip.accept(Component.literal("§7и расставит оборудование на §e" + t.maxWorkers + "§7 работников."));
            tooltip.accept(Component.literal("§7Выручка: §a" + t.revenue() + "₽§7/день на работника"));
            tooltip.accept(Component.literal("§8Внутри ТЦ не работает — там места уже размечены."));
            return;
        }
        if (t != null) {
            tooltip.accept(Component.literal("§7Выручка: §a" + t.revenue() + "₽§7/день на работника, до §e" + t.maxWorkers + "§7 работников"));
            tooltip.accept(Component.literal("§7Зарплата работника: §c" + t.wage() + "₽§7/день"));
            tooltip.accept(Component.literal("§7Комплект на работника: §e" + LegalBiz.kitText(t)));
        }
        tooltip.accept(Component.literal("§7ПКМ по блоку на своей земле — там встанет главная касса."));
        tooltip.accept(Component.literal("§dВ своём ТЦ — ПКМ по центру площадки своего вида (+50%)."));
        tooltip.accept(Component.literal("§7Здание строишь сам; оборудование ставь в ±8 блоках от кассы."));
        tooltip.accept(Component.literal("§8Сломаешь главную кассу — заведение закроется, лицензия вернётся."));
    }
}
