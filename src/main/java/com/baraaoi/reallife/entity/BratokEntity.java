package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;

/**
 * Братки от Шайкина.
 * Стадия 2 — бьют «до полусмерти», потом обчищают карманы.
 * Стадия 3 — приходят «с паяльником»: убивают и увозят в подвал.
 */
public class BratokEntity extends DebtNpcEntity {
    private int talk;
    private long calmUntil;
    private double pocket;

    public BratokEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
        this.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        this.setDropChance(EquipmentSlotType.MAINHAND, 0.0F);
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
        PlayerEntity p = getTargetPlayer();
        int need = Config.BRATOK_BRIBE.get();
        if (pocket >= need) {
            if (p != null) say(p, "§aО, другое дело. Я своё взял — пацаны сами разберутся. Бывай, коммерсант.");
            if (p != null && world.getServer() != null) {
                EconomyData data = EconomyData.get(world.getServer());
                Account a = data.get(p);
                a.nextBratkiTick = Math.max(a.nextBratkiTick, world.getGameTime() + 6000);
                data.markDirty();
            }
            this.remove();
        } else if (p != null) {
            say(p, "§eЗабираю " + Finance.money(value) + ". Ещё " + Finance.money(need - pocket) + " — и я тебя не видел.");
        }
    }

    @Override
    public void writeAdditional(net.minecraft.nbt.CompoundNBT nbt) {
        super.writeAdditional(nbt);
        nbt.putDouble("Pocket", pocket);
    }

    @Override
    public void readAdditional(net.minecraft.nbt.CompoundNBT nbt) {
        super.readAdditional(nbt);
        pocket = nbt.getDouble("Pocket");
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
        PlayerEntity p = getTargetPlayer();
        if (p == null || world.getServer() == null) return false;
        return DebtManager.mfoStage(EconomyData.get(world.getServer()).get(p)) == 3;
    }

    @Override
    public boolean wantsToFight() {
        return world.getGameTime() >= calmUntil;
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (target instanceof ServerPlayerEntity && !lethal()) {
            ServerPlayerEntity p = (ServerPlayerEntity) target;
            if (p.getHealth() <= 7.0F) {
                // не добивают — грабят
                calmUntil = world.getGameTime() + 600;
                this.setAttackTarget(null);
                Account a = EconomyData.get(world.getServer()).get(p);
                double got = BankService.takeValuables(p, a.mfoDebt, 0.5);
                say(p, got > 0
                        ? "§cЛежи и не дёргайся. Забираем на " + Finance.money(got) + ". Это пока только проценты."
                        : "§cПустой, как барабан... В следующий раз придём с паяльником.");
                if (got > 0) BankService.payMfo(p, a, got, " (изъято братками)");
                EconomyData.get(world.getServer()).markDirty();
                return false;
            }
        }
        return super.attackEntityAsMob(target);
    }

    @Override
    protected void serverSecond(ServerPlayerEntity p, Account a) {
        if (++talk % 12 == 1 && this.rand.nextInt(2) == 0) {
            String[] soft = {"Слышь, ты Шайкину должен!", "Сейчас будем разговаривать по-взрослому.",
                    "Гони бабки, коммерсант!", "Мы тебя предупреждали."};
            String[] hard = {"Паяльник греется...", "Ну всё, ты доигрался.", "Поедешь с нами в подвал.",
                    "Шайкин сказал — без разговоров."};
            String[] l = lethal() ? hard : soft;
            say(p, "§c" + l[this.rand.nextInt(l.length)]);
        }
    }
}
