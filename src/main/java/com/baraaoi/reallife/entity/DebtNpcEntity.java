package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.util.Nbt;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import com.baraaoi.reallife.economy.ItemValues;
import com.baraaoi.reallife.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Базовый NPC, пришедший к конкретному должнику.
 */
public abstract class DebtNpcEntity extends PathfinderMob {
    
    protected UUID targetId;

    protected DebtNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public void setTarget(Player player) {
        this.targetId = player.getUUID();
    }

    
    public UUID getTargetId() {
        return targetId;
    }

    
    public Player getTargetPlayer() {
        return targetId == null ? null : this.level().getPlayerByUUID(targetId);
    }

    /** Имя над головой и в чате. */
    public abstract String npcName();

    /** Нужен ли ещё этот NPC при данном состоянии счёта. */
    public abstract boolean isStillNeeded(Account a);

    /** Серверная логика раз в секунду. */
    protected void serverSecond(ServerPlayer target, Account a) {}

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    public void say(Player p, String text) {
        p.sendSystemMessage(Component.literal("§7<" + npcName() + "§7> §f" + text));
    }

    /** Сколько десяток (купюр по 10₽) может выпасть при смерти. */
    protected int moneyDrop() {
        return 2;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source, boolean recentlyHitIn) {
        super.dropCustomDeathLoot(level, source, recentlyHitIn);
        int max = moneyDrop();
        if (max > 0) {
            int n = 1 + this.random.nextInt(max);
            this.spawnAtLocation(level, new ItemStack(ModItems.BANKNOTE_10.get(), n));
        }
    }

    /** Бегает ли NPC за деньгами на земле (как пиглин за золотом). */
    public boolean likesMoney() {
        return false;
    }

    /** NPC подобрал деньги на сумму value. */
    public void onMoneyPicked(double value) {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || this.tickCount % 20 != 0) return;
        // постоянная Сила I — дубинкой бьют больно
        MobEffectInstance str = this.getEffect(MobEffects.STRENGTH);
        if (str == null || str.getDuration() < 100) {
            this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600, 0, false, false));
        }
        MinecraftServer server = this.level().getServer();
        if (server == null) return;
        Player t = getTargetPlayer();
        if (!(t instanceof ServerPlayer) || !t.isAlive()) {
            if (t == null) this.discard();
            return;
        }
        if (this.distanceToSqr(t) > 128 * 128) {
            this.discard();
            return;
        }
        Account a = EconomyData.get(server).get(t);
        if (!isStillNeeded(a)) {
            onDismissed((ServerPlayer) t);
            this.discard();
            return;
        }
        // отстал — «подъехал на машине»
        if (this.distanceToSqr(t) > 40 * 40) {
            DebtManager.teleportNear(this, t, 6);
        }
        serverSecond((ServerPlayer) t, a);
    }

    protected void onDismissed(ServerPlayer target) {}

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput nbt) {
        super.addAdditionalSaveData(nbt);
        if (targetId != null) Nbt.putUUID(nbt, "DebtTarget", targetId);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput nbt) {
        super.readAdditionalSaveData(nbt);
        if (Nbt.hasUUID(nbt, "DebtTarget")) targetId = Nbt.getUUID(nbt, "DebtTarget");
    }

    /** Ходит за должником на дистанции. */
    public static class FollowTargetGoal extends Goal {
        private final DebtNpcEntity mob;
        private final double speed;
        private final double minDist;

        public FollowTargetGoal(DebtNpcEntity mob, double speed, double minDist) {
            this.mob = mob;
            this.speed = speed;
            this.minDist = minDist;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player p = mob.getTargetPlayer();
            return p != null && p.isAlive() && mob.getTarget() == null;
        }

        @Override
        public void tick() {
            Player p = mob.getTargetPlayer();
            if (p == null) return;
            mob.getLookControl().setLookAt(p, 10.0F, (float) mob.getMaxHeadXRot());
            if (mob.distanceToSqr(p) > minDist * minDist) {
                mob.getNavigation().moveTo(p, speed);
            } else {
                mob.getNavigation().stop();
            }
        }
    }

    /** Атакует только своего должника. */
    public static class HuntTargetGoal extends Goal {
        private final DebtNpcEntity mob;

        public HuntTargetGoal(DebtNpcEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player p = mob.getTargetPlayer();
            return p != null && p.isAlive() && !p.isCreative() && !p.isSpectator() && mob.wantsToFight();
        }

        @Override
        public void start() {
            mob.setTarget(mob.getTargetPlayer());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (mob.getTarget() == null) mob.setTarget(mob.getTargetPlayer());
        }

        @Override
        public void stop() {
            mob.setTarget(null);
        }
    }

    /** Бежит к лежащим на земле деньгам и подбирает их. */
    public static class PickupMoneyGoal extends Goal {
        private final DebtNpcEntity mob;
        private final double speed;
        private ItemEntity item;

        public PickupMoneyGoal(DebtNpcEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private ItemEntity find() {
            List<ItemEntity> list = mob.level().getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(10.0D, 4.0D, 10.0D),
                    e -> e.isAlive() && ItemValues.isCash(e.getItem()));
            ItemEntity best = null;
            double bd = Double.MAX_VALUE;
            for (ItemEntity e : list) {
                double d = mob.distanceToSqr(e);
                if (d < bd) { bd = d; best = e; }
            }
            return best;
        }

        @Override
        public boolean canUse() {
            if (!mob.likesMoney() || mob.tickCount % 5 != 0) return false;
            item = find();
            return item != null;
        }

        @Override
        public boolean canContinueToUse() {
            return item != null && item.isAlive() && mob.distanceToSqr(item) < 14 * 14;
        }

        @Override
        public void start() {
            mob.setTarget(null);
            mob.getNavigation().moveTo(item, speed);
        }

        @Override
        public void tick() {
            if (item == null) return;
            mob.getLookControl().setLookAt(item, 30.0F, 30.0F);
            if (mob.distanceToSqr(item) < 2.0D * 2.0D) {
                double v = ItemValues.stackValue(item.getItem());
                item.discard();
                item = null;
                mob.onMoneyPicked(v);
            } else if (mob.tickCount % 10 == 0) {
                mob.getNavigation().moveTo(item, speed);
            }
        }

        @Override
        public void stop() {
            item = null;
            mob.getNavigation().stop();
        }
    }

    /** Хочет ли NPC сейчас драться. */
    public boolean wantsToFight() {
        return false;
    }
}
