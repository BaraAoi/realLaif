package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.LegalBiz;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

/**
 * Наёмный работник заведения: стоит за кассой. Без него заведение не работает и не приносит прибыли.
 * Убьют — придётся нанимать нового.
 */
public class WorkerEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.INT);

    public int bizId;

    public WorkerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, 0);
    }

    public BizType getBizType() {
        BizType t = BizType.byId(this.entityData.get(TYPE));
        return t == null ? BizType.SHAWARMA : t;
    }

    public void setBizType(BizType t) {
        this.entityData.set(TYPE, t.ordinal());
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
        if (e instanceof Player || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, amount);
        return false;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!this.level().isClientSide()) LegalBiz.onWorkerDeath(this);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("WorkerType", this.entityData.get(TYPE));
        nbt.putInt("BizId", bizId);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput nbt) {
        super.readAdditionalSaveData(nbt);
        this.entityData.set(TYPE, nbt.getIntOr("WorkerType", 0));
        this.bizId = nbt.getIntOr("BizId", 0);
    }
}
