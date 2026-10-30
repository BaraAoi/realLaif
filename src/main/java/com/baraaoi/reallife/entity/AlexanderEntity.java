package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

/**
 * Александр — сотрудник службы взыскания. Заставляет копать: каждый выкопанный блок идёт в счёт долга.
 * Убежишь — догонит и вернёт. Перестанешь копать — получишь.
 */
public class AlexanderEntity extends DebtNpcEntity {
    private int talk;
    private int idleStrikes;

    public AlexanderEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new FollowTargetGoal(this, 1.1D, 4.0D));
    }

    @Override
    public String npcName() {
        return "§6Александр (взыскание)";
    }

    @Override
    public boolean isStillNeeded(Account a) {
        return DebtManager.bankStage(a) == 2;
    }

    @Override
    protected void serverSecond(ServerPlayer p, Account a) {
        talk++;
        long now = this.level().getGameTime();
        double dist = this.distanceToSqr(p);
        if (dist > 24 * 24) {
            p.teleportTo(this.getX(), this.getY(), this.getZ());
            p.hurtServer((net.minecraft.server.level.ServerLevel) p.level(), this.damageSources().mobAttack(this), 2.0F);
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 1));
            say(p, "§cКуда собрался?! Работать! Осталось блоков: " + a.laborBlocksLeft);
            return;
        }
        if (a.lastLaborTick > 0 && now - a.lastLaborTick > 1200) {
            a.lastLaborTick = now;
            EconomyData.get(this.level().getServer()).setDirty();
            p.hurtServer((net.minecraft.server.level.ServerLevel) p.level(), this.damageSources().mobAttack(this), 3.0F);
            if (++idleStrikes >= 5) {
                a.wanted = true;
                say(p, "§4Всё, моё терпение кончилось. Я вызываю полицию.");
            } else {
                say(p, "§cЯ не понял, почему стоим? Копай давай! Ещё " + a.laborBlocksLeft + " блоков. §7(предупреждение "
                        + idleStrikes + "/5)");
            }
            return;
        }
        if (talk % 40 == 0) {
            String[] lines = {
                    "Копай. Камень, земля, руда — всё в счёт долга.",
                    "Руду не прячь, она тоже банку.",
                    "Жора тебя по-хорошему просил. Теперь по-моему.",
                    "Не нравится? Могу полицию вызвать. Хочешь?",
            };
            say(p, lines[(talk / 40) % lines.length] + " §7(осталось " + a.laborBlocksLeft + ")");
        }
    }

    @Override
    protected void onDismissed(ServerPlayer p) {
        say(p, "Ну вот. Можешь же, когда хочешь. Свободен. Кирку оставь себе — пригодится.");
    }
}
