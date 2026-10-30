package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.LegalBusiness;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Покупатель (v1.7) — «живость» заведений. Приходит к открытому заведению (есть работники, оборудование и сырьё),
 * открывает двери, подходит к кассе и к паре единиц оборудования, задерживается, бормочет заказ — и уходит.
 * Косметика: на выручку не влияет, но считается в «покупателях за день».
 */
public class CustomerEntity extends PathfinderMob {
    public static final int VARIANTS = 6;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);

    private static final String[][] ORDER = {
            {"Одну шаурму, сырную, без лука!", "А шаурма сегодняшняя? Точно-точно?", "Две с собой, и соуса побольше!"},
            {"Пакет не нужен, свой есть.", "А хлеб свежий? Посмотрю срок годности...", "Девушка, а где у вас молоко?"},
            {"Мне комплекс, и коврики протрите!", "Только аккуратно, она новая. Почти.", "А воском натрёте? Сколько?"},
            {"Щебень фракции 20-40 есть? КамАЗ пришлю.", "Мне песка машину, на дачу.", "Почём куб гранита?"},
            {"Сколько дадите за бабушкины серьги?", "Это золото, честно! Ну, позолота...", "Телефон заложить хочу, до зарплаты."},
            {"Девяносто пятый, полный бак!", "Мне хот-дог и кофе. И на третьей колонке двадцать литров.", "Омывайку незамерзайку есть?"},
            {"А этот телевизор со скидкой будет?", "Мне просто посмотреть. Я просто смотрю.", "А гарантия сколько? А если уроню?"}};
    private static final String[] BYE = {"Спасибо, до свидания!", "Ещё зайду!", "Дороговато у вас...", "*довольно уходит*", "Сдачу не надо!"};
    private static final String[] HIT = {"Ай! Да я на вас жалобу напишу!", "Больше сюда ни ногой!", "Охрана! Хулиганы!"};

    public int bizId = -1;
    public int life = 3000;
    /** 0 — к точкам заведения, 1 — уходит. */
    public int stage;
    
    public BlockPos counter;
    public final List<BlockPos> targets = new ArrayList<>();
    private int leaveTicks;
    private boolean served;

    public CustomerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        if (this.getNavigation() instanceof GroundPathNavigation) ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    public int getVariant() {
        return Math.max(0, Math.min(VARIANTS - 1, this.entityData.get(VARIANT)));
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, Math.floorMod(v, VARIANTS));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new ShopGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    
    LegalBusiness biz() {
        if (this.level().getServer() == null || bizId < 0) return null;
        return EconomyData.get(this.level().getServer()).bizById(bizId);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;
        life--;
        if (stage == 1) leaveTicks++;
        boolean far = counter == null || this.distanceToSqr(counter.getX() + 0.5, counter.getY(), counter.getZ() + 0.5) > 22 * 22;
        if (life <= 0 || (stage == 1 && (far || leaveTicks > 400))) this.discard();
        else if (stage == 0 && life < 600) startLeaving();
    }

    void startLeaving() {
        if (stage == 1) return;
        stage = 1;
        leaveTicks = 0;
        this.getNavigation().stop();
    }

    /** Сказать фразу игрокам рядом. */
    public void say(String text, double radius) {
        if (this.level().isClientSide()) return;
        for (Player p : this.level().players()) {
            if (p.distanceToSqr(this) <= radius * radius) {
                p.sendSystemMessage(Component.literal("§7<§fПокупатель§7> §f" + text));
            }
        }
    }

    String orderLine() {
        LegalBusiness b = biz();
        BizType t = b == null ? BizType.MINIMARKET : b.type;
        String[] l = ORDER[t.ordinal() % ORDER.length];
        return l[random.nextInt(l.length)];
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        boolean r = super.hurtServer(level, source, amount);
        if (!this.level().isClientSide() && this.isAlive() && source.getEntity() instanceof Player && stage == 0) {
            say(HIT[random.nextInt(HIT.length)], 12);
            startLeaving();
        }
        return r;
    }

    /** ПКМ игроком (из ForgeEvents). */
    public void talkTo(Player player) {
        player.sendSystemMessage(Component.literal("§7<§fПокупатель§7> §f" + (stage == 0 ? orderLine() : BYE[random.nextInt(BYE.length)])));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Biz", bizId);
        nbt.putInt("Life", life);
        nbt.putInt("Variant", getVariant());
        if (counter != null) nbt.putLong("Counter", counter.asLong());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput nbt) {
        super.readAdditionalSaveData(nbt);
        bizId = nbt.getIntOr("Biz", 0);
        life = Math.min(600, nbt.getIntOr("Life", 600)); // после загрузки мира — быстро уходит
        setVariant(nbt.getIntOr("Variant", 0));
        counter = nbt.getLong("Counter").map(BlockPos::of).orElse(null);
        stage = 1;
    }

    /** Место, где можно встать рядом с target. */
    
    BlockPos standNear(BlockPos target) {
        for (int i = 0; i < 30; i++) {
            int dx = random.nextInt(3) - 1, dz = random.nextInt(3) - 1, dy = random.nextInt(3) - 1;
            if (dx == 0 && dz == 0) continue;
            BlockPos p = target.offset(dx, dy, dz);
            if (!this.level().isLoaded(p)) continue;
            BlockState below = this.level().getBlockState(p.below());
            if (this.level().isEmptyBlock(p) && this.level().isEmptyBlock(p.above()) && below.isSolid()) return p;
        }
        return null;
    }

    /** Обход точек заведения, потом — уход прочь от кассы. */
    class ShopGoal extends Goal {
        private BlockPos look;
        private BlockPos goal;
        private int linger;
        private int timeout;

        ShopGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            CustomerEntity me = CustomerEntity.this;
            if (stage == 1) {
                Vec3 from = counter == null ? me.position() : new Vec3(counter.getX() + 0.5, counter.getY(), counter.getZ() + 0.5);
                Vec3 v = DefaultRandomPos.getPosAway(me, 24, 7, from);
                if (v == null) return false;
                goal = BlockPos.containing(v);
                look = null;
                linger = 0;
                timeout = 400;
                return me.getNavigation().moveTo(v.x, v.y, v.z, 0.8D);
            }
            while (!targets.isEmpty()) {
                look = targets.remove(0);
                goal = standNear(look);
                if (goal != null) break;
            }
            if (goal == null) {
                if (targets.isEmpty()) startLeaving();
                return false;
            }
            linger = 60 + random.nextInt(100);
            timeout = 600;
            return me.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 0.7D);
        }

        @Override
        public boolean canContinueToUse() {
            if (stage == 1) return goal != null && timeout > 0 && !CustomerEntity.this.getNavigation().isDone();
            return goal != null && linger > 0 && timeout > 0;
        }

        @Override
        public void tick() {
            CustomerEntity me = CustomerEntity.this;
            timeout--;
            if (stage == 1 || goal == null) return;
            boolean near = me.distanceToSqr(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5) < 1.8D * 1.8D;
            if (near) {
                if (look != null) me.getLookControl().setLookAt(look.getX() + 0.5, look.getY() + 0.5, look.getZ() + 0.5, 10F, 40F);
                if (linger == 40 && look != null && look.equals(counter) && !served) {
                    served = true;
                    LegalBusiness b = biz();
                    if (b != null) b.customersToday++;
                    if (random.nextInt(3) == 0) say(orderLine(), 10);
                }
                linger--;
            } else if (me.getNavigation().isDone()) {
                if (timeout % 40 == 0) me.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 0.7D);
                linger -= 2;
            }
        }

        @Override
        public void stop() {
            boolean wasCounter = look != null && look.equals(counter);
            goal = null;
            look = null;
            CustomerEntity.this.getNavigation().stop();
            if (stage == 0 && targets.isEmpty()) {
                if (wasCounter || served) { if (random.nextInt(4) == 0) say(BYE[random.nextInt(BYE.length)], 8); }
                startLeaving();
            }
        }
    }
}
