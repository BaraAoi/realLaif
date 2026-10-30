package com.baraaoi.reallife.economy;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.container.BizContainer;
import com.baraaoi.reallife.entity.WorkerEntity;
import com.baraaoi.reallife.event.DebtManager;
import com.baraaoi.reallife.event.Visitors;
import com.baraaoi.reallife.item.BizPermitItem;
import com.baraaoi.reallife.item.DirtyMoneyItem;
import com.baraaoi.reallife.registry.ModEntities;
import com.baraaoi.reallife.registry.ModItems;
import com.baraaoi.reallife.structure.BizBuildings;
import com.baraaoi.reallife.structure.Blueprint;
import com.baraaoi.reallife.structure.Buildings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.Random;
import java.util.UUID;

/**
 * Легальный бизнес v5: заведения, наёмные работники, ежедневная прибыль «как повезёт»
 * и отмывание грязных денег через кассу (с риском налоговой проверки).
 */
public final class LegalBiz {
    private LegalBiz() {}

    public static final String BIZ = "§a[Бизнес] §f";
    public static final String TAX = "§6§l[ФНС] §r§f";
    private static final Random R = new Random();

    // ------------------------------------------------------------------ удача

    /** Итог дня: множитель базовой прибыли и описание. */
    public static final class Roll {
        public final double mult;
        public final String note;
        public final int kind;

        Roll(double mult, String note, int kind) {
            this.mult = mult;
            this.note = note;
            this.kind = kind;
        }
    }

    public static final int BAD = 0, NORMAL = 1, GOOD = 2, JACKPOT = 3;

    private static final String[] BAD_DAYS = {"СЭС закрыла на полдня", "сломался кассовый аппарат", "отключили свет",
            "ремонт дороги перед входом", "кассир перепутал сдачу", "пожарная проверка", "залило соседи сверху"};
    private static final String[] GOOD_DAYS = {"рядом открылась остановка", "блогер снял обзор", "праздники — очереди",
            "конкурент закрылся на ремонт", "хорошая погода", "день зарплаты в городе"};
    private static final String[] JACKPOT_DAYS = {"заведение попало в топ на картах", "свадьба сняла всё целиком",
            "ажиотаж: дефицит у конкурентов", "приехал городской фестиваль"};

    /** luckBonus — прибавка к множителю от эффекта «Удача» (минус — от «Невезения»). */
    public static Roll roll(Random r, double luckBonus) {
        double x = r.nextDouble();
        double m;
        String note;
        int kind;
        if (x < 0.10) {
            m = r.nextDouble() * 0.4;
            note = BAD_DAYS[r.nextInt(BAD_DAYS.length)];
            kind = BAD;
        } else if (x < 0.80) {
            m = 0.7 + r.nextDouble() * 0.6;
            note = "обычный день";
            kind = NORMAL;
        } else if (x < 0.95) {
            m = 1.3 + r.nextDouble() * 0.5;
            note = GOOD_DAYS[r.nextInt(GOOD_DAYS.length)];
            kind = GOOD;
        } else {
            m = 2.0 + r.nextDouble();
            note = JACKPOT_DAYS[r.nextInt(JACKPOT_DAYS.length)];
            kind = JACKPOT;
        }
        return new Roll(Math.max(0, m + luckBonus), note, kind);
    }

    public static double luckBonus(ServerPlayerEntity p) {
        if (p == null) return 0;
        double bonus = 0;
        EffectInstance luck = p.getActivePotionEffect(Effects.LUCK);
        if (luck != null) bonus += 0.3 * (luck.getAmplifier() + 1);
        EffectInstance bad = p.getActivePotionEffect(Effects.UNLUCK);
        if (bad != null) bonus -= 0.3 * (bad.getAmplifier() + 1);
        return bonus;
    }

    // ------------------------------------------------------------------ отмывание

    /** Безопасный дневной объём отмыва для заведения. */
    public static double safeLimit(BizType t) {
        return t.base * Config.LAUNDER_SAFE_SHARE.get();
    }

    /** Шанс, что налоговая заметит отмыв amount, если сегодня уже провели before. */
    public static double launderChance(int base, double before, double amount) {
        double share = Config.LAUNDER_SAFE_SHARE.get();
        double after = before + amount;
        if (after <= base * share + 1e-6) return 0;
        return Math.max(0, Math.min(0.95, (after / base - share) * Config.LAUNDER_RISK_SLOPE.get()));
    }

    // ------------------------------------------------------------------ покупка и стройка

