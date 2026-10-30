package com.baraaoi.reallife.item;

import com.baraaoi.reallife.economy.BankService;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

/** Смартфон: банк в кармане — счёт, снятие наличных, кредит, опт, бизнес, крипта. */
public class PhoneItem extends Item {
    public PhoneItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide()) {
            // ClientHooks не тянет клиентские классы в сигнатурах — на сервере сюда не заходим
            com.baraaoi.reallife.client.ClientHooks.openPhone();
        } else if (player instanceof ServerPlayer) {
            BankService.sync((ServerPlayer) player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Есть ли у игрока телефон (в любом слоте). */
    public static boolean has(Player p) {
        for (ItemStack st : p.getInventory().getNonEquipmentItems()) if (st.getItem() instanceof PhoneItem) return true;
        for (ItemStack st : java.util.List.of(p.getOffhandItem())) if (st.getItem() instanceof PhoneItem) return true;
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§7ПКМ — приложение «Реал Банк»:"));
        tooltip.accept(Component.literal("§7счёт, снять наличные, кредит, крипта,"));
        tooltip.accept(Component.literal("§7бизнес и оптовые закупки для заведений."));
    }
}
