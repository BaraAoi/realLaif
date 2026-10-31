package com.baraaoi.reallife.event;

import com.baraaoi.reallife.Config;
import com.baraaoi.reallife.container.FinanceContainer;
import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.economy.HouseType;
import com.baraaoi.reallife.entity.*;
import com.baraaoi.reallife.registry.ModEntities;
import com.baraaoi.reallife.registry.ModItems;
import com.baraaoi.reallife.structure.Blueprint;
import com.baraaoi.reallife.structure.Buildings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * «Терпение банка» и «методы» МФО: стадии взыскания, NPC, арест, тюрьма, подвал.
 */
public class DebtManager {
    private static final Random RANDOM = new Random();
    private static final List<Regen> REGEN = new ArrayList<>();

    private static class Regen {
        final BlockPos pos;
        final BlockState state;
        final long due;

        Regen(BlockPos pos, BlockState state, long due) {
            this.pos = pos;
            this.state = state;
            this.due = due;
        }
    }

    /** Снос участка: 8 слоёв за тик сверху вниз (коренная порода остаётся). */
    private static class Eviction {
        final int cx, cz;
        int y;

        Eviction(int cx, int cz, int y) {
            this.cx = cx;
            this.cz = cz;
            this.y = y;
        }
    }

    private static final List<Eviction> EVICTIONS = new ArrayList<>();

