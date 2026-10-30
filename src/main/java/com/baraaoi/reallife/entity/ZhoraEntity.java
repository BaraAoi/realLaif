package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Жора — вежливый сотрудник банка. Просит по-хорошему.
 * ПКМ по Жоре — оплатить просрочку ценностями из инвентаря.
 */
public class ZhoraEntity extends DebtNpcEntity {
    private static final String[] LINES = {
            "Здравствуйте! Я Жора из «Реал Банка». У вас просрочка §c%s§f. Пожалуйста, пополните счёт.",
            "Извините, что беспокою... Может, всё-таки внесёте §c%s§f? Можно прямо мне — нажмите на меня.",
            "Я понимаю, жизнь тяжёлая. Но банку нужно §c%s§f. Давайте решим по-хорошему?",
            "Мне правда неудобно, но если не заплатите §c%s§f, к вам придёт Александр. Он не такой добрый.",
            "Хорошая погода, правда? Кстати, о погоде... ваш долг §c%s§f.",
    };
    private int talk;

    public ZhoraEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new FollowTargetGoal(this, 1.0D, 3.0D));
    }

    @Override
    public String npcName() {
        return "§eЖора (Реал Банк)";
    }

    @Override
    public boolean isStillNeeded(Account a) {
        return DebtManager.bankStage(a) == 1;
    }

    @Override
    protected void serverSecond(ServerPlayer p, Account a) {
        if (++talk % 25 == 1) {
            say(p, String.format(LINES[(talk / 25) % LINES.length], Finance.money(a.arrears)));
        }
    }

    @Override
    protected void onDismissed(ServerPlayer p) {
        say(p, "Спасибо огромное! Хорошего вам дня!");
    }
}