    public static void buyPermit(ServerPlayerEntity p, Account a, int typeIdx) {
        BizType t = BizType.byId(typeIdx);
        if (t == null) return;
        if (a.captivity != Account.FREE) { BankService.msg(p, "§cВы сейчас не можете пользоваться услугами."); return; }
        if (!BankService.charge(p, a, t.price)) {
            BankService.msg(p, BIZ + "§cРазрешение на «" + t.title + "» стоит " + Finance.money(t.price) + ". Счёт + наличка — не хватает.");
            return;
        }
        BankService.give(p, BizPermitItem.create(ModItems.BIZ_PERMIT.get(), t));
        BankService.msg(p, BIZ + "§aРазрешение на «" + t.title + "» оформлено! §fПКМ им по ровной земле — бригада построит заведение 11×11. "
                + "Потом найми работника в кассе.");
    }

    /** Построить заведение по разрешению. true — построено. */
    public static boolean place(ServerPlayerEntity p, BlockPos origin, Direction facing, BizType type) {
        World world = p.world;
        MinecraftServer server = p.getServer();
        if (server == null) return false;
        if (world.getDimensionKey() != World.OVERWORLD) {
            BankService.msg(p, BIZ + "§cЗаведения строят только в обычном мире.");
            return false;
        }
        EconomyData data = EconomyData.get(server);
        String problem = BizBuildings.checkSite(world, origin, facing, data, p.getUniqueID());
        if (problem != null) {
            BankService.msg(p, BIZ + "§cБригада отказалась строить: §f" + problem);
            return false;
        }
        LegalBusiness b = register(data, world, origin, facing, type, p.getUniqueID(), p.getGameProfile().getName());
        int claimed = Buildings.claimArea(data, p.getUniqueID(), b.min.add(-4, 0, -4), b.max.add(4, 0, 4), true);
        data.markDirty();
        BankService.msg(p, BIZ + "§aОткрыто заведение «" + type.title + "»! §fБазовая прибыль §a" + Finance.money(type.base)
                + "§f/день. §eНайми работника в кассе (ПКМ по кассе)§f — без него заведение закрыто.");
        if (claimed > 0) BankService.msg(p, "§3[Администрация] §fЗемля под заведением оформлена на вас: §e" + claimed + "§f участк(а/ов).");
        return true;
    }

    /** Построить и зарегистрировать (без проверок; используется и самотестом). */
    public static LegalBusiness register(EconomyData data, World world, BlockPos origin, Direction facing, BizType type, UUID owner, String ownerName) {
        Blueprint bp = BizBuildings.build(world, origin, facing, type);
        LegalBusiness b = new LegalBusiness();
        b.id = data.nextBizId++;
        b.owner = owner;
        b.ownerName = ownerName == null ? "" : ownerName;
        b.type = type;
        b.counter = bp.pos(BizBuildings.COUNTER[0], BizBuildings.COUNTER[1], BizBuildings.COUNTER[2]);
        b.workerPos = bp.pos(BizBuildings.WORKER[0], BizBuildings.WORKER[1], BizBuildings.WORKER[2]);
        BlockPos[] box = BizBuildings.box(world, origin, facing);
        b.min = box[0];
        b.max = box[1];
        data.businesses.add(b);
        data.markDirty();
        return b;
    }

    // ------------------------------------------------------------------ работник

