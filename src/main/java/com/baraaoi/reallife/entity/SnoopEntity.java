package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.EconomyData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.OpenDoorGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
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
public class SnoopEntity extends CreatureEntity {
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
    @Nullable
    public UUID targetPlayer;
    @Nullable
    private Vector3d fleeFrom;
    private boolean spooked;
    private int mutterCooldown = 200;

    public SnoopEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
        if (this.getNavigator() instanceof GroundPathNavigator) {
            GroundPathNavigator nav = (GroundPathNavigator) this.getNavigator();
            nav.setBreakDoors(true); // = умеет проходить через закрытые двери (открывает их)
        }
        this.setCustomName(new StringTextComponent(NAME));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new FleeGoal());
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(3, new SnoopGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomWalkingGoal(this, 0.7D));
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(7, new LookRandomlyGoal(this));
    }

    public boolean isFleeing() {
        return fleeTicks > 0;
    }

    @Override
    public void livingTick() {
        super.livingTick();
        if (world.isRemote) return;
        if (fleeTicks > 0) {
            fleeTicks--;
            if (fleeTicks == 0 && spooked) { this.remove(); return; }
        }
        life--;
        if (life <= 0 && fleeTicks <= 0) {
            // пора уходить: «по делам» — убегает и исчезает
            fleeFrom = this.getPositionVec().add(rand.nextGaussian(), 0, rand.nextGaussian());
            fleeTicks = 200;
            spooked = true;
        }
        if (--mutterCooldown <= 0) {
            mutterCooldown = 500 + rand.nextInt(700);
            PlayerEntity p = world.getClosestPlayer(this, 7.0D);
            if (p != null && fleeTicks <= 0) say(MUTTER[rand.nextInt(MUTTER.length)], 10);
        }
    }

    /** Сказать фразу всем игрокам в радиусе. */
    public void say(String text, double radius) {
        if (world.isRemote) return;
        for (PlayerEntity p : world.getPlayers()) {
            if (p.getDistanceSq(this) <= radius * radius) {
                p.sendStatusMessage(new StringTextComponent("§7<" + NAME + "§7> §f" + text), false);
            }
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean r = super.attackEntityFrom(source, amount);
        if (!world.isRemote && this.isAlive() && source.getTrueSource() instanceof PlayerEntity) {
            Entity by = source.getTrueSource();
            if (!spooked) say(HIT[rand.nextInt(HIT.length)], 16);
            spooked = true;
            fleeFrom = by.getPositionVec();
            fleeTicks = 200;
            this.getNavigator().clearPath();
        }
        return r;
    }

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void writeAdditional(CompoundNBT nbt) {
        super.writeAdditional(nbt);
        nbt.putInt("Life", life);
        nbt.putInt("Flee", fleeTicks);
        nbt.putBoolean("Spooked", spooked);
        if (targetPlayer != null) nbt.putUniqueId("Target", targetPlayer);
    }

    @Override
    public void readAdditional(CompoundNBT nbt) {
        super.readAdditional(nbt);
        life = nbt.contains("Life") ? nbt.getInt("Life") : 6000;
        fleeTicks = nbt.getInt("Flee");
        spooked = nbt.getBoolean("Spooked");
        targetPlayer = nbt.hasUniqueId("Target") ? nbt.getUniqueId("Target") : null;
        if (spooked && fleeTicks <= 0) fleeTicks = 40;
    }

    // ------------------------------------------------------------------ куда сунуть нос

    /** Интересные точки: станки и дом игрока-«клиента», а также чужие дома рядом. */
    List<BlockPos> interests() {
        List<BlockPos> l = new ArrayList<>();
        MinecraftServer server = this.getServer();
        if (server == null) return l;
        EconomyData data = EconomyData.get(server);
        BlockPos me = this.getPosition();
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
    @Nullable
    BlockPos standNear(BlockPos target) {
        for (int i = 0; i < 30; i++) {
            int dx = rand.nextInt(5) - 2, dz = rand.nextInt(5) - 2, dy = rand.nextInt(3) - 1;
            BlockPos p = target.add(dx, dy, dz);
            if (!world.isBlockLoaded(p)) continue;
            BlockState below = world.getBlockState(p.down());
            if (world.isAirBlock(p) && world.isAirBlock(p.up()) && below.getMaterial().isSolid()) return p;
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
            this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean shouldExecute() {
            if (isFleeing()) return false;
            if (cooldown > 0) { cooldown--; return false; }
            List<BlockPos> l = interests();
            if (l.isEmpty() || rand.nextInt(4) == 0) {
                Vector3d v = RandomPositionGenerator.findRandomTarget(SnoopEntity.this, 16, 5);
                if (v == null) return false;
                look = null;
                goal = new BlockPos(v);
            } else {
                look = l.get(rand.nextInt(l.size()));
                goal = standNear(look);
                if (goal == null) { cooldown = 60; return false; }
            }
            linger = 80 + rand.nextInt(140);
            timeout = 900;
            return SnoopEntity.this.getNavigator().tryMoveToXYZ(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 0.75D);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !isFleeing() && goal != null && linger > 0 && timeout > 0;
        }

        @Override
        public void tick() {
            timeout--;
            boolean near = SnoopEntity.this.getDistanceSq(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5) < 2.5D * 2.5D;
            if (near || SnoopEntity.this.getNavigator().noPath()) {
                if (!near && timeout % 40 == 0) {
                    SnoopEntity.this.getNavigator().tryMoveToXYZ(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 0.75D);
                }
                if (near) {
                    linger--;
                    if (look != null) SnoopEntity.this.getLookController().setLookPosition(look.getX() + 0.5, look.getY() + 0.5, look.getZ() + 0.5, 10F, 40F);
                } else if (SnoopEntity.this.getNavigator().noPath()) {
                    linger -= 3;
                }
            }
        }

        @Override
        public void resetTask() {
            goal = null;
            look = null;
            cooldown = 20 + rand.nextInt(60);
            SnoopEntity.this.getNavigator().clearPath();
        }
    }

    /** Бегство после удара: подальше от обидчика, на повышенной скорости. */
    class FleeGoal extends Goal {
        FleeGoal() {
            this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean shouldExecute() {
            return isFleeing();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return isFleeing();
        }

        @Override
        public void tick() {
            if (SnoopEntity.this.getNavigator().noPath() || SnoopEntity.this.ticksExisted % 30 == 0) {
                Vector3d from = fleeFrom == null ? SnoopEntity.this.getPositionVec() : fleeFrom;
                Vector3d v = RandomPositionGenerator.findRandomTargetBlockAwayFrom(SnoopEntity.this, 20, 6, from);
                if (v == null) v = RandomPositionGenerator.findRandomTarget(SnoopEntity.this, 16, 5);
                if (v != null) SnoopEntity.this.getNavigator().tryMoveToXYZ(v.x, v.y, v.z, 1.45D);
            }
        }

        @Override
        public void resetTask() {
            SnoopEntity.this.getNavigator().clearPath();
        }
    }

    @Nullable
    public ServerPlayerEntity target() {
        MinecraftServer s = getServer();
        return s == null || targetPlayer == null ? null : s.getPlayerList().getPlayerByUUID(targetPlayer);
    }
}
