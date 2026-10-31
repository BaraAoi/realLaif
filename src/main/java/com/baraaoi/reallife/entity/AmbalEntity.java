package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Бывшая охрана казино. Оставлена только для совместимости старых миров — сразу исчезает. */
public class AmbalEntity extends DebtNpcEntity {
    private int talk;

    public AmbalEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
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
        return false; // охрана казино больше никого не выгоняет
    }

    @Override
    protected int moneyDrop() {
        return 0;
    }

    @Override
    protected void serverSecond(ServerPlayer p, Account a) {
        if (this.distanceToSqr(p) < 2.8D * 2.8D) {
            this.discard();
            return;
        }
        if (++talk % 4 == 1) {
            String[] l = {"Гражданин, пройдёмте.", "Вам сегодня слишком везёт.", "Без резких движений.", "Казино всегда в плюсе. Всегда."};
            say(p, "§7" + l[this.random.nextInt(l.length)]);
        }
    }
}
