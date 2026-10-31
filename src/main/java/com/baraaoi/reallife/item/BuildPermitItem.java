package com.baraaoi.reallife.item;

import com.baraaoi.reallife.structure.Buildings;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Разрешение на строительство банка / конторы МФО. ПКМ по земле — здание построится перед игроком.
 */
public class BuildPermitItem extends Item {
    public enum Kind { BANK, MFO, CASINO }

    private final Kind kind;

    public BuildPermitItem(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (world.isClientSide() || player == null) return InteractionResult.SUCCESS;
        Direction facing = ctx.getHorizontalDirection();
        BlockPos origin = ctx.getClickedPos().above().relative(facing, 2);
        if (kind == Kind.BANK) {
            Buildings.buildBank(world, origin, facing);
            player.sendSystemMessage(Component.literal("§6[Реал Банк] §fОтделение банка открыто! Добро пожаловать."));
        } else if (kind == Kind.CASINO) {
            Buildings.buildCasino(world, origin, facing, false);
            player.sendSystemMessage(Component.literal("§5[Казино] §fКазино «Три семёрки» открыто. Ставки сделаны!"));
        } else {
            Buildings.buildMfo(world, origin, facing);
            player.sendSystemMessage(Component.literal("§2[Шайкин и Ко] §fКонтора открыта. Деньги — сразу, вопросы — потом."));
        }
        if (!player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        if (kind == Kind.BANK) {
            tooltip.accept(Component.literal("§7ПКМ по земле — построить здание банка"));
            tooltip.accept(Component.literal("§7(19×12 блоков перед вами)"));
        } else if (kind == Kind.CASINO) {
            tooltip.accept(Component.literal("§7ПКМ по земле — построить казино «Три семёрки»"));
            tooltip.accept(Component.literal("§7(31×31 блок перед вами)"));
        } else {
            tooltip.accept(Component.literal("§7ПКМ по земле — построить контору МФО"));
            tooltip.accept(Component.literal("§7(8×7 блоков перед вами)"));
        }
        tooltip.accept(Component.literal("§cБлоки на месте стройки будут снесены!"));
    }
}
