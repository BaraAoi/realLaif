package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

/**
 * Кассир банка: ПКМ — окно банка. Удар — тревожная кнопка (через 30 сек СОБР за всеми, кто был в зале).
 * Убийство — из кассы высыпается крупная сумма.
 */
public class CashierEntity extends CreatureEntity {
    public CashierEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
        this.enablePersistence();
        this.setCustomName(new StringTextComponent("§6Кассир «Реал Банка»"));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(2, new LookRandomlyGoal(this));
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote) return super.attackEntityFrom(source, amount);
        Entity e = source.getTrueSource();
        if (e instanceof ServerPlayerEntity) {
            DebtManager.cashierHit(this, (ServerPlayerEntity) e);
            return super.attackEntityFrom(source, amount);
        }
        if (source == DamageSource.OUT_OF_WORLD) return super.attackEntityFrom(source, amount);
        return false;
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (!world.isRemote) DebtManager.cashierKilled(this);
    }

    @Override
    protected void dropSpecialItems(DamageSource source, int looting, boolean recentlyHitIn) {
        super.dropSpecialItems(source, looting, recentlyHitIn);
        int min = Config.CASHIER_LOOT_MIN.get(), max = Math.max(min, Config.CASHIER_LOOT_MAX.get());
        long amount = (min + this.rand.nextInt(max - min + 1)) / 100L * 100L;
        for (int note : BankService.NOTES) {
            long n = amount / note;
            amount -= n * note;
            while (n > 0) {
                int c = (int) Math.min(64, n);
                n -= c;
                this.entityDropItem(new ItemStack(BankService.noteItem(note), c));
            }
        }
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return false;
    }
}
