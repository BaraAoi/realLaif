package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;

/**
 * Полицейский. Бьёт должника, пока тот не умрёт. После смерти — арест.
 */
public class PoliceEntity extends DebtNpcEntity {
    private int talk;

    public PoliceEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
        this.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.STICK));
        this.setDropChance(EquipmentSlotType.MAINHAND, 0.0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(3, new FollowTargetGoal(this, 1.1D, 2.0D));
        this.targetSelector.addGoal(1, new HuntTargetGoal(this));
    }

    @Override
    public boolean wantsToFight() {
        return true;
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
    protected void serverSecond(ServerPlayerEntity p, Account a) {
        if (++talk % 15 == 1 && this.rand.nextInt(3) == 0) {
            String[] lines = {"Стоять! Полиция!", "Руки за голову!", "Сопротивление бесполезно!",
                    "Вы задержаны за злостную неуплату!", "Лицом в землю!"};
            say(p, "§c" + lines[this.rand.nextInt(lines.length)]);
        }
    }
}
