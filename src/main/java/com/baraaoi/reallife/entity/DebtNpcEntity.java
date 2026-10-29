package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Базовый NPC, пришедший к конкретному должнику.
 */
public abstract class DebtNpcEntity extends CreatureEntity {
    @Nullable
    protected UUID targetId;

    protected DebtNpcEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
        this.enablePersistence();
    }

    public void setTarget(PlayerEntity player) {
        this.targetId = player.getUniqueID();
    }

    @Nullable
    public UUID getTargetId() {
        return targetId;
    }

    @Nullable
    public PlayerEntity getTargetPlayer() {
        return targetId == null ? null : world.getPlayerByUuid(targetId);
    }

    /** Имя над головой и в чате. */
    public abstract String npcName();

    /** Нужен ли ещё этот NPC при данном состоянии счёта. */
    public abstract boolean isStillNeeded(Account a);

    /** Серверная логика раз в секунду. */
    protected void serverSecond(ServerPlayerEntity target, Account a) {}

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 8.0F));
    }

    public void say(PlayerEntity p, String text) {
        p.sendStatusMessage(new StringTextComponent("§7<" + npcName() + "§7> §f" + text), false);
    }

    @Override
    public void tick() {
        super.tick();
        if (world.isRemote || this.ticksExisted % 20 != 0) return;
        MinecraftServer server = world.getServer();
        if (server == null) return;
        PlayerEntity t = getTargetPlayer();
        if (!(t instanceof ServerPlayerEntity) || !t.isAlive()) {
            if (t == null) this.remove();
            return;
        }
        if (this.getDistanceSq(t) > 128 * 128) {
            this.remove();
            return;
        }
        Account a = EconomyData.get(server).get(t);
        if (!isStillNeeded(a)) {
            onDismissed((ServerPlayerEntity) t);
            this.remove();
            return;
        }
        // отстал — «подъехал на машине»
        if (this.getDistanceSq(t) > 40 * 40) {
            DebtManager.teleportNear(this, t, 6);
        }
        serverSecond((ServerPlayerEntity) t, a);
    }

    protected void onDismissed(ServerPlayerEntity target) {}

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void writeAdditional(CompoundNBT nbt) {
        super.writeAdditional(nbt);
        if (targetId != null) nbt.putUniqueId("DebtTarget", targetId);
    }

    @Override
    public void readAdditional(CompoundNBT nbt) {
        super.readAdditional(nbt);
        if (nbt.hasUniqueId("DebtTarget")) targetId = nbt.getUniqueId("DebtTarget");
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
            this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean shouldExecute() {
            PlayerEntity p = mob.getTargetPlayer();
            return p != null && p.isAlive() && mob.getAttackTarget() == null;
        }

        @Override
        public void tick() {
            PlayerEntity p = mob.getTargetPlayer();
            if (p == null) return;
            mob.getLookController().setLookPositionWithEntity(p, 10.0F, (float) mob.getVerticalFaceSpeed());
            if (mob.getDistanceSq(p) > minDist * minDist) {
                mob.getNavigator().tryMoveToEntityLiving(p, speed);
            } else {
                mob.getNavigator().clearPath();
            }
        }
    }

    /** Атакует только своего должника. */
    public static class HuntTargetGoal extends Goal {
        private final DebtNpcEntity mob;

        public HuntTargetGoal(DebtNpcEntity mob) {
            this.mob = mob;
            this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean shouldExecute() {
            PlayerEntity p = mob.getTargetPlayer();
            return p != null && p.isAlive() && !p.isCreative() && !p.isSpectator() && mob.wantsToFight();
        }

        @Override
        public void startExecuting() {
            mob.setAttackTarget(mob.getTargetPlayer());
        }

        @Override
        public boolean shouldContinueExecuting() {
            return shouldExecute();
        }

        @Override
        public void tick() {
            if (mob.getAttackTarget() == null) mob.setAttackTarget(mob.getTargetPlayer());
        }

        @Override
        public void resetTask() {
            mob.setAttackTarget(null);
        }
    }

    /** Хочет ли NPC сейчас драться. */
    public boolean wantsToFight() {
        return false;
    }
}
