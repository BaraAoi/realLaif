package com.baraaoi.reallife;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.economy.CreditRating;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.economy.HouseType;
import com.baraaoi.reallife.entity.DebtNpcEntity;
import com.baraaoi.reallife.event.DebtManager;
import com.baraaoi.reallife.registry.ModBlocks;
import com.baraaoi.reallife.registry.ModEntities;
import com.baraaoi.reallife.structure.Blueprint;
import com.baraaoi.reallife.structure.Buildings;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Интеграционный самотест для CI: запускается только с -Dreallife.selftest=true.
 * Строит все здания, прогоняет экономику по дням и создаёт всех NPC, затем выключает сервер.
 */
public class SelfTest {
    private static final Logger LOG = LogManager.getLogger("RealLifeSelfTest");

    private static void check(boolean cond, String what) {
        if (!cond) throw new IllegalStateException("CHECK FAILED: " + what);
        LOG.info("ok: " + what);
    }

    @SubscribeEvent
    public void onStarted(FMLServerStartedEvent event) {
        if (!Boolean.getBoolean("reallife.selftest")) return;
        MinecraftServer s = event.getServer();
        try {
            run(s);
            LOG.info("REALLIFE SELFTEST PASSED");
        } catch (Throwable t) {
            LOG.error("REALLIFE SELFTEST FAILED", t);
        }
        s.initiateShutdown(false);
    }

    private void run(MinecraftServer s) {
        ServerWorld ow = EconomyData.overworld(s);
        BlockPos base = new BlockPos(ow.getWorldInfo().getSpawnX(), 120, ow.getWorldInfo().getSpawnZ());

        Blueprint bank = Buildings.buildBank(ow, base, Direction.SOUTH);
        check(ow.getBlockState(bank.pos(0, 1, 5)).getBlock() == ModBlocks.BANK_COUNTER.get(), "bank counter placed");
        Blueprint mfo = Buildings.buildMfo(ow, base.east(40), Direction.WEST);
        check(ow.getBlockState(mfo.pos(0, 1, 5)).getBlock() == ModBlocks.MFO_COUNTER.get(), "mfo counter placed (rotated)");
        Direction[] dirs = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
        for (HouseType h : HouseType.values()) {
            Blueprint b = Buildings.buildHouse(ow, base.add(h.ordinal() * 40, 0, 80), dirs[h.ordinal() % 4], h);
            LOG.info("built " + h.name() + " box " + b.min() + " -> " + b.max());
            check(!ow.isAirBlock(b.pos(0, -1, 2)), h.name() + " has floor");
        }

        EconomyData data = EconomyData.get(s);
        DebtManager.ensureFacilities(s, data);
        check(ow.getBlockState(data.jailPos).getBlock() == ModBlocks.JAIL_WALL.get(), "jail built");
        check(ow.getBlockState(data.pitPos).getBlock() == ModBlocks.JAIL_WALL.get(), "pit built");

        // ---- экономика
        Account a = data.getOrCreate(UUID.randomUUID());
        long today = BankService.today(s);
        a.createdDay = today;
        int fresh = CreditRating.score(a, today);
        LOG.info("fresh score " + fresh + ", max loan " + CreditRating.maxLoan(a, today));
        check(fresh >= 450 && CreditRating.maxLoan(a, today) > 0, "new client can get a small loan");
        a.balance = 3000;
        a.lifetimeDeposited = 6000;
        int rich = CreditRating.score(a, today);
        check(rich > fresh, "savings and income raise score (" + rich + ")");

        a.loanPrincipal = 1000;
        a.loanRate = 0.01;
        a.loanDays = 10;
        a.balance = 250;
        double pay = a.loanPayment();
        LOG.info("loan payment " + Finance.money(pay));
        BankService.processDay(s, data);
        check(a.arrears < 0.01 && a.onTimePayments == 1, "first payment auto-debited");
        a.balance = 0;
        for (int d = 1; d <= 6; d++) {
            BankService.processDay(s, data);
            LOG.info("day " + d + ": arrears " + Finance.money(a.arrears) + ", overdue " + a.overdueDays + ", stage " + DebtManager.bankStage(a));
        }
        check(DebtManager.bankStage(a) == 3, "bank escalates to police");
        check(CreditRating.score(a, today) < rich, "late payments lower score");

        // ---- МФО
        Account m = data.getOrCreate(UUID.randomUUID());
        m.mfoDebt = 1000;
        m.mfoPrincipal = 1000;
        m.mfoDays = 2;
        for (int d = 1; d <= 6; d++) {
            BankService.processDay(s, data);
            LOG.info("mfo day " + d + ": debt " + Finance.money(m.mfoDebt) + ", stage " + DebtManager.mfoStage(m));
        }
        check(m.mfoDebt > 2000, "mfo compound interest grows");
        check(DebtManager.mfoStage(m) == 3, "mfo escalates to lethal");

        // ---- NPC
        EntityType<?>[] types = {ModEntities.ZHORA.get(), ModEntities.ALEXANDER.get(), ModEntities.POLICE.get(),
                ModEntities.VOVAN.get(), ModEntities.BRATOK.get()};
        for (EntityType<?> t : types) {
            DebtNpcEntity e = (DebtNpcEntity) t.create(ow);
            check(e != null, "create " + t.getRegistryName());
            e.setPosition(base.getX(), base.getY() + 2, base.getZ() + 3);
            ow.addEntity(e);
            check(e.getAttributeValue(Attributes.MAX_HEALTH) > 0, e.npcName() + " hp=" + e.getHealth());
            e.remove();
        }
        data.reset(a.id);
        data.reset(m.id);
    }
}
