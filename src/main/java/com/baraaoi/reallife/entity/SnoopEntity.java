package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.util.Nbt;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.EconomyData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * «Подозрительный тип» v6 — самостоятельный NPC (не случайное событие).
 * Шляется туда-сюда, открывает двери, заглядывает в дома и к печатным станкам, бормочет.
 * Ударишь — с воплем убегает и исчезает. Сам уходит через ~5 минут.
 * Пока он рядом и видит станок — это наводка для ОБЭП (см. Raids).
 */
public class SnoopEntity extends PathfinderMob {
    public static final String NAME = "§8Подозрительный тип";
    private static final String[] MUTTER = {
            "Я тут просто гуляю... воздухом дышу.",
            "Хорошая хата. А что это у тебя там так гудит?",
            "Чё смотришь? Улица общая.",
            "Не подскажешь, который час? ...А, неважно.",
            "Слушай, а ты где работаешь? Просто интересно.",
            "Сколько тут квадратов? Просто спрашиваю.",
            "Говорят, тут кто-то деньги печатает. Не слышал?",
            "*что-то быстро записывает в блокнот*",
            "*фотографирует забор на телефон*",
            "*делает вид, что завязывает шнурки*"};
    private static final String[] HIT = {
            "Ай! Всё-всё, ухожу! Я ничего не видел!",
            "Э, ты чё?! Я в полицию... то есть, домой!",
            "Псих! Я просто мимо шёл!",
            "Понял, не дурак! Меня тут не было!",
            "Ладно-ладно! Уже бегу! *роняет блокнот*"};

    /** Сколько тиков ему ещё гулять. */
    public int life = 6000;
    /** Тиков бегства (после удара). */
    public int fleeTicks;
    
    public UUID targetPlayer;
    
    private Vec3 fleeFrom;
    private boolean spooked;
    private int mutterCooldown = 200;

