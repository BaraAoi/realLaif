package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * Кассир банка: ПКМ — окно банка. Удар — тревожная кнопка (через 30 сек СОБР за всеми, кто был в зале).
 * Убийство — из кассы высыпается крупная сумма.
 */
public class CashierEntity extends PathfinderMob {
    public CashierEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("§6Кассир «Реал Банка»"));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        if (this.level().isClientSide()) return super.hurtServer(level, source, amount);
        Entity e = source.getEntity();
        if (e instanceof ServerPlayer) {
            DebtManager.cashierHit(this, (ServerPlayer) e);
            return super.hurtServer(level, source, amount);
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, amount);
        return false;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!this.level().isClientSide()) DebtManager.cashierKilled(this);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source, boolean recentlyHitIn) {
        super.dropCustomDeathLoot(level, source, recentlyHitIn);
        int min = Config.CASHIER_LOOT_MIN.get(), max = Math.max(min, Config.CASHIER_LOOT_MAX.get());
        long amount = (min + this.random.nextInt(max - min + 1)) / 100L * 100L;
        for (int note : BankService.NOTES) {
            long n = amount / note;
            amount -= n * note;
            while (n > 0) {
                int c = (int) Math.min(64, n);
                n -= c;
                this.spawnAtLocation(level, new ItemStack(BankService.noteItem(note), c));
            }
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
