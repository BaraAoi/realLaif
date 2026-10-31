package com.baraaoi.reallife.item;

import com.baraaoi.reallife.util.Nbt;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.HouseType;
import com.baraaoi.reallife.structure.Buildings;
import com.baraaoi.reallife.structure.Blueprint;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
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
import java.util.UUID;

/**
 * Ордер на строительство дома по ипотеке. Банк строит дом бесплатно — платить потом.
 */
public class HouseOrderItem extends Item {
    public HouseOrderItem(Properties props) {
        super(props);
    }

    public static ItemStack create(Item item, UUID owner, int house, long orderId) {
        ItemStack s = new ItemStack(item);
        CompoundTag t = new CompoundTag();
        Nbt.putUUID(t, "owner", owner);
        t.putInt("house", house);
        t.putLong("order", orderId);
        Nbt.setItemTag(s, t);
        return s;
    }

    /** v1.10: ордер на дом, купленный сразу (paid — без залога банка). */
    public static ItemStack create(Item item, UUID owner, int house, long orderId, boolean paid) {
        ItemStack s = create(item, owner, house, orderId);
        if (paid) com.baraaoi.reallife.util.Nbt.editItem(s, _t -> _t.putBoolean("paid", true));
        return s;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (world.isClientSide() || player == null) return InteractionResult.SUCCESS;
        ItemStack stack = ctx.getItemInHand();
        CompoundTag t = com.baraaoi.reallife.util.Nbt.itemTag(stack);
        if (t == null || !Nbt.hasUUID(t, "owner")) {
            player.sendSystemMessage(Component.literal("§cОрдер недействителен."));
            return InteractionResult.FAIL;
        }
        if (!Nbt.getUUID(t, "owner").equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cЭто чужой ордер! Банк строит только владельцу ипотеки."));
            return InteractionResult.FAIL;
        }
        if (world.dimension() != Level.OVERWORLD) {
            player.sendSystemMessage(Component.literal("§cБанк строит только в обычном мире."));
            return InteractionResult.FAIL;
        }
        EconomyData data = EconomyData.get(world.getServer());
        Account a = data.get(player);
        HouseType type = HouseType.byId(t.getIntOr("house", 0));
        if (type == null || a.pendingHouse != type.ordinal() || a.houseOrderId != t.getLongOr("order", 0L)) {
            player.sendSystemMessage(Component.literal("§cОрдер аннулирован банком."));
            stack.shrink(1);
            return InteractionResult.FAIL;
        }
        Direction facing = ctx.getHorizontalDirection();
        BlockPos origin = ctx.getClickedPos().above().relative(facing, 2);
        String problem = Buildings.checkHouseSite(world, origin, facing, type, data, player.getUUID());
        if (problem != null) {
            player.sendSystemMessage(Component.literal("§6[Реал Банк] §cБригада отказалась строить: §f" + problem));
            return InteractionResult.FAIL;
        }
        Blueprint bp = Buildings.buildHouse(world, origin, facing, type);
        a.hasHouse = true;
        a.houseType = type.ordinal();
        a.houseMin = bp.min();
        a.houseMax = bp.max();
        a.houseBuilt = true;
        a.pendingHouse = -1;
        int claimed = Buildings.claimArea(data, player.getUUID(), bp.min().offset(-8, 0, -8), bp.max().offset(8, 0, 8), true);
        if (a.landCase && player.getUUID().equals(data.owner(a.landChunk))) a.landCase = false;
        data.setDirty();
        stack.shrink(1);
        if (claimed > 0) player.sendSystemMessage(Component.literal("§3[Администрация] §fЗемля под домом и вокруг оформлена на вас: §e"
                + claimed + "§f участк(а/ов). Стройте спокойно."));
        player.sendSystemMessage(Component.literal("§6[Реал Банк] §fБригада построила вам: §e" + type.title
                + (a.hasMortgage() ? "§f. Не забывайте платить по ипотеке — иначе дом изымут." : "§f. Дом полностью ваш — с новосельем!")));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        CompoundTag t = com.baraaoi.reallife.util.Nbt.itemTag(stack);
        if (t != null) {
            HouseType type = HouseType.byId(t.getIntOr("house", 0));
            if (type != null) tooltip.accept(Component.literal("§eДом: " + type.title));
        }
        tooltip.accept(Component.literal("§7ПКМ по земле — бригада банка построит дом перед вами."));
        tooltip.accept(Component.literal("§7Нужна ровная площадка без чужих построек."));
        if (t != null && t.getBooleanOr("paid", false)) {
            tooltip.accept(Component.literal("§aОплачен полностью — дом ваш, не в залоге."));
        } else {
            tooltip.accept(Component.literal("§8Ордер действует, даже если ипотека уже погашена."));
            tooltip.accept(Component.literal("§8Залог банка. При неуплате дом изымается."));
        }
    }
}