    public static WorkerEntity spawnWorker(ServerWorld w, LegalBusiness b) {
        if (b.workerId != null) {
            Entity old = w.getEntityByUuid(b.workerId);
            if (old != null) old.remove();
        }
        WorkerEntity e = ModEntities.WORKER.get().create(w);
        if (e == null) return null;
        double dx = b.counter.getX() - b.workerPos.getX(), dz = b.counter.getZ() - b.workerPos.getZ();
        float yaw = (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        e.setLocationAndAngles(b.workerPos.getX() + 0.5, b.workerPos.getY(), b.workerPos.getZ() + 0.5, yaw, 0F);
        e.rotationYawHead = yaw;
        e.renderYawOffset = yaw;
        e.setBizType(b.type);
        e.bizId = b.id;
        e.setCustomName(new StringTextComponent("§a" + b.type.workerName));
        e.setCustomNameVisible(true);
        w.addEntity(e);
        b.workerId = e.getUniqueID();
        b.worker = true;
        return e;
    }

    static void hire(ServerPlayerEntity p, Account a, LegalBusiness b) {
        ServerWorld w = p.getServerWorld();
        if (b.worker && b.workerId != null && w.getEntityByUuid(b.workerId) != null) {
            BankService.msg(p, BIZ + "Работник уже на месте: " + b.type.workerName + ".");
            return;
        }
        int cost = b.type.hireCost();
        if (!BankService.charge(p, a, cost)) {
            BankService.msg(p, BIZ + "§cНайм стоит " + Finance.money(cost) + " (подъёмные). Не хватает.");
            return;
        }
        if (spawnWorker(w, b) == null) {
            a.balance += cost;
            return;
        }
        EconomyData.get(p.getServer()).markDirty();
        BankService.msg(p, BIZ + "§aНанят работник: " + b.type.workerName + ". §fЗарплата " + Finance.money(b.type.wage())
                + "/день — вычитается из выручки. Береги его: убьют — нанимай заново.");
    }

    public static void onWorkerDeath(WorkerEntity e) {
        MinecraftServer server = e.getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        LegalBusiness b = data.bizById(e.bizId);
        if (b == null || b.workerId == null || !b.workerId.equals(e.getUniqueID())) return;
        b.worker = false;
        b.workerId = null;
        data.markDirty();
        ServerPlayerEntity owner = b.owner == null ? null : server.getPlayerList().getPlayerByUUID(b.owner);
        if (owner != null) BankService.msg(owner, BIZ + "§cСМС: «" + b.type.workerName + " из «" + b.type.title
                + "» погиб. Заведение закрыто, пока не наймёшь нового (ПКМ по кассе).»");
    }

    public static void workerTalk(ServerPlayerEntity p, WorkerEntity e) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        LegalBusiness b = EconomyData.get(server).bizById(e.bizId);
        String[] lines = {"Здравствуйте! Чем могу помочь?", "Касса справа, карты принимаем.", "Акция: второй товар — за полную цену!",
                "Директор сказал улыбаться. Я улыбаюсь.", "Скоро обед, приходите после."};
        String text = lines[R.nextInt(lines.length)];
        if (b != null && p.getUniqueID().equals(b.owner)) {
            text = "Шеф! Вчера вышло " + Finance.money(b.lastProfit) + " (" + (b.lastNote.isEmpty() ? "ещё не считали" : b.lastNote) + ").";
        }
        p.sendStatusMessage(new StringTextComponent("§7<§a" + e.getBizType().workerName + "§7> §f" + text), false);
    }

    // ------------------------------------------------------------------ касса