    /** Стереть слои [yTo..yFrom] чанка (без выпадения вещей, бедрок не трогаем). */
    public static void wipeLayers(ServerLevel w, int cx, int cz, int yFrom, int yTo) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = yFrom; y >= yTo; y--)
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++) {
                    m.set((cx << 4) + x, y, (cz << 4) + z);
                    BlockState st = w.getBlockState(m);
                    if (st.isAir() || st.getBlock() == Blocks.BEDROCK) continue;
                    net.minecraft.world.level.block.entity.BlockEntity te = w.getBlockEntity(m);
                    if (te instanceof net.minecraft.world.Container) ((net.minecraft.world.Container) te).clearContent();
                    w.setBlock(m, air, 2 | 16);
                }
    }

    private static void tickEvictions(ServerLevel ow) {
        if (EVICTIONS.isEmpty()) return;
        Eviction e = EVICTIONS.get(0);
        int to = Math.max(0, e.y - 7);
        wipeLayers(ow, e.cx, e.cz, e.y, to);
        e.y = to - 1;
        if (e.y < 0) EVICTIONS.remove(0);
    }

    // ================================================================== стадии

    /** 0 — всё хорошо, 1 — Жора, 2 — Александр, 3 — полиция. */
    public static int bankStage(Account a) {
        if (a.captivity != Account.FREE) return 0;
        if (a.wanted) return 3;
        if (a.arrears <= 0.01) return 0;
        if (a.overdueDays >= Config.POLICE_DAYS.get()) return 3;
        if (a.overdueDays >= Config.ALEXANDER_DAYS.get() || a.laborBlocksLeft > 0) return 2;
        if (a.overdueDays >= Config.ZHORA_DAYS.get()) return 1;
        return 0;
    }

    /** 0 — всё хорошо, 1 — Вован, 2 — братки (бьют и грабят), 3 — братки с паяльником. */
    public static int mfoStage(Account a) {
        if (a.captivity != Account.FREE || !a.hasMfo()) return 0;
        if (a.mfoLethal || a.mfoOverdueDays >= Config.LETHAL_DAYS.get()) return 3;
        if (a.mfoOverdueDays >= Config.BRATKI_DAYS.get()) return 2;
        if (a.mfoOverdueDays >= Config.VOVAN_DAYS.get()) return 1;
        return 0;
    }

    // ================================================================== тик сервера

    public static void serverTick(MinecraftServer server) {
        ServerLevel ow = EconomyData.overworld(server);
        if (ow == null) return;
        long now = ow.getGameTime();
        EconomyData data = EconomyData.get(server);

        if (!REGEN.isEmpty()) {
            Iterator<Regen> it = REGEN.iterator();
            while (it.hasNext()) {
                Regen r = it.next();
                if (now >= r.due) {
                    if (ow.isEmptyBlock(r.pos)) ow.setBlock(r.pos, r.state, 3);
                    it.remove();
                }
            }
        }

        tickEvictions(ow);

        if (now % 20 != 0) return;

        tickAlarms(server, data, now);
        if (now % 600 == 0) tickCashiers(data, ow, now);
        if (now % 100 == 0) clearCityMobs(data, ow);

        tickLand(server, data, ow, now);

        // смена игрового дня
        long day = ow.getOverworldClockTime() / 24000L;
        if (data.lastDay < 0 || day < data.lastDay) {
            data.lastDay = day;
            data.setDirty();
        }
        int guard = 0;
        while (data.lastDay < day && guard++ < 30) {
            data.lastDay++;
            BankService.processDay(server, data);
        }

        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (p.containerMenu instanceof FinanceContainer || p.containerMenu instanceof com.baraaoi.reallife.container.CasinoContainer
                    || p.containerMenu instanceof com.baraaoi.reallife.container.BizContainer
                    || p.containerMenu instanceof com.baraaoi.reallife.container.CryptoContainer) BankService.sync(p);
            if ((now / 20 + p.getId()) % 2 == 0) tickPlayer(server, data, p, now);
        }
    }

    private static void tickPlayer(MinecraftServer server, EconomyData data, ServerPlayer p, long now) {
        Account a = data.get(p);
        if (a.captivity != Account.FREE) {
            tickCaptivity(server, data, p, a, now);
            return;
        }
        if (p.isSpectator() || !p.isAlive()) return;
        ServerLevel w = p.level();
        List<DebtNpcEntity> mine = findNpcs(w, p);

        // ---------------- ограбление банка: розыск на сутки
        if (a.robber && now >= a.robberyUntil) {
            a.robber = false;
            if (a.arrears <= 0.01) {
                a.wanted = false;
                a.policeWave = 0;
                a.nextPoliceTick = 0;
            }
            data.setDirty();
            BankService.msg(p, "§9[Полиция] §7Дело об ограблении банка приостановлено за недостатком улик.");
        }

        // ---------------- самострой
        if (a.landCase && count(mine, InspectorEntity.class) == 0 && w.dimension() == Level.OVERWORLD) {
            spawn(ModEntities.INSPECTOR.get(), w, p, 5, 9);
        }

        // ---------------- v1.8: автокредит (счёт в минусе / штрафы / просрочка)
        if (a.arrears > 0.01 || a.balance < -0.5) {
            BankService.autoCredit(p, a, BankService.today(server));
            data.setDirty();
        }

        // ---------------- банк
        int bs = bankStage(a);
        if (bs == 3 && !a.wanted) {
            a.wanted = true;
            data.setDirty();
            BankService.msg(p, BankService.BANK + "§4Терпение банка лопнуло. Ваше дело передано в полицию. За вами выехали!");
        }
        if (bs == 1) {
            if (count(mine, ZhoraEntity.class) == 0) {
                spawn(ModEntities.ZHORA.get(), w, p, 8, 14);
                BankService.msg(p, BankService.BANK + "§eК вам направлен сотрудник банка Жора. Он просто хочет поговорить.");
            }
        } else if (bs == 2) {
            if (a.laborBlocksLeft <= 0) {
                a.laborBlocksLeft = (int) Math.max(24, Math.min(256, Math.ceil(a.arrears / 4.0)));
                a.laborPerBlock = a.arrears / a.laborBlocksLeft;
                a.lastLaborTick = now;
                data.setDirty();
                if (!hasItem(p, ModItems.DEBTOR_PICKAXE.get().getDefaultInstance())) {
                    BankService.give(p, new ItemStack(ModItems.DEBTOR_PICKAXE.get()));
                }
                BankService.msg(p, BankService.BANK + "§6Приехал Александр из службы взыскания. Вам выдана кирка. "
                        + "Выкопайте §c" + a.laborBlocksLeft + "§6 блоков камня/земли/руды — и долг будет отработан. Добыча идёт банку.");
            }
            if (count(mine, AlexanderEntity.class) == 0) spawn(ModEntities.ALEXANDER.get(), w, p, 6, 10);
        } else if (bs == 3) {
            p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            int cops = count(mine, PoliceEntity.class);
            if (cops == 0) {
                if ((a.policeWave == 0 && a.nextPoliceTick == 0) || (a.nextPoliceTick > 0 && now >= a.nextPoliceTick)) {
                    int n = Math.min(16, Config.POLICE_COUNT.get() + 2 * a.policeWave);
                    for (int i = 0; i < n; i++) spawn(ModEntities.POLICE.get(), w, p, 10, 18);
                    a.policeWave++;
                    a.nextPoliceTick = 0;
                    data.setDirty();
                    BankService.msg(p, "§9§l[Полиция] §r§cВы окружены! " + n + " сотрудников. Сопротивление бесполезно!");
                } else if (a.nextPoliceTick == 0) {
                    a.nextPoliceTick = now + 1200;
                    data.setDirty();
                    BankService.msg(p, "§9§l[Полиция] §r§cВызвано подкрепление. Через минуту будут здесь.");
                }
            }
        }

        // ---------------- МФО
        int ms = mfoStage(a);
        if (ms == 1) {
            if (count(mine, VovanEntity.class) == 0) {
                spawn(ModEntities.VOVAN.get(), w, p, 8, 14);
                BankService.msg(p, BankService.MFO + "§cК тебе выехал наш специалист по возврату. Вован. Не зли его.");
            }
        } else if (ms >= 2) {
            int bros = count(mine, BratokEntity.class);
            if (bros == 0 && now >= a.nextBratkiTick) {
                int n = ms == 3 ? 4 : 3;
                for (int i = 0; i < n; i++) spawn(ModEntities.BRATOK.get(), w, p, 8, 14);
                a.nextBratkiTick = now + (ms == 3 ? 1200 : 3600);
                data.setDirty();
                if (ms == 3) {
                    BankService.msg(p, BankService.MFO + "§4§lПриехали братки с паяльником. Беги. Хотя... не поможет.");
                } else {
                    BankService.msg(p, BankService.MFO + "§cК тебе приехали братки. Разговор будет коротким.");
                }
                if (!a.mfoBurned) {
                    a.mfoBurned = true;
                    arson(server, a, p, false);
                }
            }
        }
        if (ms >= 1 && now - a.lastSms > 2400) {
            a.lastSms = now;
            String[] sms = {"Верни долг §c%s§f, или будет хуже.", "Мы знаем, где ты живёшь. Долг: §c%s",
                    "Твоя мама в курсе, что ты должен §c%s§f?", "Последнее предупреждение! §c%s"};
            BankService.msg(p, "§8[СМС от «Шайкин и Ко»] §f" + String.format(sms[RANDOM.nextInt(sms.length)], Finance.money(a.mfoDebt)));
        }
    }

    // ================================================================== NPC

    public static List<DebtNpcEntity> findNpcs(ServerLevel w, Player p) {
        return w.getEntitiesOfClass(DebtNpcEntity.class, p.getBoundingBox().inflate(96.0D),
                e -> p.getUUID().equals(e.getTargetId()));
    }

    private static int count(List<DebtNpcEntity> list, Class<?> cls) {
        int n = 0;
        for (DebtNpcEntity e : list) if (cls.isInstance(e) && e.isAlive()) n++;
        return n;
    }

    private static boolean hasItem(Player p, ItemStack like) {
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) if (s.getItem() == like.getItem()) return true;
        return false;
    }

    public static <T extends DebtNpcEntity> T spawn(EntityType<T> type, ServerLevel w, Player p, int minR, int maxR) {
        T e = type.create(w, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (e == null) return null;
        BlockPos pos = findSpawnPos(w, p, minR, maxR);
        e.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, RANDOM.nextFloat() * 360F, 0F);
        e.setDebtor(p);
        e.setCustomName(Component.literal(e.npcName()));
        e.setCustomNameVisible(true);
        w.addFreshEntity(e);
        return e;
    }

    public static void teleportNear(Entity e, Player p, int r) {
        if (!(e.level() instanceof ServerLevel)) return;
        BlockPos pos = findSpawnPos((ServerLevel) e.level(), p, 2, r);
        e.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    public static BlockPos findSpawnPos(ServerLevel w, Player p, int minR, int maxR) {
        BlockPos base = p.blockPosition();
        for (int i = 0; i < 24; i++) {
            double ang = RANDOM.nextDouble() * Math.PI * 2;
            int r = minR + RANDOM.nextInt(Math.max(1, maxR - minR + 1));
            int x = base.getX() + (int) Math.round(Math.cos(ang) * r);
            int z = base.getZ() + (int) Math.round(Math.sin(ang) * r);
            for (int dy = 0; dy <= 8; dy++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    BlockPos pos = new BlockPos(x, base.getY() + dy * sign, z);
                    if (w.isEmptyBlock(pos) && w.isEmptyBlock(pos.above()) && w.getBlockState(pos.below()).isSolid()) {
                        return pos;
                    }
                }
            }
        }
        if (w.dimension() == Level.OVERWORLD) {
            int x = base.getX() + 3, z = base.getZ() + 3;
            int y = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - base.getY()) < 6) return new BlockPos(x, y, z);
        }
        return base;
    }

    // ================================================================== отработка долга копанием

    /** @return true если событие разрушения надо отменить. */
    public static boolean onBreak(ServerPlayer p, BlockPos pos, BlockState state) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return false;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(p);
        ServerLevel ow = EconomyData.overworld(server);
        long now = ow.getGameTime();
        if (a.captivity == Account.PIT) {
            if (p.level() == ow && a.cellActive && Buildings.inPitMine(a.cellCenter, pos) && !state.isAir()) {
                // забой: блок ломается как обычно (с добычей), через 5 сек порода нарастает снова
                REGEN.add(new Regen(pos.immutable(), Buildings.pitBlock(RANDOM), now + 100));
                return false;
            }
            return true;
        }
        if (a.captivity == Account.JAIL) return true;

        if (bankStage(a) == 2 && a.laborBlocksLeft > 0) {
            boolean diggable = state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                    || state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)
                    || state.is(net.minecraftforge.common.Tags.Blocks.ORES);
            if (diggable && state.getDestroySpeed(p.level(), pos) >= 0) {
                p.level().removeBlock(pos, false);
                double bonus = com.baraaoi.reallife.economy.ItemValues.laborValue(state);
                a.arrears -= a.laborPerBlock + bonus;
                a.laborBlocksLeft--;
                a.lastLaborTick = now;
                data.setDirty();
                if (bonus > 0) BankService.msg(p, "§7<§6Александр§7> §fО, руда! Это банку. Зачтено ещё " + Finance.money(bonus) + ".");
                if (a.laborBlocksLeft <= 0 || a.arrears <= 0.01) {
                    a.clearArrears();
                    BankService.msg(p, BankService.BANK + "§aДолг отработан! Просрочка списана.");
                } else {
                    BankService.actionBar(p, "§6Отработка долга: §fосталось §c" + a.laborBlocksLeft
                            + "§f блоков, просрочка §c" + Finance.money(a.arrears));
                }
                return true;
            }
        }
        return false;
    }

    // ================================================================== смерть, арест, похищение

    public static void onPlayerDeath(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(p);
        if (a.captivity != Account.FREE) return;
        if (bankStage(a) == 3) arrest(server, data, p, a);
        else if (mfoStage(a) == 3) kidnap(server, data, p, a);
    }

    public static void onNpcKilled(DebtNpcEntity npc, ServerPlayer killer) {
        MinecraftServer server = killer.level().getServer();
        if (server == null || npc.getTargetId() == null) return;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(npc.getTargetId());
        if (a == null) return;
        Player target = server.getPlayerList().getPlayer(a.id);
        if (npc instanceof ZhoraEntity || npc instanceof AlexanderEntity) {
            if (!a.wanted) {
                a.wanted = true;
                if (target != null) BankService.msg(target, BankService.BANK
                        + "§4Нападение на сотрудника банка! Дело передано в полицию. За вами уже выехали.");
            }
        } else if (npc instanceof VovanEntity || (npc instanceof BratokEntity && mfoStage(a) == 2)) {
            if (!a.mfoLethal) {
                a.mfoLethal = true;
                a.nextBratkiTick = 0;
                if (target != null) BankService.msg(target, BankService.MFO
                        + "§4Ты обидел наших пацанов. Теперь разговор будет другой. Жди гостей с паяльником.");
            }
        }
        data.setDirty();
    }

    private static void removeNpcs(MinecraftServer server, Player p, boolean bank) {
        for (ServerLevel w : server.getAllLevels()) {
            for (DebtNpcEntity e : w.getEntitiesOfClass(DebtNpcEntity.class, p.getBoundingBox().inflate(160.0D),
                    e -> p.getUUID().equals(e.getTargetId()))) {
                boolean isBank = e instanceof ZhoraEntity || e instanceof AlexanderEntity || e instanceof PoliceEntity;
                if (isBank == bank) e.discard();
            }
        }
    }

    public static void arrest(MinecraftServer server, EconomyData data, ServerPlayer p, Account a) {
        ServerLevel ow = EconomyData.overworld(server);
        long now = ow.getGameTime();
        double debt = a.arrears + a.loanPrincipal + a.mortPrincipal;
        double seizedCash = Math.max(0, a.balance);
        double seizedItems = BankService.inventoryValue(p);
        p.getInventory().clearContent();
        a.balance = Math.min(0, a.balance);
        String house = a.hasHouse ? houseTitle(a) : null;
        demolishHouse(ow, a);
        a.clearLoan();
        a.clearMortgage();
        a.pendingHouse = -1;
        a.houseOrderId = 0;
        a.clearArrears();
        a.wanted = false;
        a.robber = false;
        a.policeWave = 0;
        a.nextPoliceTick = 0;
        a.defaults++;
        a.captivity = Account.JAIL;
        a.jailUntil = now + Config.JAIL_TICKS.get();
        a.policeBribe = 0;
        a.landCase = false;
        int land = data.ownedCount(a.id);
        data.landOwners.values().removeIf(a.id::equals);
        data.setDirty();
        removeNpcs(server, p, true);
        buildCell(server, data, a, p.blockPosition());
        BankService.msg(p, "§9§l[Полиция] §r§cВы арестованы за злостное уклонение от уплаты долга.");
        BankService.msg(p, "§7Конфисковано: всё имущество (≈" + Finance.money(seizedItems) + "), счёт " + Finance.money(seizedCash)
                + (house != null ? ", дом «" + house + "»" : "") + (land > 0 ? ", участков: " + land : "") + ". Долг " + Finance.money(debt) + " закрыт за счёт конфискации.");
        BankService.msg(p, "§7Срок: " + (Config.JAIL_TICKS.get() / 1200) + " мин. Кредитная история испорчена.");
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other != p) BankService.msg(other, "§9[Новости] §fИгрок §e" + p.getGameProfile().name()
                    + "§f арестован за долги перед «Реал Банком».");
        }
    }

    /** Короткое СИЗО без конфискации (ОБЭП, налоговая): срок ticks, потом автоматически на свободу. */
    public static void jailFor(MinecraftServer server, EconomyData data, ServerPlayer p, Account a, int ticks, String reason) {
        if (a.captivity != Account.FREE || ticks <= 0) return;
        long now = EconomyData.overworld(server).getGameTime();
        a.captivity = Account.JAIL;
        a.jailUntil = now + ticks;
        data.setDirty();
        buildCell(server, data, a, p.blockPosition());
        sendToCaptivity(server, data, p, a);
        BankService.msg(p, "§9§l[Полиция] §r§cВы помещены в СИЗО: " + reason + ". §7Срок: " + Math.max(1, ticks / 1200) + " мин.");
    }

    public static void kidnap(MinecraftServer server, EconomyData data, ServerPlayer p, Account a) {
        double debt = a.mfoDebt;
        p.getInventory().clearContent();
        double taken = Math.max(0, a.balance) * 0.5;
        a.balance -= taken;
        arson(server, a, p, true);
        a.pitBlocksLeft = 0;
        a.pitDebtLeft = Math.max(150, Math.min(1500, Math.ceil(debt * 0.25 / 10.0) * 10.0));
        a.clearMfo();
        a.captivity = Account.PIT;
        data.setDirty();
        removeNpcs(server, p, false);
        buildCell(server, data, a, p.blockPosition());
        BankService.msg(p, BankService.MFO + "§4Очнулся? Ты в подвале у Шайкина. Вещи твои теперь наши, со счёта ты «добровольно» перевёл "
                + Finance.money(taken) + ", а дом... ну, бывает, горит.");
        BankService.give(p, new ItemStack(ModItems.DEBTOR_PICKAXE.get()));
        BankService.msg(p, BankService.MFO + "§cВот кирка. Копай руду, камень, землю и сдавай всё в приёмник. Наберёшь на §4"
                + Finance.money(a.pitDebtLeft) + "§c — отпустим, долг " + Finance.money(debt) + " будет закрыт.");
    }

    private static String houseTitle(Account a) {
        HouseType t = HouseType.byId(a.houseType);
        return t == null ? "дом" : t.title;
    }

    /** Изъятие дома банком: дом сносится полностью. */
    public static void demolishHouse(ServerLevel ow, Account a) {
        if (!a.hasHouse) return;
        BlockPos min = a.houseMin, max = a.houseMax;
        long volume = (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        if (volume > 0 && volume < 20000) {
            // сверху вниз, чтобы ничего не осыпалось
            for (int y = max.getY(); y >= min.getY(); y--)
                for (int x = min.getX(); x <= max.getX(); x++)
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        ow.removeBlockEntity(pos);
                        ow.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
            BlockPos sign = new BlockPos((min.getX() + max.getX()) / 2, min.getY(), min.getZ());
            ow.setBlock(sign, Blocks.OAK_SIGN.defaultBlockState(), 3);
            net.minecraft.world.level.block.entity.BlockEntity te = ow.getBlockEntity(sign);
            if (te instanceof net.minecraft.world.level.block.entity.SignBlockEntity) {
                net.minecraft.world.level.block.entity.SignBlockEntity s = (net.minecraft.world.level.block.entity.SignBlockEntity) te;
                s.updateText(_t -> _t.setMessage(0, Component.literal("ИЗЪЯТО")), true);
                s.updateText(_t -> _t.setMessage(1, Component.literal("«Реал Банком»")), true);
                s.updateText(_t -> _t.setMessage(2, Component.literal("за долги")), true);
            }
        }
        a.hasHouse = false;
        a.houseType = -1;
        net.minecraft.world.phys.AABB houseBox = new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(min), net.minecraft.world.phys.Vec3.atLowerCornerOf(max.offset(1, 1, 1)));
        if (houseBox.contains(a.businessChest.getX() + 0.5, a.businessChest.getY() + 0.5, a.businessChest.getZ() + 0.5)) {
            a.businessChest = BlockPos.ZERO;
            a.chestGiven = false; // сундук цеха изъят вместе с домом — со следующим станком дадут новый
        }
        if (houseBox.contains(a.launderPos.getX() + 0.5, a.launderPos.getY() + 0.5, a.launderPos.getZ() + 0.5)) a.launderPos = BlockPos.ZERO;
        a.printerList.removeIf(pp -> houseBox.contains(pp.getX() + 0.5, pp.getY() + 0.5, pp.getZ() + 0.5));
        if (a.hasBusiness && !a.businessMin.equals(BlockPos.ZERO)) {
            // старый подвал засыпают; станки, стоявшие в доме, снесены вместе с ним (вычистятся из списка сами)
            Buildings.fillBasement(ow, a.businessMin, a.businessMax);
            if (new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(a.businessMin), net.minecraft.world.phys.Vec3.atLowerCornerOf(a.businessMax.offset(1, 1, 1))).contains(
                    a.businessChest.getX() + 0.5, a.businessChest.getY() + 0.5, a.businessChest.getZ() + 0.5)) {
                a.businessChest = BlockPos.ZERO;
                a.chestGiven = false; // старый сундук засыпан — при следующей покупке станка дадут «Сундук цеха»
            }
            a.businessMin = BlockPos.ZERO;
            a.businessMax = BlockPos.ZERO;
        }
    }

    /** Поджог от братков. full=true — сжечь дом по-настоящему. */
    public static void arson(MinecraftServer server, Account a, Player p, boolean full) {
        ServerLevel ow = EconomyData.overworld(server);
        if (a.hasHouse) {
            BlockPos min = a.houseMin, max = a.houseMax;
            int fires = full ? 60 : 8;
            for (int i = 0; i < fires * 6 && fires > 0; i++) {
                int x = min.getX() + RANDOM.nextInt(max.getX() - min.getX() + 1);
                int y = min.getY() + RANDOM.nextInt(max.getY() - min.getY() + 1);
                int z = min.getZ() + RANDOM.nextInt(max.getZ() - min.getZ() + 1);
                BlockPos pos = new BlockPos(x, y, z);
                if (ow.isEmptyBlock(pos) && ow.getBlockState(pos.below()).isSolid()) {
                    ow.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                    fires--;
                }
                if (full && ow.getBlockState(pos).getSoundType() == net.minecraft.world.level.block.SoundType.GLASS) {
                    ow.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            if (full) {
                // братки забирают всё, что лежало в сундуках
                for (int x = min.getX(); x <= max.getX(); x++)
                    for (int y = min.getY(); y <= max.getY(); y++)
                        for (int z = min.getZ(); z <= max.getZ(); z++) {
                            net.minecraft.world.level.block.entity.BlockEntity te = ow.getBlockEntity(new BlockPos(x, y, z));
                            if (te instanceof net.minecraft.world.Container) ((net.minecraft.world.Container) te).clearContent();
                        }
            }
            if (full && a.hasBusiness) {
                net.minecraft.world.level.block.entity.BlockEntity te = ow.getBlockEntity(a.businessChest);
                if (te instanceof net.minecraft.world.Container) ((net.minecraft.world.Container) te).clearContent();
            }
            if (p != null) BankService.msg(p, BankService.MFO + (full ? "§4Твой дом горит. Ничего личного." : "§cУ тебя дома что-то загорелось. Случайность, конечно."));
        } else if (p != null && p.level() instanceof ServerLevel && !full) {
            ServerLevel w = (ServerLevel) p.level();
            int lit = 0;
            for (int i = 0; i < 40 && lit < 5; i++) {
                BlockPos pos = p.blockPosition().offset(RANDOM.nextInt(13) - 6, RANDOM.nextInt(3) - 1, RANDOM.nextInt(13) - 6);
                if (pos.distSqr(p.blockPosition()) < 9) continue;
                if (w.isEmptyBlock(pos) && w.getBlockState(pos.below()).isSolid()) {
                    w.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                    lit++;
                }
            }
            BankService.msg(p, BankService.MFO + "§cОй, а чего это вокруг тебя всё горит? Совпадение.");
        }
    }

    // ================================================================== неволя

    private static boolean overlapsOtherCell(EconomyData data, Account self, BlockPos c) {
        for (Account o : data.all()) {
            if (o == self || !o.cellActive) continue;
            BlockPos oc = o.cellCenter;
            if (Math.abs(oc.getX() - c.getX()) < 14 && Math.abs(oc.getZ() - c.getZ()) < 14 && Math.abs(oc.getY() - c.getY()) < 10) return true;
        }
        return false;
    }

    /** Построить персональную камеру (СИЗО в небе или подвал под МФО) со снимком исходных блоков. */
    public static void buildCell(MinecraftServer server, EconomyData data, Account a, BlockPos near) {
        if (a.cellActive || a.captivity == Account.FREE) return;
        ServerLevel ow = EconomyData.overworld(server);
        LevelData info = ow.getLevelData();
        int sx = info.getRespawnData().pos().getX(), sz = info.getRespawnData().pos().getZ();
        BlockPos center = null;
        if (a.captivity == Account.JAIL) {
            int y = Math.min(228, ow.getMaxY() - 11);
            for (int k = 0; k < 64 && center == null; k++) {
                BlockPos c = new BlockPos(sx + (k % 8) * 16, y, sz - 32 - (k / 8) * 16);
                if (!overlapsOtherCell(data, a, c) && Buildings.cellFits(ow, c, 3)) center = c;
            }
            if (center == null) center = new BlockPos(sx, y, sz - 32);
            Blueprint b = Buildings.buildJail(ow, center);
            a.cellSnapshot = b.snapshot();
        } else {
            BlockPos mfo = null;
            double best = Double.MAX_VALUE;
            BlockPos ref = near != null ? near : new BlockPos(sx, 64, sz);
            for (BlockPos m : data.mfoCenters) {
                double d = m.distSqr(ref);
                if (d < best) { best = d; mfo = m; }
            }
            int floor = mfo != null ? mfo.getY() : ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
            int mx = mfo != null ? mfo.getX() : sx, mz = mfo != null ? mfo.getZ() : sz;
            int y = floor - 10;
            BlockPos c = new BlockPos(mx, y, mz);
            for (int k = 0; k < 40 && overlapsOtherCell(data, a, c); k++) {
                y -= 8;
                if (y < 6) { mx += 12; y = floor - 10; }
                c = new BlockPos(mx, Math.max(6, y), mz);
            }
            center = c;
            Blueprint b = Buildings.buildPit(ow, center);
            a.cellSnapshot = b.snapshot();
        }
        a.cellCenter = center;
        a.cellActive = true;
        data.setDirty();
    }

    /** Убрать камеру и вернуть мир как было. */
    public static void restoreCell(MinecraftServer server, EconomyData data, Account a) {
        if (!a.cellActive) return;
        ServerLevel ow = EconomyData.overworld(server);
        BlockPos c = a.cellCenter;
        REGEN.removeIf(r -> Math.abs(r.pos.getX() - c.getX()) <= 5 && Math.abs(r.pos.getZ() - c.getZ()) <= 5
                && r.pos.getY() >= c.getY() - 1 && r.pos.getY() <= c.getY() + 6);
        Blueprint.restore(ow, a.cellSnapshot);
        a.cellSnapshot = new net.minecraft.nbt.ListTag();
        a.cellActive = false;
        data.setDirty();
    }

    private static double[] cell(Account a) {
        BlockPos c = a.cellCenter;
        double dz = a.captivity == Account.JAIL ? 0.5 : 2.5;
        return new double[]{c.getX() + 0.5, c.getY() + 1, c.getZ() + dz};
    }

    public static void sendToCaptivity(MinecraftServer server, EconomyData data, ServerPlayer p, Account a) {
        if (a.captivity == Account.FREE) return;
        if (!a.cellActive) buildCell(server, data, a, p.blockPosition());
        ServerLevel ow = EconomyData.overworld(server);
        double[] c = cell(a);
        p.teleportTo(ow, c[0], c[1], c[2], java.util.Set.of(), p.getYRot(), p.getXRot(), false);
    }

    private static void tickCaptivity(MinecraftServer server, EconomyData data, ServerPlayer p, Account a, long now) {
        if (!p.isAlive()) return;
        if (!a.cellActive) buildCell(server, data, a, p.blockPosition());
        ServerLevel ow = EconomyData.overworld(server);
        double[] c = cell(a);
        if (p.level() != ow || p.distanceToSqr(c[0], c[1], c[2]) > 30) {
            p.teleportTo(ow, c[0], c[1], c[2], java.util.Set.of(), p.getYRot(), p.getXRot(), false);
        }
        if (p.getFoodData().getFoodLevel() < 6) {
            p.getFoodData().eat(6, 0.6F);
            BankService.msg(p, "§7Вам принесли миску баланды.");
        }
        if (a.captivity == Account.JAIL) {
            long left = a.jailUntil - now;
            if (left <= 0) {
                release(server, data, p, a);
            } else {
                long sec = left / 20;
                BankService.actionBar(p, String.format("§9СИЗО №1: §fдо освобождения §c%d:%02d", sec / 60, sec % 60));
            }
        } else if (a.captivity == Account.PIT) {
            if (a.pitDebtLeft <= 0.01) {
                release(server, data, p, a);
            } else {
                if (!hasItem(p, new ItemStack(ModItems.DEBTOR_PICKAXE.get()))) {
                    BankService.give(p, new ItemStack(ModItems.DEBTOR_PICKAXE.get()));
                    BankService.msg(p, BankService.MFO + "§7На, держи кирку. Руками много не накопаешь.");
                }
                BankService.actionBar(p, "§4Подвал Шайкина: §fсдай в приёмник добычи ещё на §c" + Finance.money(a.pitDebtLeft));
            }
        }
    }

    public static void release(MinecraftServer server, EconomyData data, ServerPlayer p, Account a) {
        int was = a.captivity;
        a.captivity = Account.FREE;
        a.pitBlocksLeft = 0;
        a.pitDebtLeft = 0;
        a.jailUntil = 0;
        if (was == Account.PIT) p.getInventory().clearContent(); // всё, что накопал, остаётся Шайкину
        data.setDirty();
        // сначала увозим домой (кровать → дом → спавн мира), потом разбираем камеру
        String where = sendHome(server, p, a);
        restoreCell(server, data, a);
        BankService.msg(p, "§7Вас довезли: " + where + ".");
        if (was == Account.JAIL) {
            BankService.msg(p, "§9[Полиция] §fВы освобождены. Начните жизнь с чистого листа. И больше не берите кредитов, которые не можете вернуть.");
        } else {
            BankService.msg(p, BankService.MFO + "§fЛадно, отработал. Кирку и всё, что в карманах, оставь — это наше. Вали отсюда. Нужны деньги — заходи!");
        }
    }

    /**
     * Отвезти игрока «домой»: к его точке возрождения (кровать / якорь / /spawnpoint), если её нет — к ипотечному дому,
     * и только потом — на общий спавн мира. Возвращает описание, куда отвезли.
     */
    public static String sendHome(MinecraftServer server, ServerPlayer p, Account a) {
        BlockPos bed = (p.getRespawnConfig() == null ? null : p.getRespawnConfig().respawnData().pos());
        if (bed != null) {
            ServerLevel bw = server.getLevel((p.getRespawnConfig() == null ? null : p.getRespawnConfig().respawnData().dimension()));
            if (bw != null) {
                BlockPos at = standNear(bw, bed, 3);
                if (at != null) {
                    p.teleportTo(bw, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, java.util.Set.of(), p.getYRot(), p.getXRot(), false);
                    return "к вашей кровати (точке возрождения)";
                }
            }
        }
        ServerLevel ow = EconomyData.overworld(server);
        if (a.hasHouse && !a.houseMin.equals(BlockPos.ZERO)) {
            BlockPos c = new BlockPos((a.houseMin.getX() + a.houseMax.getX()) / 2, a.houseMin.getY() + 1, (a.houseMin.getZ() + a.houseMax.getZ()) / 2);
            BlockPos at = standNear(ow, c, 4);
            if (at != null) {
                p.teleportTo(ow, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, java.util.Set.of(), p.getYRot(), p.getXRot(), false);
                return "к вашему дому";
            }
        }
        LevelData info = ow.getLevelData();
        int x = info.getRespawnData().pos().getX(), z = info.getRespawnData().pos().getZ();
        int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        p.teleportTo(ow, x + 0.5, y, z + 0.5, java.util.Set.of(), p.getYRot(), p.getXRot(), false);
        return "на точку спавна мира (кровати и дома нет — поставьте кровать и поспите в ней)";
    }

    /** Ближайшее место, где можно стоять (2 блока воздуха, под ногами твёрдое), в радиусе r от pos. */
    public static BlockPos standNear(ServerLevel w, BlockPos pos, int r) {
        for (int d = 0; d <= r; d++)
            for (int dy = 0; dy <= 2; dy++)
                for (int sy = 1; sy >= -1; sy -= 2)
                    for (int dx = -d; dx <= d; dx++)
                        for (int dz = -d; dz <= d; dz++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != d) continue;
                            BlockPos q = pos.offset(dx, dy * sy, dz);
                            if (q.getY() < 1 || q.getY() > 254) continue;
                            if (w.isEmptyBlock(q) && w.isEmptyBlock(q.above()) && w.getBlockState(q.below()).isSolid()
                                    && !w.getBlockState(q.below()).liquid()) return q;
                        }
        return null;
    }

    /** Сдать добычу в приёмник подвала. */
    public static void pitDeliver(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(p);
        if (a.captivity != Account.PIT) {
            BankService.msg(p, "§8[Приёмник Шайкина] §7Сюда сдают добычу должники. Ты пока не должник. Пока.");
            return;
        }
        double total = 0;
        for (int i = 0; i < p.getInventory().getNonEquipmentItems().size(); i++) {
            ItemStack st = p.getInventory().getNonEquipmentItems().get(i);
            double v = Buildings.pitValue(st);
            if (v > 0) {
                total += v * st.getCount();
                p.getInventory().getNonEquipmentItems().set(i, ItemStack.EMPTY);
            }
        }
        if (total <= 0) {
            BankService.msg(p, BankService.MFO + "§7Пусто. Копай: руда, уголь, камень, земля, гравий — всё идёт в зачёт. Алмаз — 200₽.");
            return;
        }
        a.pitDebtLeft = Math.max(0, a.pitDebtLeft - total);
        data.setDirty();
        if (a.pitDebtLeft <= 0.01) {
            release(server, data, p, a);
        } else {
            BankService.msg(p, BankService.MFO + "Принято добычи на §a" + Finance.money(total) + "§f. Осталось сдать на §c" + Finance.money(a.pitDebtLeft));
        }
    }

    // ================================================================== ограбление банка

    private static class Alarm {
        final AxisAlignedBBHolder box;
        final long due;
        final Set<UUID> players = new java.util.HashSet<>();

        Alarm(AxisAlignedBBHolder box, long due) {
            this.box = box;
            this.due = due;
        }
    }

    private static class AxisAlignedBBHolder {
        final BlockPos min, max;

        AxisAlignedBBHolder(BlockPos min, BlockPos max) {
            this.min = min;
            this.max = max;
        }

        boolean contains(Entity e) {
            return e.getX() >= min.getX() - 1 && e.getX() <= max.getX() + 2 && e.getZ() >= min.getZ() - 1
                    && e.getZ() <= max.getZ() + 2 && e.getY() >= min.getY() - 2 && e.getY() <= max.getY() + 2;
        }
    }

    private static final List<Alarm> ALARMS = new ArrayList<>();

    /** Игрок ударил кассира: тревожная кнопка. Всех, кто сейчас в зале, через 30 сек ищет СОБР. */
    public static void cashierHit(CashierEntity c, ServerPlayer attacker) {
        MinecraftServer server = attacker.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        long now = c.level().getGameTime();
        com.baraaoi.reallife.economy.Structure st = data.structureAt(c.blockPosition(), com.baraaoi.reallife.economy.Structure.BANK, 1);
        BlockPos min = st != null ? st.min : c.blockPosition().offset(-11, -3, -11);
        BlockPos max = st != null ? st.max : c.blockPosition().offset(11, 6, 11);
        for (Alarm al : ALARMS) {
            if (al.box.min.equals(min)) {
                al.players.add(attacker.getUUID());
                return;
            }
        }
        Alarm al = new Alarm(new AxisAlignedBBHolder(min, max), now + Config.ALARM_DELAY.get());
        al.players.add(attacker.getUUID());
        for (ServerPlayer o : server.getPlayerList().getPlayers()) {
            if (o.level() == c.level() && al.box.contains(o) && !o.isSpectator()) al.players.add(o.getUUID());
        }
        ALARMS.add(al);
        int sec = Config.ALARM_DELAY.get() / 20;
        for (UUID id : al.players) {
            ServerPlayer o = server.getPlayerList().getPlayer(id);
            if (o != null) BankService.msg(o, "§4§l[ТРЕВОГА] §r§cКассир нажал тревожную кнопку! Через " + sec
                    + " сек. приедет СОБР — искать будут всех, кто сейчас в банке. Даже если уйдёте.");
        }
    }

    public static void cashierKilled(CashierEntity c) {
        MinecraftServer server = c.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        com.baraaoi.reallife.economy.Structure st = data.structureAt(c.blockPosition(), com.baraaoi.reallife.economy.Structure.BANK, 1);
        if (st != null) {
            st.cashierRespawn = c.level().getGameTime() + 12000;
            data.setDirty();
        }
    }

    private static void tickAlarms(MinecraftServer server, EconomyData data, long now) {
        if (ALARMS.isEmpty()) return;
        Iterator<Alarm> it = ALARMS.iterator();
        while (it.hasNext()) {
            Alarm al = it.next();
            if (now < al.due) continue;
            it.remove();
            for (UUID id : al.players) {
                Account a = data.getOrCreate(id);
                if (a.captivity != Account.FREE) continue;
                a.wanted = true;
                a.robber = true;
                a.robberyUntil = now + 24000;
                a.policeWave = Math.max(1, a.policeWave);
                a.nextPoliceTick = Math.max(1, now);
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) BankService.msg(p, "§9§l[СОБР] §r§cВы были в банке во время нападения на кассира. Вы объявлены в розыск на сутки!");
            }
            data.setDirty();
        }
    }

    /** v1.9: монстры, забредшие в городские постройки (казино, банк, МФО), «выводятся охраной». */
    public static int clearCityMobs(EconomyData data, ServerLevel ow) {
        int n = 0;
        for (com.baraaoi.reallife.economy.Structure st : data.structures) {
            if (!st.protect) continue; // getEntitiesWithinAABB сам пропускает невыгруженные чанки
            for (net.minecraft.world.entity.monster.Monster m : ow.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                    new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(st.min), net.minecraft.world.phys.Vec3.atLowerCornerOf(st.max.offset(1, 1, 1))))) {
                m.discard();
                n++;
            }
        }
        return n;
    }

    /**
     * v1.9: досветить казино — каждый третий блок «шахматного» пола становится морским фонарём (уровень света на полу ≥ 12,
     * монстры не спавнятся). Работает и для уже построенных казино. Возвращает число поставленных фонарей.
     */
    public static int lightCasino(ServerLevel w, com.baraaoi.reallife.economy.Structure st) {
        int n = 0;
        for (BlockPos mp : BlockPos.betweenClosed(st.min, st.max)) {
            int rx = mp.getX() - st.min.getX(), rz = mp.getZ() - st.min.getZ();
            if (rx % 3 != 1 || rz % 3 != 1) continue;
            BlockState bs = w.getBlockState(mp);
            if (bs.getBlock() != Blocks.CONCRETE.black() && bs.getBlock() != Blocks.CONCRETE.red()) continue;
            BlockPos up = mp.above();
            BlockState us = w.getBlockState(up);
            if (us.isSolidRender() || w.canSeeSky(up)) continue;
            if (!w.getBlockState(mp.below()).isSolid()) continue;
            w.setBlock(mp.immutable(), Blocks.SEA_LANTERN.defaultBlockState(), 2 | 16);
            n++;
        }
        return n;
    }

    /** Посадить на место убитых кассиров (не раньше чем через полдня после убийства). */
    private static void tickCashiers(EconomyData data, ServerLevel ow, long now) {
        for (com.baraaoi.reallife.economy.Structure st : data.structures) {
            if (st.cashiers.isEmpty() || now < st.cashierRespawn) continue;
            for (BlockPos c : st.cashiers) {
                if (!ow.isLoaded(c)) continue;
                if (ow.getEntitiesOfClass(CashierEntity.class, new net.minecraft.world.phys.AABB(c).inflate(1.5D)).isEmpty()) {
                    Buildings.spawnCashier(ow, c, net.minecraft.core.Direction.NORTH);
                }
            }
        }
    }

    // ================================================================== взятки

    /** Полицейский подобрал деньги: копится «взятка», при достижении порога наряд уезжает на сутки. */
    public static void policeBribe(PoliceEntity cop, double value) {
        MinecraftServer server = cop.level().getServer();
        if (server == null || cop.getTargetId() == null) return;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(cop.getTargetId());
        if (a == null) return;
        Player p = server.getPlayerList().getPlayer(a.id);
        a.policeBribe += value;
        int need = Config.POLICE_BRIBE.get();
        long now = cop.level().getGameTime();
        if (a.policeBribe >= need) {
            a.policeBribe = 0;
            a.policeWave = Math.max(1, a.policeWave);
            a.nextPoliceTick = now + 24000;
            if (p != null) {
                for (ServerLevel w : server.getAllLevels()) {
                    for (PoliceEntity e : w.getEntitiesOfClass(PoliceEntity.class, p.getBoundingBox().inflate(160.0D),
                            e -> a.id.equals(e.getTargetId()))) e.discard();
                }
                BankService.msg(p, "§9§l[Полиция] §r§7Сержант пересчитал купюры: «Так... гражданин, мы вас не видели. Сутки.» "
                        + "§8Наряд уехал. Розыск никто не отменял.");
            } else {
                cop.discard();
            }
        } else if (p != null) {
            cop.say(p, "§eЭто что, взятка?.. Мало. §7(собрано " + Finance.money(a.policeBribe) + " из " + Finance.money(need) + ")");
        }
        data.setDirty();
    }

    // ================================================================== земля и самострой

    private static boolean landTracked(ServerPlayer p) {
        return Config.LAND_CONTROL.get() && !p.isCreative() && !p.isSpectator() && p.level().dimension() == Level.OVERWORLD;
    }

    public static void onPlace(ServerPlayer p, BlockPos pos) {
        if (!landTracked(p)) return;
        MinecraftServer server = p.level().getServer();
        if (server == null) return;
        EconomyData data = EconomyData.get(server);
        Account a = data.get(p);
        if (a.captivity != Account.FREE) return;
        long chunk = EconomyData.chunkKey(pos);
        UUID owner = data.owner(chunk);
        if (p.getUUID().equals(owner)) return;
        if (data.mallAt(pos, 1) != null) return; // в ТЦ права проверяет сам ТЦ (своё помещение / владелец)
        int n = data.addPlaced(chunk, p.getUUID(), pos);
        int limit = Config.LAND_THRESHOLD.get();
        if (n == Math.max(1, limit - 10) && !a.landCase) {
            BankService.msg(p, "§3[Администрация] §7Вы строите на " + (owner == null ? "ничейной" : "чужой") + " земле. "
                    + "Купите участок (§f/reallife land buy§7, " + landQuote(server, p.getUUID(), chunk) + "), иначе придёт инспектор.");
        }
        if (n >= limit && !a.landCase) openLandCase(server, data, p, a, chunk, owner);
    }

    public static void onPlayerBreak(ServerPlayer p, BlockPos pos) {
        if (p.level().dimension() != Level.OVERWORLD || p.level().getServer() == null) return;
        EconomyData data = EconomyData.get(p.level().getServer());
        data.removePlaced(EconomyData.chunkKey(pos), p.getUUID(), pos);
    }

    private static void openLandCase(MinecraftServer server, EconomyData data, ServerPlayer p, Account a, long chunk, UUID owner) {
        long now = EconomyData.overworld(server).getGameTime();
        double fine = Config.LAND_FINE.get();
        double take = Math.max(0, Math.min(a.balance, fine));
        a.balance -= take;
        if (fine - take > 0.01) a.arrears += fine - take;
        a.landCase = true;
        a.landChunk = chunk;
        a.landDeadline = now + Config.LAND_DEADLINE.get();
        data.setDirty();
        int min = Config.LAND_DEADLINE.get() / 1200;
        BankService.msg(p, "§3§l[Администрация] §r§cСамовольное строительство! §fШтраф " + Finance.money(fine)
                + (fine - take > 0.01 ? " (не хватило — §c" + Finance.money(fine - take) + "§f записано в долг банку)" : " списан со счёта") + ".");
        BankService.msg(p, "§3[Администрация] §fВ течение §c" + min + " мин.§f снесите постройку или купите участок за §e"
                + landQuote(server, p.getUUID(), chunk) + "§f (ПКМ по инспектору или §7/reallife land buy§f). Иначе — выселение и снос всего чанка.");
        if (owner != null && !owner.equals(Buildings.CITY)) {
            BankService.msg(p, "§3[Администрация] §7Участок принадлежит другому игроку — при сносе уберут только ваши блоки.");
        }
        if (p.level().dimension() == Level.OVERWORLD) spawn(ModEntities.INSPECTOR.get(), p.level(), p, 5, 9);
    }

    /** Свободные чанки участка 3×3 вокруг chunk, которые игрок может купить. */
    public static java.util.List<Long> plotChunks(EconomyData data, UUID player, long chunk) {
        net.minecraft.world.level.ChunkPos c = net.minecraft.world.level.ChunkPos.unpack(chunk);
        java.util.List<Long> l = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                long k = net.minecraft.world.level.ChunkPos.pack(c.x() + dx, c.z() + dz);
                if (data.owner(k) == null) l.add(k);
            }
        return l;
    }

    /** Текст цены участка 3×3 вокруг chunk для игрока. */
    public static String landQuote(MinecraftServer server, UUID player, long chunk) {
        int n = plotChunks(EconomyData.get(server), player, chunk).size();
        return Finance.money(plotPrice(Math.max(1, n))) + " (участок 3×3 чанка, свободных: " + n + ")";
    }

    /** Цена участка из n свободных чанков. */
    public static int plotPrice(int n) {
        return Config.LAND_PRICE.get() * n;
    }

    /** Купить участок 3×3 чанка вокруг chunk (только свободные чанки): со счёта, если не хватает — наличными. */
    public static boolean buyLand(ServerPlayer p, Account a, long chunk) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return false;
        EconomyData data = EconomyData.get(server);
        UUID owner = data.owner(chunk);
        if (owner != null && !owner.equals(p.getUUID())) {
            BankService.msg(p, "§3[Администрация] §cЦентр участка " + (owner.equals(Buildings.CITY) ? "городской" : "принадлежит другому игроку") + " и не продаётся.");
            return false;
        }
        java.util.List<Long> free = plotChunks(data, p.getUUID(), chunk);
        if (free.isEmpty()) {
            BankService.msg(p, "§3[Администрация] §fВся земля 3×3 вокруг уже оформлена — покупать нечего.");
            return false;
        }
        int price = plotPrice(free.size());
        if (!BankService.charge(p, a, price)) {
            BankService.msg(p, "§3[Администрация] §cНе хватает денег: участок (" + free.size() + " чанк(ов) из 3×3) стоит " + Finance.money(price) + " (счёт + наличные).");
            return false;
        }
        for (long k : free) data.claim(p.getUUID(), k);
        if (a.landCase && data.owner(a.landChunk) != null && p.getUUID().equals(data.owner(a.landChunk))) a.landCase = false;
        data.setDirty();
        net.minecraft.world.level.ChunkPos cp = net.minecraft.world.level.ChunkPos.unpack(chunk);
        BankService.msg(p, "§3[Администрация] §aУчасток 3×3 чанка оформлен в собственность §f(" + free.size() + " новых чанк(ов) за "
                + Finance.money(price) + "; блоки X " + (cp.getMinBlockX() - 16) + ".." + (cp.getMaxBlockX() + 16) + ", Z " + (cp.getMinBlockZ() - 16)
                + ".." + (cp.getMaxBlockZ() + 16) + "). Стройте на здоровье!");
        return true;
    }

    private static void tickLand(MinecraftServer server, EconomyData data, ServerLevel ow, long now) {
        int limit = Config.LAND_THRESHOLD.get();
        for (Account a : new ArrayList<>(data.all())) {
            if (!a.landCase) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(a.id);
            UUID owner = data.owner(a.landChunk);
            if (a.id.equals(owner)) {
                a.landCase = false;
                data.setDirty();
                continue;
            }
            if (data.placedCount(a.landChunk, a.id) <= limit / 3) {
                a.landCase = false;
                data.setDirty();
                if (p != null) BankService.msg(p, "§3[Администрация] §aСамострой снесён. Дело закрыто.");
                continue;
            }
            if (p != null && a.landDeadline - now <= 1200 && a.landDeadline - now > 1180) {
                BankService.msg(p, "§3[Администрация] §cДо сноса осталась 1 минута!");
            }
            if (now >= a.landDeadline) evict(server, data, ow, a, p, owner);
        }
    }

    private static void evict(MinecraftServer server, EconomyData data, ServerLevel ow, Account a, ServerPlayer p, UUID owner) {
        long chunk = a.landChunk;
        net.minecraft.world.level.ChunkPos cp = net.minecraft.world.level.ChunkPos.unpack(chunk);
        a.landCase = false;
        if (owner == null) {
            EVICTIONS.add(new Eviction(cp.x(), cp.z(), ow.getMaxY()));
        } else {
            // чужой/городской участок — убираем только блоки нарушителя
            java.util.Map<UUID, Set<Long>> m = data.placed.get(chunk);
            if (m != null && m.get(a.id) != null) {
                for (long l : m.get(a.id)) {
                    BlockPos pos = BlockPos.of(l);
                    net.minecraft.world.level.block.entity.BlockEntity te = ow.getBlockEntity(pos);
                    if (te instanceof net.minecraft.world.Container) ((net.minecraft.world.Container) te).clearContent();
                    ow.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
                m.remove(a.id);
            }
        }
        if (owner == null) data.clearPlaced(chunk);
        data.setDirty();
        String where = "чанк " + cp.x() + ", " + cp.z() + " (X " + cp.getMinBlockX() + ".." + cp.getMaxBlockX() + ", Z " + cp.getMinBlockZ() + ".." + cp.getMaxBlockZ() + ")";
        if (p != null) {
            BankService.msg(p, "§3§l[Администрация] §r§4Выселение! §cСамострой сносится: " + where
                    + (owner == null ? " — до коренной породы." : " — убраны ваши блоки."));
        }
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other != p) BankService.msg(other, "§9[Новости] §fУ игрока §e" + a.name + "§f снесли самострой: " + where + ".");
        }
    }

    // ================================================================== запуск мира

    public static void onServerStarted(MinecraftServer server) {
        ServerLevel ow = EconomyData.overworld(server);
        EconomyData data = EconomyData.get(server);
        // старые постоянные тюрьма/подвал (версия 1.0) — засыпаем
        if (data.jailBuilt) {
            fillBox(ow, data.jailPos);
            data.jailBuilt = false;
        }
        if (data.pitBuilt) {
            fillBox(ow, data.pitPos);
            data.pitBuilt = false;
        }
        LevelData info = ow.getLevelData();
        int sx = info.getRespawnData().pos().getX(), sz = info.getRespawnData().pos().getZ();
        net.minecraft.core.Direction south = net.minecraft.core.Direction.SOUTH;
        if (Config.SPAWN_BUILDINGS.get() && !data.spawnBuilt) {
            int by = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz + 6);
            Buildings.buildBank(ow, new BlockPos(sx, by, sz + 6), south, true);
            int my = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx - 18, sz + 6);
            Buildings.buildMfo(ow, new BlockPos(sx - 18, my, sz + 6), south, true);
            data.spawnBuilt = true;
            data.spawnVersion = 3;
            com.baraaoi.reallife.RealLife.LOG.info("Real Life: bank and MFO built at world spawn " + sx + ", " + sz);
        } else if (data.spawnBuilt && data.spawnVersion < 3) {
            // мир с версии 2: находим банк и МФО по стойкам, перестраиваем банк (кассиры) и делаем оба неразрушимыми
            BlockPos bc = findInColumn(ow, sx, sz + 11, com.baraaoi.reallife.registry.ModBlocks.BANK_COUNTER.get());
            int by = bc != null ? bc.getY() - 1 : ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz + 6);
            Buildings.buildBank(ow, new BlockPos(sx, by, sz + 6), south, true);
            BlockPos mc = findInColumn(ow, sx - 18, sz + 11, com.baraaoi.reallife.registry.ModBlocks.MFO_COUNTER.get());
            int my = mc != null ? mc.getY() - 1 : ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx - 18, sz + 6);
            Buildings.buildMfo(ow, new BlockPos(sx - 18, my, sz + 6), south, true);
            data.spawnVersion = 3;
            com.baraaoi.reallife.RealLife.LOG.info("Real Life: spawn bank rebuilt (v3), bank y=" + by + ", mfo y=" + my);
        }
        for (Account acc : data.all()) {
            if (!acc.needPrinters) continue;
            acc.needPrinters = false;
            com.baraaoi.reallife.economy.Business.migrateLegacy(ow, acc);
        }
        if (Config.SPAWN_CASINO.get() && !data.casinoBuilt) {
            data.casinoBuilt = true;
            data.casinoLights = 1; // новое казино уже строится со светящимся полом
            buildSpawnCasino(ow, data, sx, sz);
        }
        if (data.casinoLights < 1) {
            data.casinoLights = 1;
            for (com.baraaoi.reallife.economy.Structure st : data.structures) {
                if (!com.baraaoi.reallife.economy.Structure.CASINO.equals(st.type)) continue;
                int lit = lightCasino(ow, st);
                com.baraaoi.reallife.RealLife.LOG.info("Real Life: casino lit up with " + lit + " sea lanterns at " + st.min);
            }
        }
        data.setDirty();
    }

    private static BlockPos findInColumn(ServerLevel w, int x, int z, net.minecraft.world.level.block.Block b) {
        for (int y = w.getMaxY(); y > 0; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (w.getBlockState(p).getBlock() == b) return p;
        }
        return null;
    }

    /** Казино: 100–130 блоков от спавна (не впритык, но в пределах 12 чанков), на самом ровном месте. */
    public static BlockPos buildSpawnCasino(ServerLevel ow, EconomyData data, int sx, int sz) {
        BlockPos best = null;
        net.minecraft.core.Direction bestDir = net.minecraft.core.Direction.SOUTH;
        int bestScore = Integer.MAX_VALUE;
        for (int k = 0; k < 8; k++) {
            double ang = k * Math.PI / 4 + Math.PI / 8;
            for (int dist : new int[]{100, 125}) {
                int dx = (int) Math.round(Math.cos(ang) * dist), dz = (int) Math.round(Math.sin(ang) * dist);
                net.minecraft.core.Direction f = Math.abs(dx) > Math.abs(dz)
                        ? (dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST)
                        : (dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH);
                Blueprint probe = new Blueprint(ow, new BlockPos(sx + dx, 64, sz + dz), f);
                int[] hs = new int[9];
                int n = 0, water = 0;
                boolean taken = false;
                for (int lx : new int[]{-16, 0, 16})
                    for (int lz : new int[]{-8, 12, 31}) {
                        BlockPos q = probe.pos(lx, 0, lz);
                        int h = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, q.getX(), q.getZ());
                        hs[n++] = h;
                        if (ow.getBlockState(new BlockPos(q.getX(), h - 1, q.getZ())).liquid()) water++;
                        long ck = EconomyData.chunkKey(q);
                        if (data.owner(ck) != null || data.placed.containsKey(ck)) taken = true;
                    }
                if (taken) continue;
                java.util.Arrays.sort(hs);
                int score = (hs[8] - hs[0]) + water * 8;
                if (score < bestScore) {
                    bestScore = score;
                    best = new BlockPos(sx + dx, hs[4], sz + dz);
                    bestDir = f;
                }
            }
        }
        if (best == null) {
            com.baraaoi.reallife.RealLife.LOG.warn("Real Life: no free place for the casino near spawn");
            return null;
        }
        Buildings.buildCasino(ow, best, bestDir, true);
        data.setDirty();
        com.baraaoi.reallife.RealLife.LOG.info("Real Life: casino built at " + best + " facing " + bestDir + " (score " + bestScore + ")");
        return best;
    }

    private static void fillBox(ServerLevel w, BlockPos c) {
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-4, 0, -4), c.offset(4, 5, 4))) {
            net.minecraft.world.level.block.entity.BlockEntity te = w.getBlockEntity(p);
            if (te instanceof net.minecraft.world.Container) ((net.minecraft.world.Container) te).clearContent();
            w.setBlock(p.immutable(), Blocks.STONE.defaultBlockState(), 2 | 16);
        }
    }
}