    public SnoopEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        if (this.getNavigation() instanceof GroundPathNavigation) {
            GroundPathNavigation nav = (GroundPathNavigation) this.getNavigation();
            nav.setCanOpenDoors(true); // = умеет проходить через закрытые двери (открывает их)
        }
        this.setCustomName(Component.literal(NAME));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FleeGoal());
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(3, new SnoopGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public boolean isFleeing() {
        return fleeTicks > 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;
        if (fleeTicks > 0) {
            fleeTicks--;
            if (fleeTicks == 0 && spooked) { this.discard(); return; }
        }
        life--;
        if (life <= 0 && fleeTicks <= 0) {
            // пора уходить: «по делам» — убегает и исчезает
            fleeFrom = this.position().add(random.nextGaussian(), 0, random.nextGaussian());
            fleeTicks = 200;
            spooked = true;
        }
        if (--mutterCooldown <= 0) {
            mutterCooldown = 500 + random.nextInt(700);
            Player p = this.level().getNearestPlayer(this, 7.0D);
            if (p != null && fleeTicks <= 0) say(MUTTER[random.nextInt(MUTTER.length)], 10);
        }
    }

    /** Сказать фразу всем игрокам в радиусе. */
    public void say(String text, double radius) {
        if (this.level().isClientSide()) return;
        for (Player p : this.level().players()) {
            if (p.distanceToSqr(this) <= radius * radius) {
                p.sendSystemMessage(Component.literal("§7<" + NAME + "§7> §f" + text));
            }
        }
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        boolean r = super.hurtServer(level, source, amount);
        if (!this.level().isClientSide() && this.isAlive() && source.getEntity() instanceof Player) {
            Entity by = source.getEntity();
            if (!spooked) say(HIT[random.nextInt(HIT.length)], 16);
            spooked = true;
            fleeFrom = by.position();
            fleeTicks = 200;
            this.getNavigation().stop();
        }
        return r;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Life", life);
        nbt.putInt("Flee", fleeTicks);
        nbt.putBoolean("Spooked", spooked);
        if (targetPlayer != null) Nbt.putUUID(nbt, "Target", targetPlayer);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput nbt) {
        super.readAdditionalSaveData(nbt);
        life = nbt.getIntOr("Life", 6000);
        fleeTicks = nbt.getIntOr("Flee", 0);
        spooked = nbt.getBooleanOr("Spooked", false);
        targetPlayer = Nbt.hasUUID(nbt, "Target") ? Nbt.getUUID(nbt, "Target") : null;
        if (spooked && fleeTicks <= 0) fleeTicks = 40;
    }

    // ------------------------------------------------------------------ куда сунуть нос

    /** Интересные точки: станки и дом игрока-«клиента», а также чужие дома рядом. */
    List<BlockPos> interests() {
        List<BlockPos> l = new ArrayList<>();
        MinecraftServer server = this.level().getServer();
        if (server == null) return l;
        EconomyData data = EconomyData.get(server);
        BlockPos me = this.blockPosition();
        for (Account a : data.all()) {
            boolean mine = a.id.equals(targetPlayer);
            for (BlockPos p : a.printerList) if (mine || p.distanceSq(me) < 40 * 40) l.add(p);
            if (a.hasHouse && !a.houseMin.equals(BlockPos.ZERO)) {
                BlockPos c = new BlockPos((a.houseMin.getX() + a.houseMax.getX()) / 2, a.houseMin.getY() + 1,
                        (a.houseMin.getZ() + a.houseMax.getZ()) / 2);
                if (mine || c.distanceSq(me) < 40 * 40) l.add(c);
            }
        }
        l.removeIf(p -> p.distanceSq(me) > 48 * 48);
        return l;
    }

    /** Место, где можно встать рядом с target (воздух на 2 блока, под ногами твёрдое). */
    
    BlockPos standNear(BlockPos target) {
        for (int i = 0; i < 30; i++) {
            int dx = random.nextInt(5) - 2, dz = random.nextInt(5) - 2, dy = random.nextInt(3) - 1;
            BlockPos p = target.offset(dx, dy, dz);
            if (!this.level().isLoaded(p)) continue;
            BlockState below = this.level().getBlockState(p.below());
            if (this.level().isEmptyBlock(p) && this.level().isEmptyBlock(p.above()) && below.isSolid()) return p;
        }
        return null;
    }

    /** Ходит от одной «интересной» точки к другой и задерживается возле них. */
    class SnoopGoal extends Goal {
        private BlockPos goal;
        private BlockPos look;
        private int linger;
        private int timeout;
        private int cooldown;

        SnoopGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isFleeing()) return false;
            if (cooldown > 0) { cooldown--; return false; }
            List<BlockPos> l = interests();
            if (l.isEmpty() || random.nextInt(4) == 0) {
                Vec3 v = DefaultRandomPos.getPos(SnoopEntity.this, 16, 5);
                if (v == null) return false;
                look = null;
                goal = BlockPos.containing(v);
            } else {
                look = l.get(random.nextInt(l.size()));
                goal = standNear(look);
                if (goal == null) { cooldown = 60; return false; }
            }
            linger = 80 + random.nextInt(140);
            timeout = 900;
            return SnoopEntity.this.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 0.75D);
        }

        @Override
        public boolean canContinueToUse() {
            return !isFleeing() && goal != null && linger > 0 && timeout > 0;
        }

        @Override
        public void tick() {
            timeout--;
            boolean near = SnoopEntity.this.distanceToSqr(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5) < 2.5D * 2.5D;
            if (near || SnoopEntity.this.getNavigation().isDone()) {
                if (!near && timeout % 40 == 0) {
                    SnoopEntity.this.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 0.75D);
                }
                if (near) {
                    linger--;
                    if (look != null) SnoopEntity.this.getLookControl().setLookAt(look.getX() + 0.5, look.getY() + 0.5, look.getZ() + 0.5, 10F, 40F);
                } else if (SnoopEntity.this.getNavigation().isDone()) {
                    linger -= 3;
                }
            }
        }

        @Override
        public void stop() {
            goal = null;
            look = null;
            cooldown = 20 + random.nextInt(60);
            SnoopEntity.this.getNavigation().stop();
        }
    }

    /** Бегство после удара: подальше от обидчика, на повышенной скорости. */
    class FleeGoal extends Goal {
        FleeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isFleeing();
        }

        @Override
        public boolean canContinueToUse() {
            return isFleeing();
        }

        @Override
        public void tick() {
            if (SnoopEntity.this.getNavigation().isDone() || SnoopEntity.this.tickCount % 30 == 0) {
                Vec3 from = fleeFrom == null ? SnoopEntity.this.position() : fleeFrom;
                Vec3 v = DefaultRandomPos.getPosAway(SnoopEntity.this, 20, 6, from);
                if (v == null) v = DefaultRandomPos.getPos(SnoopEntity.this, 16, 5);
                if (v != null) SnoopEntity.this.getNavigation().moveTo(v.x, v.y, v.z, 1.45D);
            }
        }

        @Override
        public void stop() {
            SnoopEntity.this.getNavigation().stop();
        }
    }

    
    public ServerPlayer target() {
        MinecraftServer s = getServer();
        return s == null || targetPlayer == null ? null : s.getPlayerList().getPlayer(targetPlayer);
    }
}
