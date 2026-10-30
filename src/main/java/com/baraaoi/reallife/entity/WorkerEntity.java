package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.LegalBiz;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * Наёмный работник заведения: стоит за кассой. Без него заведение не работает и не приносит прибыли.
 * Убьют — придётся нанимать нового.
 */
public class WorkerEntity extends CreatureEntity {
    private static final DataParameter<Integer> TYPE = EntityDataManager.createKey(WorkerEntity.class, DataSerializers.VARINT);

    public int bizId;

    public WorkerEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
        this.enablePersistence();
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(TYPE, 0);
    }

    public BizType getBizType() {
        BizType t = BizType.byId(this.dataManager.get(TYPE));
        return t == null ? BizType.SHAWARMA : t;
    }

    public void setBizType(BizType t) {
        this.dataManager.set(TYPE, t.ordinal());
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
        if (e instanceof PlayerEntity || source == DamageSource.OUT_OF_WORLD) return super.attackEntityFrom(source, amount);
        return false;
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (!world.isRemote) LegalBiz.onWorkerDeath(this);
    }

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public void writeAdditional(CompoundNBT nbt) {
        super.writeAdditional(nbt);
        nbt.putInt("WorkerType", this.dataManager.get(TYPE));
        nbt.putInt("BizId", bizId);
    }

    @Override
    public void readAdditional(CompoundNBT nbt) {
        super.readAdditional(nbt);
        this.dataManager.set(TYPE, nbt.getInt("WorkerType"));
        this.bizId = nbt.getInt("BizId");
    }
}
