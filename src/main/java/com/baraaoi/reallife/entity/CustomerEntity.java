package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.LegalBusiness;
import net.minecraft.block.BlockState;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.OpenDoorGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Покупатель (v1.7) — «живость» заведений. Приходит к открытому заведению (есть работники, оборудование и сырьё),
 * открывает двери, подходит к кассе и к паре единиц оборудования, задерживается, бормочет заказ — и уходит.
 * Косметика: на выручку не влияет, но считается в «покупателях за день».
 */
public class CustomerEntity extends CreatureEntity {
    public static final int VARIANTS = 6;
    private static final DataParameter<Integer> VARIANT = EntityDataManager.createKey(CustomerEntity.class, DataSerializers.VARINT);

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
    @Nullable
    public BlockPos counter;
    public final List<BlockPos> targets = new ArrayList<>();
    private int leaveTicks;
    private boolean served;

    public CustomerEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
        if (this.getNavigator() instanceof GroundPathNavigator) ((GroundPathNavigator) this.getNavigator()).setBreakDoors(true);
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(VARIANT, 0);
    }

    public int getVariant() {
        return Math.max(0, Math.min(VARIANTS - 1, this.dataManager.get(VARIANT)));
    }

    public void setVariant(int v) {
        this.dataManager.set(VARIANT, Math.floorMod(v, VARIANTS));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new ShopGoal());
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(7, new LookRandomlyGoal(this));
    }

    @Nullable
    LegalBusiness biz() {
        if (this.getServer() == null || bizId < 0) return null;
        return EconomyData.get(this.getServer()).bizById(bizId);
    }

    @Override
    public void livingTick() {
        super.livingTick();
        if (world.isRemote) return;
        life--;
        if (stage == 1) leaveTicks++;
        boolean far = counter == null || this.getDistanceSq(counter.getX() + 0.5, counter.getY(), counter.getZ() + 0.5) > 22 * 22;
        if (life <= 0 || (stage == 1 && (far || leaveTicks > 400))) this.remove();
        else if (stage == 0 && life < 600) startLeaving();
    }

    void startLeaving() {
        if (stage == 1) return;
        stage = 1;
        leaveTicks = 0;
        this.getNavigator().clearPath();
    }

    /** Сказать фразу игрокам рядом. */
    public void say(String text, double radius) {
        if (world.isRemote) return;
        for (PlayerEntity p : world.getPlayers()) {
            if (p.getDistanceSq(this) <= radius * radius) {
                p.sendStatusMessage(new StringTextComponent("§7<§fПокупатель§7> §f" + text), false);
            }
        }
    }

    String orderLine() {
        LegalBusiness b = biz();
        BizType t = b == null ? BizType.MINIMARKET : b.type;
        String[] l = ORDER[t.ordinal() % ORDER.length];
        return l[rand.nextInt(l.length)];
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean r = super.attackEntityFrom(source, amount);
        if (!world.isRemote && this.isAlive() && source.getTrueSource() instanceof PlayerEntity && stage == 0) {
            say(HIT[rand.nextInt(HIT.length)], 12);
            startLeaving();
        }
        return r;
    }

    /** ПКМ игроком (из ForgeEvents). */
    public void talkTo(PlayerEntity player) {
        player.sendStatusMessage(new StringTextComponent("§7<§fПокупатель§7> §f" + (stage == 0 ? orderLine() : BYE[rand.nextInt(BYE.length)])), false);
    }

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void writeAdditional(CompoundNBT nbt) {
        super.writeAdditional(nbt);
        nbt.putInt("Biz", bizId);
        nbt.putInt("Life", life);
        nbt.putInt("Variant", getVariant());
        if (counter != null) nbt.putLong("Counter", counter.toLong());
    }

    @Override
    public void readAdditional(CompoundNBT nbt) {
        super.readAdditional(nbt);
        bizId = nbt.getInt("Biz");
        life = Math.min(600, nbt.contains("Life") ? nbt.getInt("Life") : 600); // после загрузки мира — быстро уходит
        setVariant(nbt.getInt("Variant"));
        counter = nbt.contains("Counter") ? BlockPos.fromLong(nbt.getLong("Counter")) : null;
        stage = 1;
    }

    /** Место, где можно встать рядом с target. */
    @Nullable
    BlockPos standNear(BlockPos target) {
        for (int i = 0; i < 30; i++) {
            int dx = rand.nextInt(3) - 1, dz = rand.nextInt(3) - 1, dy = rand.nextInt(3) - 1;
            if (dx == 0 && dz == 0) continue;
            BlockPos p = target.add(dx, dy, dz);
            if (!world.isBlockLoaded(p)) continue;
            BlockState below = world.getBlockState(p.down());
            if (world.isAirBlock(p) && world.isAirBlock(p.up()) && below.getMaterial().isSolid()) return p;
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
            this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean shouldExecute() {
            CustomerEntity me = CustomerEntity.this;
            if (stage == 1) {
                Vector3d from = counter == null ? me.getPositionVec() : new Vector3d(counter.getX() + 0.5, counter.getY(), counter.getZ() + 0.5);
                Vector3d v = RandomPositionGenerator.findRandomTargetBlockAwayFrom(me, 24, 7, from);
                if (v == null) return false;
                goal = new BlockPos(v);
                look = null;
                linger = 0;
                timeout = 400;
                return me.getNavigator().tryMoveToXYZ(v.x, v.y, v.z, 0.8D);
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
            linger = 60 + rand.nextInt(100);
            timeout = 600;
            return me.getNavigator().tryMoveToXYZ(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 0.7D);
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (stage == 1) return goal != null && timeout > 0 && !CustomerEntity.this.getNavigator().noPath();
            return goal != null && linger > 0 && timeout > 0;
        }

        @Override
        public void tick() {
            CustomerEntity me = CustomerEntity.this;
            timeout--;
            if (stage == 1 || goal == null) return;
            boolean near = me.getDistanceSq(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5) < 1.8D * 1.8D;
            if (near) {
                if (look != null) me.getLookController().setLookPosition(look.getX() + 0.5, look.getY() + 0.5, look.getZ() + 0.5, 10F, 40F);
                if (linger == 40 && look != null && look.equals(counter) && !served) {
                    served = true;
                    LegalBusiness b = biz();
                    if (b != null) b.customersToday++;
                    if (rand.nextInt(3) == 0) say(orderLine(), 10);
                }
                linger--;
            } else if (me.getNavigator().noPath()) {
                if (timeout % 40 == 0) me.getNavigator().tryMoveToXYZ(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 0.7D);
                linger -= 2;
            }
        }

        @Override
        public void resetTask() {
            boolean wasCounter = look != null && look.equals(counter);
            goal = null;
            look = null;
            CustomerEntity.this.getNavigator().clearPath();
            if (stage == 0 && targets.isEmpty()) {
                if (wasCounter || served) { if (rand.nextInt(4) == 0) say(BYE[rand.nextInt(BYE.length)], 8); }
                startLeaving();
            }
        }
    }
}
