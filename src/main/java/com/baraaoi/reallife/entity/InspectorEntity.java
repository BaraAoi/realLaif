package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.Finance;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;

/**
 * Инспектор отдела по борьбе с самостроем. Ходит за нарушителем, выписывает штраф,
 * ПКМ — оформить покупку участка.
 */
public class InspectorEntity extends DebtNpcEntity {
    private int talk;

    public InspectorEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
        this.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.WRITABLE_BOOK));
        this.setDropChance(EquipmentSlotType.MAINHAND, 0.0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new FollowTargetGoal(this, 1.0D, 3.0D));
    }

    @Override
    public String npcName() {
        return "§3Инспектор по самострою";
    }

    @Override
    public boolean isStillNeeded(Account a) {
        return a.landCase && a.captivity == Account.FREE;
    }

    @Override
    protected int moneyDrop() {
        return 0;
    }

    @Override
    protected void serverSecond(ServerPlayerEntity p, Account a) {
        if (++talk % 40 == 1) {
            long left = Math.max(0, a.landDeadline - world.getGameTime()) / 20;
            say(p, String.format("§fГражданин, постройка без документов! Снесите её или купите участок за §e%s§f "
                    + "(ПКМ по мне). Иначе через §c%d:%02d§f — снос всего участка до коренной породы.",
                    com.baraaoi.reallife.event.DebtManager.landQuote(p.getServer(), p.getUniqueID(), a.landChunk), left / 60, left % 60));
        }
    }
}
