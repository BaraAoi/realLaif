package com.baraaoi.reallife.item;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.HouseType;
import com.baraaoi.reallife.structure.Buildings;
import com.baraaoi.reallife.structure.Blueprint;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
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
        CompoundNBT t = s.getOrCreateTag();
        t.putUniqueId("owner", owner);
        t.putInt("house", house);
        t.putLong("order", orderId);
        return s;
    }

    /** v1.10: ордер на дом, купленный сразу (paid — без залога банка). */
    public static ItemStack create(Item item, UUID owner, int house, long orderId, boolean paid) {
        ItemStack s = create(item, owner, house, orderId);
        if (paid) s.getOrCreateTag().putBoolean("paid", true);
        return s;
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext ctx) {
        World world = ctx.getWorld();
        PlayerEntity player = ctx.getPlayer();
        if (world.isRemote || player == null) return ActionResultType.SUCCESS;
        ItemStack stack = ctx.getItem();
        CompoundNBT t = stack.getTag();
        if (t == null || !t.hasUniqueId("owner")) {
            player.sendStatusMessage(new StringTextComponent("§cОрдер недействителен."), false);
            return ActionResultType.FAIL;
        }
        if (!t.getUniqueId("owner").equals(player.getUniqueID())) {
            player.sendStatusMessage(new StringTextComponent("§cЭто чужой ордер! Банк строит только владельцу ипотеки."), false);
            return ActionResultType.FAIL;
        }
        if (world.getDimensionKey() != World.OVERWORLD) {
            player.sendStatusMessage(new StringTextComponent("§cБанк строит только в обычном мире."), false);
            return ActionResultType.FAIL;
        }
        EconomyData data = EconomyData.get(world.getServer());
        Account a = data.get(player);
        HouseType type = HouseType.byId(t.getInt("house"));
        if (type == null || a.pendingHouse != type.ordinal() || a.houseOrderId != t.getLong("order")) {
            player.sendStatusMessage(new StringTextComponent("§cОрдер аннулирован банком."), false);
            stack.shrink(1);
            return ActionResultType.FAIL;
        }
        Direction facing = ctx.getPlacementHorizontalFacing();
        BlockPos origin = ctx.getPos().up().offset(facing, 2);
        String problem = Buildings.checkHouseSite(world, origin, facing, type, data, player.getUniqueID());
        if (problem != null) {
            player.sendStatusMessage(new StringTextComponent("§6[Реал Банк] §cБригада отказалась строить: §f" + problem), false);
            return ActionResultType.FAIL;
        }
        Blueprint bp = Buildings.buildHouse(world, origin, facing, type);
        a.hasHouse = true;
        a.houseType = type.ordinal();
        a.houseMin = bp.min();
        a.houseMax = bp.max();
        a.houseBuilt = true;
        a.pendingHouse = -1;
        int claimed = Buildings.claimArea(data, player.getUniqueID(), bp.min().add(-8, 0, -8), bp.max().add(8, 0, 8), true);
        if (a.landCase && player.getUniqueID().equals(data.owner(a.landChunk))) a.landCase = false;
        data.markDirty();
        stack.shrink(1);
        if (claimed > 0) player.sendStatusMessage(new StringTextComponent("§3[Администрация] §fЗемля под домом и вокруг оформлена на вас: §e"
                + claimed + "§f участк(а/ов). Стройте спокойно."), false);
        player.sendStatusMessage(new StringTextComponent("§6[Реал Банк] §fБригада построила вам: §e" + type.title
                + (a.hasMortgage() ? "§f. Не забывайте платить по ипотеке — иначе дом изымут." : "§f. Дом полностью ваш — с новосельем!")), false);
        return ActionResultType.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        CompoundNBT t = stack.getTag();
        if (t != null) {
            HouseType type = HouseType.byId(t.getInt("house"));
            if (type != null) tooltip.add(new StringTextComponent("§eДом: " + type.title));
        }
        tooltip.add(new StringTextComponent("§7ПКМ по земле — бригада банка построит дом перед вами."));
        tooltip.add(new StringTextComponent("§7Нужна ровная площадка без чужих построек."));
        if (t != null && t.getBoolean("paid")) {
            tooltip.add(new StringTextComponent("§aОплачен полностью — дом ваш, не в залоге."));
        } else {
            tooltip.add(new StringTextComponent("§8Ордер действует, даже если ипотека уже погашена."));
            tooltip.add(new StringTextComponent("§8Залог банка. При неуплате дом изымается."));
        }
    }
}
