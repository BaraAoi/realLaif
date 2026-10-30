package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Полицейский. Бьёт должника, пока тот не умрёт. После смерти — арест.
 */
public class PoliceEntity extends DebtNpcEntity {
    private int talk;

    public PoliceEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new PickupMoneyGoal(this, 1.3D));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(3, new FollowTargetGoal(this, 1.1D, 2.0D));
        this.targetSelector.addGoal(1, new HuntTargetGoal(this));
    }

    @Override
    public boolean wantsToFight() {
        return true;
    }

    @Override
    public boolean likesMoney() {
        return true;
    }

    @Override
    public void onMoneyPicked(double value) {
        DebtManager.policeBribe(this, value);
    }

    @Override
    public String npcName() {
        return "§9Полиция";
    }

    @Override
    public boolean isStillNeeded(Account a) {
        return DebtManager.bankStage(a) == 3;
    }

    @Override
    protected void serverSecond(ServerPlayer p, Account a) {
        if (++talk % 15 == 1 && this.random.nextInt(3) == 0) {
            String[] lines = {"Стоять! Полиция!", "Руки за голову!", "Сопротивление бесполезно!",
                    "Вы задержаны за злостную неуплату!", "Лицом в землю!"};
            say(p, "§c" + lines[this.random.nextInt(lines.length)]);
        }
    }
}
