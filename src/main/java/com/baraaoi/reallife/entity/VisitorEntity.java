package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.event.Visitors;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Случайный гость (налоговая, сантехник, цыганка, гопник...). Вся логика — в {@link Visitors};
 * сущность только ходит, бьёт и уводит лошадей. После перезапуска сервера гости исчезают сами.
 */
public class VisitorEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(VisitorEntity.class, EntityDataSerializers.INT);

    public static final int IDLE = 0, FOLLOW = 1, FLEE = 2, ATTACK = 3, STAY = 4, GOTO = 5;

    
    public UUID targetId;
    public int mode = FOLLOW;
    public double followDist = 2.5D;
    
    public BlockPos gotoPos;
    private int attackCooldown;

    public VisitorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, 0);
    }

    public VisitorKind getKind() {
        return VisitorKind.byId(this.entityData.get(KIND));
    }

    public void setKind(VisitorKind k) {
        this.entityData.set(KIND, k.ordinal());
    }

    
    public Player getTargetPlayer() {
        return targetId == null ? null : this.level().getPlayerByUUID(targetId);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new VisitorGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (!Visitors.isManaged(this)) {
            this.discard();
            return;
        }
        if (attackCooldown > 0) attackCooldown--;
        Player p = getTargetPlayer();
        // конокрад: гонит лошадь прочь от хозяина
        if (this.getVehicle() instanceof AbstractHorse && p != null) {
            double dx = this.getX() - p.getX();
            double dz = this.getZ() - p.getZ();
            if (dx * dx + dz * dz < 0.01) dx = 1;
            float yaw = (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
            yaw += (float) Math.sin(this.tickCount * 0.05) * 25.0F; // петляет
            this.rotationYaw = yaw;
            this.prevRotationYaw = yaw;
            this.rotationYawHead = yaw;
            this.renderYawOffset = yaw;
            this.moveForward = 1.0F;
            this.moveStrafing = 0.0F;
            AbstractHorse horse = (AbstractHorse) this.getVehicle();
            if (horse.collidedHorizontally && horse.onGround()) {
                horse.setDeltaMovement(horse.getDeltaMovement().offset(0, 0.45D, 0)); // перепрыгнуть препятствие
            }
            return;
        }
        if (mode == ATTACK && p != null && p.isAlive() && attackCooldown == 0 && this.distanceToSqr(p) < 2.6D * 2.6D) {
            attackCooldown = 20;
            this.swing(InteractionHand.MAIN_HAND);
            if (p.getHealth() > 5.0F) p.hurtServer((net.minecraft.server.level.ServerLevel) p.level(), this.damageSources().mobAttack(this), 2.0F);
        }
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        if (!this.level().isClientSide() && Visitors.onHurt(this, source)) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("VisitorKind", this.entityData.get(KIND));
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput nbt) {
        super.readAdditionalSaveData(nbt);
        this.entityData.set(KIND, nbt.getIntOr("VisitorKind", 0));
    }

    /** Ходьба по режиму: за игроком, прочь, к точке, на месте. */
    static class VisitorGoal extends Goal {
        private final VisitorEntity mob;
        private int repath;

        VisitorGoal(VisitorEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mob.mode != IDLE && !mob.isPassenger();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            Player p = mob.getTargetPlayer();
            if (p == null) return;
            if (repath > 0) repath--;
            switch (mob.mode) {
                case FOLLOW:
                case ATTACK: {
                    mob.getLookControl().setLookAt(p, 10.0F, (float) mob.getMaxHeadXRot());
                    double d = mob.mode == ATTACK ? 1.4D : mob.followDist;
                    if (mob.distanceToSqr(p) > d * d) {
                        if (repath == 0) {
                            mob.getNavigation().moveTo(p, mob.mode == ATTACK ? 1.25D : 1.0D);
                            repath = 10;
                        }
                    } else {
                        mob.getNavigation().stop();
                    }
                    break;
                }
                case FLEE: {
                    if (mob.getNavigation().isDone() || repath == 0) {
                        Vec3 v = DefaultRandomPos.getPosAway(mob, 16, 7, p.position());
                        if (v != null) mob.getNavigation().moveTo(v.x, v.y, v.z, 1.25D);
                        repath = 40;
                    }
                    break;
                }
                case GOTO: {
                    if (mob.gotoPos != null && repath == 0) {
                        mob.getNavigation().moveTo(mob.gotoPos.getX() + 0.5, mob.gotoPos.getY(), mob.gotoPos.getZ() + 0.5, 1.0D);
                        repath = 20;
                    }
                    break;
                }
                default: {
                    mob.getNavigation().stop();
                    mob.getLookControl().setLookAt(p, 10.0F, (float) mob.getMaxHeadXRot());
                }
            }
        }
    }
}
