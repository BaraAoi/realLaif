package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Братки от Шайкина.
 * Стадия 2 — бьют «до полусмерти», потом обчищают карманы.
 * Стадия 3 — приходят «с паяльником»: убивают и увозят в подвал.
 */
public class BratokEntity extends DebtNpcEntity {
    private int talk;
    private long calmUntil;
    private double pocket;

    public BratokEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new PickupMoneyGoal(this, 1.3D));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new FollowTargetGoal(this, 1.1D, 3.0D));
        this.targetSelector.addGoal(1, new HuntTargetGoal(this));
    }

    @Override
    protected int moneyDrop() {
        return 5;
    }

    @Override
    public boolean likesMoney() {
        return true;
    }

    @Override
    public void onMoneyPicked(double value) {
        pocket += value;
        Player p = getTargetPlayer();
        int need = Config.BRATOK_BRIBE.get();
        if (pocket >= need) {
            if (p != null) say(p, "§aО, другое дело. Я своё взял — пацаны сами разберутся. Бывай, коммерсант.");
            if (p != null && this.level().getServer() != null) {
                EconomyData data = EconomyData.get(this.level().getServer());
                Account a = data.get(p);
                a.nextBratkiTick = Math.max(a.nextBratkiTick, this.level().getGameTime() + 6000);
                data.setDirty();
            }
            this.discard();
        } else if (p != null) {
            say(p, "§eЗабираю " + Finance.money(value) + ". Ещё " + Finance.money(need - pocket) + " — и я тебя не видел.");
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putDouble("Pocket", pocket);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput nbt) {
        super.readAdditionalSaveData(nbt);
        pocket = nbt.getDoubleOr("Pocket", 0.0);
    }

    @Override
    public String npcName() {
        return "§4Браток";
    }

    @Override
    public boolean isStillNeeded(Account a) {
        int st = DebtManager.mfoStage(a);
        return st == 2 || st == 3;
    }

    private boolean lethal() {
        Player p = getTargetPlayer();
        if (p == null || this.level().getServer() == null) return false;
        return DebtManager.mfoStage(EconomyData.get(this.level().getServer()).get(p)) == 3;
    }

    @Override
    public boolean wantsToFight() {
        return this.level().getGameTime() >= calmUntil;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level, Entity target) {
        if (target instanceof ServerPlayer && !lethal()) {
            ServerPlayer p = (ServerPlayer) target;
            if (p.getHealth() <= 7.0F) {
                // не добивают — грабят
                calmUntil = this.level().getGameTime() + 600;
                this.setTarget(null);
                Account a = EconomyData.get(this.level().getServer()).get(p);
                double got = BankService.takeValuables(p, a.mfoDebt, 0.5);
                say(p, got > 0
                        ? "§cЛежи и не дёргайся. Забираем на " + Finance.money(got) + ". Это пока только проценты."
                        : "§cПустой, как барабан... В следующий раз придём с паяльником.");
                if (got > 0) BankService.payMfo(p, a, got, " (изъято братками)");
                EconomyData.get(this.level().getServer()).setDirty();
                return false;
            }
        }
        return super.doHurtTarget(level, target);
    }

    @Override
    protected void serverSecond(ServerPlayer p, Account a) {
        if (++talk % 12 == 1 && this.random.nextInt(2) == 0) {
            String[] soft = {"Слышь, ты Шайкину должен!", "Сейчас будем разговаривать по-взрослому.",
                    "Гони бабки, коммерсант!", "Мы тебя предупреждали."};
            String[] hard = {"Паяльник греется...", "Ну всё, ты доигрался.", "Поедешь с нами в подвал.",
                    "Шайкин сказал — без разговоров."};
            String[] l = lethal() ? hard : soft;
            say(p, "§c" + l[this.random.nextInt(l.length)]);
        }
    }
}
