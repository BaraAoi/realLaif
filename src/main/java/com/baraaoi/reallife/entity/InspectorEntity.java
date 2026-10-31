package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.Finance;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Инспектор отдела по борьбе с самостроем. Ходит за нарушителем, выписывает штраф,
 * ПКМ — оформить покупку участка.
 */
public class InspectorEntity extends DebtNpcEntity {
    private int talk;

    public InspectorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WRITABLE_BOOK));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
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
    protected void serverSecond(ServerPlayer p, Account a) {
        if (++talk % 40 == 1) {
            long left = Math.max(0, a.landDeadline - this.level().getGameTime()) / 20;
            say(p, String.format("§fГражданин, постройка без документов! Снесите её или купите участок за §e%s§f "
                    + "(ПКМ по мне). Иначе через §c%d:%02d§f — снос всего участка до коренной породы.",
                    com.baraaoi.reallife.event.DebtManager.landQuote(p.level().getServer(), p.getUUID(), a.landChunk), left / 60, left % 60));
        }
    }
}
