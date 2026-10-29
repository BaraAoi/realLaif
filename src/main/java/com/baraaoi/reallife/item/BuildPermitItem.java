package com.baraaoi.reallife.item;

import com.baraaoi.reallife.structure.Buildings;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Разрешение на строительство банка / конторы МФО. ПКМ по земле — здание построится перед игроком.
 */
public class BuildPermitItem extends Item {
    public enum Kind { BANK, MFO }

    private final Kind kind;

    public BuildPermitItem(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext ctx) {
        World world = ctx.getWorld();
        PlayerEntity player = ctx.getPlayer();
        if (world.isRemote || player == null) return ActionResultType.SUCCESS;
        Direction facing = ctx.getPlacementHorizontalFacing();
        BlockPos origin = ctx.getPos().up().offset(facing, 2);
        if (kind == Kind.BANK) {
            Buildings.buildBank(world, origin, facing);
            player.sendStatusMessage(new StringTextComponent("§6[Реал Банк] §fОтделение банка открыто! Добро пожаловать."), false);
        } else {
            Buildings.buildMfo(world, origin, facing);
            player.sendStatusMessage(new StringTextComponent("§2[Шайкин и Ко] §fКонтора открыта. Деньги — сразу, вопросы — потом."), false);
        }
        if (!player.abilities.isCreativeMode) ctx.getItem().shrink(1);
        return ActionResultType.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        if (kind == Kind.BANK) {
            tooltip.add(new StringTextComponent("§7ПКМ по земле — построить здание банка"));
            tooltip.add(new StringTextComponent("§7(13×10 блоков перед вами)"));
        } else {
            tooltip.add(new StringTextComponent("§7ПКМ по земле — построить контору МФО"));
            tooltip.add(new StringTextComponent("§7(8×7 блоков перед вами)"));
        }
        tooltip.add(new StringTextComponent("§cБлоки на месте стройки будут снесены!"));
    }
}
