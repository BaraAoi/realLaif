package com.baraaoi.reallife.entity;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.economy.ItemValues;
import com.baraaoi.reallife.event.DebtManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Коллектор Вован из МФО. Хамит, угрожает и «изымает» вещи прямо из карманов (по 50% цены).
 */
public class VovanEntity extends DebtNpcEntity {
    private int talk;

    public VovanEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new FollowTargetGoal(this, 1.15D, 1.5D));
    }

    @Override
    public String npcName() {
        return "§2Коллектор Вован";
    }

    @Override
    public boolean isStillNeeded(Account a) {
        return DebtManager.mfoStage(a) == 1;
    }

    @Override
    protected void serverSecond(ServerPlayerEntity p, Account a) {
        talk++;
        if (talk % 20 == 1) {
            String[] lines = {
                    "Слышь, должник! Шайкин передаёт привет. Долг: §c%s",
                    "Ну чё, когда бабки будут? §c%s§f, и не копейкой меньше!",
                    "Я тебе не Жора из банка, я ждать не буду. §c%s",
                    "Красивый дом у тебя. Будет жаль, если с ним что-то случится. §c%s",
                    "Братва уже интересуется, почему ты нам §c%s§f должен.",
            };
            say(p, String.format(lines[(talk / 20) % lines.length], Finance.money(a.mfoDebt)));
        }
        // каждые 45 секунд — «изъятие» из карманов, если рядом
        if (talk % 45 == 0 && this.getDistanceSq(p) < 9.0) {
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < p.inventory.mainInventory.size(); i++) {
                if (ItemValues.valueOf(p.inventory.mainInventory.get(i)) > 0) slots.add(i);
            }
            if (!slots.isEmpty()) {
                int slot = slots.get(this.rand.nextInt(slots.size()));
                ItemStack st = p.inventory.mainInventory.get(slot);
                int take = Math.max(1, Math.min(st.getCount(), 1 + this.rand.nextInt(8)));
                double value = ItemValues.valueOf(st) * take * 0.5;
                String name = st.getDisplayName().getString();
                st.shrink(take);
                say(p, "§cЭто я забираю: " + name + " x" + take + "§f. Зачтём " + Finance.money(value) + ", не благодари.");
                BankService.payMfo(p, a, value, " (изъято коллектором)");
                EconomyData.get(world.getServer()).markDirty();
            } else {
                say(p, "Карманы пустые? Ну-ну. Братва с тобой по-другому поговорит.");
            }
        }
    }

    @Override
    protected void onDismissed(ServerPlayerEntity p) {
        say(p, "Вот так бы сразу. Обращайся ещё, хе-хе.");
    }
}
