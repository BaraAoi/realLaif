package com.baraaoi.reallife.item;

import com.baraaoi.reallife.economy.BankService;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** Смартфон: банк в кармане — счёт, снятие наличных, кредит, опт, бизнес, крипта. */
public class PhoneItem extends Item {
    public PhoneItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            // ClientHooks не тянет клиентские классы в сигнатурах — на сервере сюда не заходим
            com.baraaoi.reallife.client.ClientHooks.openPhone();
        } else if (player instanceof ServerPlayerEntity) {
            BankService.sync((ServerPlayerEntity) player);
        }
        return ActionResult.resultSuccess(stack);
    }

    /** Есть ли у игрока телефон (в любом слоте). */
    public static boolean has(PlayerEntity p) {
        for (ItemStack st : p.inventory.mainInventory) if (st.getItem() instanceof PhoneItem) return true;
        for (ItemStack st : p.inventory.offHandInventory) if (st.getItem() instanceof PhoneItem) return true;
        return false;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("§7ПКМ — приложение «Реал Банк»:"));
        tooltip.add(new StringTextComponent("§7счёт, снять наличные, кредит, крипта,"));
        tooltip.add(new StringTextComponent("§7бизнес и оптовые закупки для заведений."));
    }
}