    public static void openCounter(ServerPlayerEntity p, BlockPos pos) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        LegalBusiness b = data.bizAtCounter(pos);
        if (b == null) {
            BankService.msg(p, BIZ + "§7Касса не зарегистрирована ни за кем.");
            return;
        }
        if (!p.getUniqueID().equals(b.owner)) {
            BankService.msg(p, BIZ + "«" + b.type.title + "». Владелец: §e" + (b.ownerName.isEmpty() ? "?" : b.ownerName)
                    + "§f. " + (b.worker ? "§aОткрыто." : "§cЗакрыто — нет работника."));
            return;
        }
        final BlockPos at = pos.toImmutable();
        NetworkHooks.openGui(p, new SimpleNamedContainerProvider((id, inv, pl) -> new BizContainer(id, inv, at),
                new StringTextComponent(b.type.title)), at);
        BankService.sync(p);
    }

    /** Данные открытой кассы для окна. */
    public static CompoundNBT info(LegalBusiness b, long today) {
        CompoundNBT t = new CompoundNBT();
        t.putInt("id", b.id);
        t.putInt("type", b.type.ordinal());
        t.putBoolean("worker", b.worker);
        t.putDouble("lastProfit", b.lastProfit);
        t.putString("lastNote", b.lastNote);
        t.putDouble("total", b.totalProfit);
        t.putInt("days", b.days);
        t.putDouble("laundered", b.launderedOn(today));
        t.putDouble("safe", safeLimit(b.type));
        t.putInt("x", b.counter.getX());
        t.putInt("y", b.counter.getY());
        t.putInt("z", b.counter.getZ());
        return t;
    }

    public static ListNBT infoList(EconomyData data, UUID owner, long today) {
        ListNBT l = new ListNBT();
        for (LegalBusiness b : data.bizOf(owner)) l.add(info(b, today));
        return l;
    }

    static void counterAction(ServerPlayerEntity p, Account a, BizContainer c, int action) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        LegalBusiness b = data.bizAtCounter(c.getPos());
        if (b == null || !p.getUniqueID().equals(b.owner)) return;
        if (action == Actions.BIZ_HIRE) hire(p, a, b);
        else if (action == Actions.BIZ_LAUNDER) launder(p, a, b, c);
    }

    static void launder(ServerPlayerEntity p, Account a, LegalBusiness b, BizContainer c) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        if (a.captivity != Account.FREE) return;
        ItemStack st = c.getInput().getStackInSlot(0);
        if (st.isEmpty() || !(st.getItem() instanceof DirtyMoneyItem)) { BankService.msg(p, BIZ + "Положи грязные деньги в слот кассы."); return; }
        if (!b.worker) { BankService.msg(p, BIZ + "§cЗаведение закрыто — без работника выручку не проведёшь. Найми работника."); return; }
        EconomyData data = EconomyData.get(server);
        long today = BankService.today(server);
        double amount = ItemValues.stackValue(st);
        double before = b.launderedOn(today);
        double chance = launderChance(b.type.base, before, amount);
        b.launderDay = today;
        b.launderedToday = before + amount;
        c.getInput().setInventorySlotContents(0, ItemStack.EMPTY);
        data.markDirty();
        if (R.nextDouble() < chance) {
            caught(server, data, p, a, b, amount);
            return;
        }
        double tax = Config.LAUNDER_TAX.get();
        double clean = amount * (1 - tax);
        a.balance += clean;
        BankService.msg(p, BIZ + "Выручка проведена через кассу «" + b.type.title + "»: §2" + Finance.money(amount) + "§f → §a+"
                + Finance.money(clean) + "§f на счёт (НДФЛ " + Finance.percent(tax) + "). Сегодня проведено: "
                + Finance.money(b.launderedToday) + " из безопасных " + Finance.money(safeLimit(b.type)) + ".");
        if (chance > 0) BankService.msg(p, BIZ + "§eПронесло. Риск был " + Finance.percent(chance) + " — налоговая могла заметить.");
    }

    static void caught(MinecraftServer server, EconomyData data, ServerPlayerEntity p, Account a, LegalBusiness b, double amount) {
        double fine = Config.TAX_FINE_MULT.get() * (b.type.base + amount);
        p.closeScreen();
        BankService.msg(p, TAX + "§cНалоговая проверка в «" + b.type.title + "»! §fВыручка не сходится с чеками: "
                + Finance.money(amount) + " «из воздуха».");
        BankService.msg(p, TAX + "Грязные деньги изъяты. Штраф 200% от (прибыли заведения + суммы): §c" + Finance.money(fine) + "§f.");
        Visitors.forceCharge(p, a, fine);
        data.markDirty();
        int ticks = Config.TAX_JAIL_TICKS.get();
        if (ticks > 0) {
            DebtManager.jailFor(server, data, p, a, ticks, "отмывание денег через «" + b.type.title + "»");
        }
        for (ServerPlayerEntity other : server.getPlayerList().getPlayers()) {
            if (other != p) BankService.msg(other, "§9[Новости] §fНалоговая накрыла «" + b.type.title + "» игрока §e"
                    + p.getGameProfile().getName() + "§f: отмывание денег.");
        }
    }

    // ------------------------------------------------------------------ день

    public static void processDay(MinecraftServer server, EconomyData data) {
        ServerWorld ow = EconomyData.overworld(server);
        for (LegalBusiness b : data.businesses) {
            if (b.owner == null) continue;
            Account a = data.getOrCreate(b.owner);
            ServerPlayerEntity p = server.getPlayerList().getPlayerByUUID(b.owner);
            if (b.worker && b.workerId != null && ow.isBlockLoaded(b.workerPos) && ow.getEntityByUuid(b.workerId) == null) {
                b.worker = false;
                b.workerId = null;
            }
            if (!b.worker) {
                b.lastProfit = 0;
                b.lastNote = "закрыто: нет работника";
                if (p != null) BankService.msg(p, BIZ + "§cСМС: «" + b.type.title + "» сегодня не работало — нет работника.");
                continue;
            }
            Roll r = roll(R, luckBonus(p));
            double revenue = Math.round(b.type.base * r.mult);
            double profit = revenue - b.type.wage();
            a.balance += profit;
            b.lastProfit = profit;
            b.lastNote = r.note;
            b.totalProfit += profit;
            b.days++;
            if (p != null) {
                String color = r.kind == BAD ? "§c" : r.kind == NORMAL ? "§f" : r.kind == GOOD ? "§a" : "§6§l";
                BankService.msg(p, BIZ + "СМС «" + b.type.title + "»: " + color + r.note + "§f. Выручка §a" + Finance.money(revenue)
                        + "§f, зарплата §c−" + Finance.money(b.type.wage()) + "§f → итог " + (profit >= 0 ? "§a+" : "§c")
                        + Finance.money(profit) + "§f на счёт.");
            }
        }
        data.markDirty();
    }
}
