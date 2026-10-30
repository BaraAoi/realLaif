package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.event.Visitors;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Случайный гость (налоговая, сантехник, цыганка, гопник...). Вся логика — в {@link Visitors};
 * сущность только ходит, бьёт и уводит лошадей. После перезапуска сервера гости исчезают сами.
 */
public class VisitorEntity extends CreatureEntity {
    private static final DataParameter<Integer> KIND = EntityDataManager.createKey(VisitorEntity.class, DataSerializers.VARINT);

    public static final int IDLE = 0, FOLLOW = 1, FLEE = 2, ATTACK = 3, STAY = 4, GOTO = 5;

    @Nullable
    public UUID targetId;
    public int mode = FOLLOW;
    public double followDist = 2.5D;
    @Nullable
    public BlockPos gotoPos;
    private int attackCooldown;

    public VisitorEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(KIND, 0);
    }

    public VisitorKind getKind() {
        return VisitorKind.byId(this.dataManager.get(KIND));
    }

    public void setKind(VisitorKind k) {
        this.dataManager.set(KIND, k.ordinal());
    }

    @Nullable
    public PlayerEntity getTargetPlayer() {
        return targetId == null ? null : world.getPlayerByUuid(targetId);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new VisitorGoal(this));
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 8.0F));
    }

    @Override
    public void tick() {
        super.tick();
        if (world.isRemote) return;
        if (!Visitors.isManaged(this)) {
            this.remove();
            return;
        }
        if (attackCooldown > 0) attackCooldown--;
        PlayerEntity p = getTargetPlayer();
        // конокрад: гонит лошадь прочь от хозяина
        if (this.getRidingEntity() instanceof AbstractHorseEntity && p != null) {
            double dx = this.getPosX() - p.getPosX();
            double dz = this.getPosZ() - p.getPosZ();
            if (dx * dx + dz * dz < 0.01) dx = 1;
            float yaw = (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
            yaw += (float) Math.sin(this.ticksExisted * 0.05) * 25.0F; // петляет
            this.rotationYaw = yaw;
            this.prevRotationYaw = yaw;
            this.rotationYawHead = yaw;
            this.renderYawOffset = yaw;
            this.moveForward = 1.0F;
            this.moveStrafing = 0.0F;
            AbstractHorseEntity horse = (AbstractHorseEntity) this.getRidingEntity();
            if (horse.collidedHorizontally && horse.isOnGround()) {
                horse.setMotion(horse.getMotion().add(0, 0.45D, 0)); // перепрыгнуть препятствие
            }
            return;
        }
        if (mode == ATTACK && p != null && p.isAlive() && attackCooldown == 0 && this.getDistanceSq(p) < 2.6D * 2.6D) {
            attackCooldown = 20;
            this.swingArm(Hand.MAIN_HAND);
            if (p.getHealth() > 5.0F) p.attackEntityFrom(DamageSource.causeMobDamage(this), 2.0F);
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!world.isRemote && Visitors.onHurt(this, source)) return false;
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void writeAdditional(CompoundNBT nbt) {
        super.writeAdditional(nbt);
        nbt.putInt("VisitorKind", this.dataManager.get(KIND));
    }

    @Override
    public void readAdditional(CompoundNBT nbt) {
        super.readAdditional(nbt);
        this.dataManager.set(KIND, nbt.getInt("VisitorKind"));
    }

    /** Ходьба по режиму: за игроком, прочь, к точке, на месте. */
    static class VisitorGoal extends Goal {
        private final VisitorEntity mob;
        private int repath;

        VisitorGoal(VisitorEntity mob) {
            this.mob = mob;
            this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean shouldExecute() {
            return mob.mode != IDLE && !mob.isPassenger();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return shouldExecute();
        }

        @Override
        public void tick() {
            PlayerEntity p = mob.getTargetPlayer();
            if (p == null) return;
            if (repath > 0) repath--;
            switch (mob.mode) {
                case FOLLOW:
                case ATTACK: {
                    mob.getLookController().setLookPositionWithEntity(p, 10.0F, (float) mob.getVerticalFaceSpeed());
                    double d = mob.mode == ATTACK ? 1.4D : mob.followDist;
                    if (mob.getDistanceSq(p) > d * d) {
                        if (repath == 0) {
                            mob.getNavigator().tryMoveToEntityLiving(p, mob.mode == ATTACK ? 1.25D : 1.0D);
                            repath = 10;
                        }
                    } else {
                        mob.getNavigator().clearPath();
                    }
                    break;
                }
                case FLEE: {
                    if (mob.getNavigator().noPath() || repath == 0) {
                        Vector3d v = RandomPositionGenerator.findRandomTargetBlockAwayFrom(mob, 16, 7, p.getPositionVec());
                        if (v != null) mob.getNavigator().tryMoveToXYZ(v.x, v.y, v.z, 1.25D);
                        repath = 40;
                    }
                    break;
                }
                case GOTO: {
                    if (mob.gotoPos != null && repath == 0) {
                        mob.getNavigator().tryMoveToXYZ(mob.gotoPos.getX() + 0.5, mob.gotoPos.getY(), mob.gotoPos.getZ() + 0.5, 1.0D);
                        repath = 20;
                    }
                    break;
                }
                default: {
                    mob.getNavigator().clearPath();
                    mob.getLookController().setLookPositionWithEntity(p, 10.0F, (float) mob.getVerticalFaceSpeed());
                }
            }
        }
    }
}
