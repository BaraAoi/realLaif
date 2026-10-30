package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;

/** Охрана казино: подходят к слишком везучему игроку и выносят его на улицу. */
public class AmbalEntity extends DebtNpcEntity {
    private int talk;

    public AmbalEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FollowTargetGoal(this, 1.35D, 1.2D));
    }

    @Override
    public String npcName() {
        return "§8Амбал";
    }

    @Override
    public boolean isStillNeeded(Account a) {
        return a.casinoKick;
    }

    @Override
    protected int moneyDrop() {
        return 0;
    }

    @Override
    protected void serverSecond(ServerPlayerEntity p, Account a) {
        if (this.getDistanceSq(p) < 2.8D * 2.8D) {
            DebtManager.throwOutOfCasino(p, EconomyData.get(p.getServer()), a);
            this.remove();
            return;
        }
        if (++talk % 4 == 1) {
            String[] l = {"Гражданин, пройдёмте.", "Вам сегодня слишком везёт.", "Без резких движений.", "Казино всегда в плюсе. Всегда."};
            say(p, "§7" + l[this.rand.nextInt(l.length)]);
        }
    }
}
