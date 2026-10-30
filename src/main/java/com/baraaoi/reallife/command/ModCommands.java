package com.baraaoi.reallife.command;

import com.baraaoi.reallife.economy.Account;
import com.baraaoi.reallife.economy.BankService;
import com.baraaoi.reallife.economy.CreditRating;
import com.baraaoi.reallife.economy.EconomyData;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.event.DebtManager;
import com.baraaoi.reallife.structure.Buildings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/**
 * /reallife info — сводка по счёту.
 * /reallife admin ... — команды для операторов (тестирование).
 */
public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("reallife")
                .then(Commands.literal("info").executes(c -> info(c.getSource().getPlayerOrException())))
                .then(Commands.literal("land").executes(c -> land(c.getSource().getPlayerOrException()))
                        .then(Commands.literal("buy").executes(c -> landBuy(c.getSource().getPlayerOrException()))))
                .then(Commands.literal("npc")
                        .then(Commands.literal("yes").executes(c -> { com.baraaoi.reallife.event.Visitors.answer(c.getSource().getPlayerOrException(), true); return 1; }))
                        .then(Commands.literal("no").executes(c -> { com.baraaoi.reallife.event.Visitors.answer(c.getSource().getPlayerOrException(), false); return 1; })))
                .then(Commands.literal("mall")
                        .then(Commands.literal("buy").then(Commands.argument("mall", IntegerArgumentType.integer()).then(Commands.argument("unit", IntegerArgumentType.integer())
                                .executes(c -> { com.baraaoi.reallife.economy.Malls.buy(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "mall"), IntegerArgumentType.getInteger(c, "unit")); return 1; }))))
                        .then(Commands.literal("take").then(Commands.argument("mall", IntegerArgumentType.integer()).then(Commands.argument("unit", IntegerArgumentType.integer())
                                .executes(c -> { com.baraaoi.reallife.economy.Malls.take(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "mall"), IntegerArgumentType.getInteger(c, "unit")); return 1; }))))
                        .then(Commands.literal("leave").then(Commands.argument("mall", IntegerArgumentType.integer()).then(Commands.argument("unit", IntegerArgumentType.integer())
                                .executes(c -> { com.baraaoi.reallife.economy.Malls.leave(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "mall"), IntegerArgumentType.getInteger(c, "unit")); return 1; })))))
                .then(Commands.literal("rival")
                        .then(Commands.literal("buyout").then(Commands.argument("id", IntegerArgumentType.integer())
                                .executes(c -> { com.baraaoi.reallife.economy.Rivals.buyout(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "id")); return 1; }))))
                .then(Commands.literal("admin").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("rival").executes(c -> { com.baraaoi.reallife.economy.Rivals.forceSpawn(c.getSource().getPlayerOrException()); return 1; }))
                        .then(Commands.literal("event").then(Commands.argument("kind", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(com.baraaoi.reallife.event.Visitors.eventIds(), b))
                                .executes(c -> event(c.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "kind")))))
                        .then(Commands.literal("events").executes(c -> {
                            ServerPlayer ep = c.getSource().getPlayerOrException();
                            String act = com.baraaoi.reallife.event.Visitors.activeKind(ep);
                            long t = com.baraaoi.reallife.event.Visitors.ticksToNext(ep);
                            BankService.msg(ep, act != null ? "§eСейчас у вас гость: " + act
                                    : "§eСледующий случайный гость примерно через §f" + (t / 1200) + " мин " + (t % 1200 / 20) + " с"
                                    + (com.baraaoi.reallife.Config.EVENTS_ENABLED.get() ? "" : " §c(события выключены в конфиге!)"));
                            return 1;
                        }))
                        .then(Commands.literal("money").then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(c -> money(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "amount")))))
                        .then(Commands.literal("rating").then(Commands.argument("value", IntegerArgumentType.integer(300, 850))
                                .executes(c -> rating(c.getSource(), c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "value")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> rating(c.getSource(), EntityArgument.getPlayer(c, "player"), IntegerArgumentType.getInteger(c, "value"))))))
                        .then(Commands.literal("land").then(Commands.literal("reset").executes(c -> landReset(c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> landReset(EntityArgument.getPlayer(c, "player"))))))
                        .then(Commands.literal("nextday").executes(c -> nextDay(c.getSource())))
                        .then(Commands.literal("overdue").then(Commands.argument("days", IntegerArgumentType.integer(0, 100))
                                .executes(c -> overdue(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "days")))))
                        .then(Commands.literal("mfooverdue").then(Commands.argument("days", IntegerArgumentType.integer(0, 100))
                                .executes(c -> mfoOverdue(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "days")))))
                        .then(Commands.literal("build")
                                .then(Commands.literal("bank").executes(c -> build(c.getSource().getPlayerOrException(), true)))
                                .then(Commands.literal("mfo").executes(c -> build(c.getSource().getPlayerOrException(), false)))
                                .then(Commands.literal("casino").executes(c -> {
                                    ServerPlayer cp = c.getSource().getPlayerOrException();
                                    net.minecraft.core.Direction cf = cp.getDirection();
                                    Buildings.buildCasino(cp.level(), cp.blockPosition().relative(cf, 2), cf, false);
                                    return 1;
                                })))
                        .then(Commands.literal("free").executes(c -> free(c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> free(EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("reset").executes(c -> reset(c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> reset(EntityArgument.getPlayer(c, "player")))))));
    }

    private static int event(ServerPlayer p, String kind) {
        if (kind.equalsIgnoreCase("obep") || kind.equalsIgnoreCase("snoop")) {
            String e = com.baraaoi.reallife.event.Raids.adminStart(p, kind.equalsIgnoreCase("obep"));
            if (e != null) BankService.msg(p, "§cНе вышло: " + e);
            return e == null ? 1 : 0;
        }
        com.baraaoi.reallife.entity.VisitorKind k = com.baraaoi.reallife.entity.VisitorKind.byName(kind);
        if (k == null) {
            BankService.msg(p, "§cНет такого гостя. Есть: " + String.join(", ", com.baraaoi.reallife.event.Visitors.eventIds()));
            return 0;
        }
        String err = com.baraaoi.reallife.event.Visitors.adminStart(p, k);
        if (err != null) {
            BankService.msg(p, "§cНе вышло: " + err);
            return 0;
        }
        return 1;
    }

    private static Account acc(ServerPlayer p) {
        return EconomyData.get(p.level().getServer()).get(p);
    }

    private static int info(ServerPlayer p) throws CommandSyntaxException {
        Account a = acc(p);
        long today = BankService.today(p.level().getServer());
        int score = CreditRating.score(a, today);
        BankService.msg(p, "§6===== Реал Банк: " + p.getGameProfile().name() + " ===== §8(Real Life v" + com.baraaoi.reallife.RealLife.version() + ")");
        BankService.msg(p, "Баланс: §e" + Finance.money(a.balance) + "§f, рейтинг: §b" + score + " " + CreditRating.category(score));
        BankService.msg(p, "Доступный кредит: §a" + Finance.money(CreditRating.maxLoan(a, today)) + "§f под " + Finance.percent(CreditRating.loanRate(score)) + "/день");
        if (a.hasLoan()) BankService.msg(p, "Кредит: " + Finance.money(a.loanPrincipal) + ", платёж " + Finance.money(a.loanPayment()) + "/день, осталось " + a.loanDays + " дн.");
        if (a.hasMortgage()) BankService.msg(p, "Ипотека: " + Finance.money(a.mortPrincipal) + ", платёж " + Finance.money(a.mortPayment()) + "/день, осталось " + a.mortDays + " дн.");
        if (a.arrears > 0.01) BankService.msg(p, "§cПросрочка: " + Finance.money(a.arrears) + " (" + a.overdueDays + " дн.)");
        if (a.hasBusiness) BankService.msg(p, "§2Подпольный цех: §fработает" + (a.hasLaundering ? ", ООО «Отмыв» оформлено" : ""));
        int land = EconomyData.get(p.level().getServer()).ownedCount(p.getUUID());
        if (land > 0) BankService.msg(p, "§3Участков в собственности: §f" + land);
        if (a.landCase) BankService.msg(p, "§cОткрыто дело о самострое! §7/reallife land");
        if (a.hasMfo()) BankService.msg(p, "§2МФО: §cдолг " + Finance.money(a.mfoDebt) + (a.mfoDays > 0 ? ", срок " + a.mfoDays + " дн." : ", просрочка " + a.mfoOverdueDays + " дн."));
        return 1;
    }

    private static int rating(CommandSourceStack src, ServerPlayer p, int value) {
        EconomyData data = EconomyData.get(p.level().getServer());
        Account a = data.get(p);
        long today = BankService.today(p.level().getServer());
        a.ratingBonus = CreditRating.bonusFor(a, today, value);
        data.setDirty();
        int now = CreditRating.score(a, today);
        src.sendSuccess(() -> Component.literal("§aРейтинг " + p.getGameProfile().name() + ": §b" + now + " " + CreditRating.category(now)
                + " §7(поправка " + (a.ratingBonus >= 0 ? "+" : "") + a.ratingBonus + ")"), true);
        if (src.getEntity() != p) BankService.msg(p, BankService.BANK + "Ваш кредитный рейтинг пересмотрен: §b" + now);
        return 1;
    }

    private static int land(ServerPlayer p) {
        EconomyData data = EconomyData.get(p.level().getServer());
        Account a = data.get(p);
        long chunk = EconomyData.chunkKey(p.blockPosition());
        java.util.UUID owner = data.owner(chunk);
        net.minecraft.world.level.ChunkPos cp = net.minecraft.world.level.ChunkPos.unpack(chunk);
        String who;
        if (owner == null) who = "§7ничей (купить участок 3×3 чанка вокруг: §f/reallife land buy§7, "
                + Finance.money(DebtManager.plotPrice(DebtManager.plotChunks(data, p.getUUID(), chunk).size())) + ")";
        else if (owner.equals(p.getUUID())) {
            int free = DebtManager.plotChunks(data, p.getUUID(), chunk).size();
            who = "§aваш" + (free > 0 ? " §7(докупить соседние " + free + " чанк(ов) 3×3: §f/reallife land buy§7, " + Finance.money(DebtManager.plotPrice(free)) + ")" : "");
        }
        else if (owner.equals(Buildings.CITY)) who = "§6городской (банк/МФО)";
        else {
            Account o = data.get(owner);
            who = "§cчужой" + (o != null && o.name != null && !o.name.isEmpty() ? " (" + o.name + ")" : "");
        }
        BankService.msg(p, "§3===== Участок " + cp.x() + ", " + cp.z() + " (X " + cp.getMinBlockX() + ".." + cp.getMaxBlockX() + ", Z " + cp.getMinBlockZ() + ".." + cp.getMaxBlockZ() + ") =====");
        BankService.msg(p, "Владелец: " + who);
        if (!p.getUUID().equals(owner)) {
            BankService.msg(p, "Ваших блоков здесь: §e" + data.placedCount(chunk, p.getUUID()) + "§f из §e"
                    + com.baraaoi.reallife.Config.LAND_THRESHOLD.get() + "§f до визита инспектора.");
        }
        BankService.msg(p, "Ваших участков всего: §e" + data.ownedCount(p.getUUID()));
        if (a.landCase) {
            net.minecraft.world.level.ChunkPos lc = net.minecraft.world.level.ChunkPos.unpack(a.landChunk);
            long left = Math.max(0, a.landDeadline - p.level().getGameTime()) / 20;
            BankService.msg(p, String.format("§cОткрыто дело о самострое: чанк %d, %d. До сноса %d:%02d.", lc.x(), lc.z(), left / 60, left % 60));
        }
        return 1;
    }

    private static int landBuy(ServerPlayer p) {
        if (p.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            BankService.msg(p, "§cУчастки продаются только в обычном мире.");
            return 0;
        }
        EconomyData data = EconomyData.get(p.level().getServer());
        return DebtManager.buyLand(p, data.get(p), EconomyData.chunkKey(p.blockPosition())) ? 1 : 0;
    }

    private static int landReset(ServerPlayer p) {
        EconomyData data = EconomyData.get(p.level().getServer());
        Account a = data.get(p);
        a.landCase = false;
        for (java.util.Map<java.util.UUID, java.util.Set<Long>> m : data.placed.values()) m.remove(p.getUUID());
        data.setDirty();
        BankService.msg(p, "§aДела о самострое и счётчики блоков сброшены.");
        return 1;
    }

    private static int money(ServerPlayer p, int amount) {
        Account a = acc(p);
        a.balance += amount;
        EconomyData.get(p.level().getServer()).setDirty();
        BankService.msg(p, "§aБаланс изменён: " + Finance.money(a.balance));
        return 1;
    }

    private static int nextDay(CommandSourceStack src) {
        MinecraftServer server = src.getServer();
        BankService.processDay(server, EconomyData.get(server));
        src.sendSuccess(() -> Component.literal("§aОбработан один банковский день."), true);
        return 1;
    }

    private static int overdue(ServerPlayer p, int days) {
        Account a = acc(p);
        a.overdueDays = days;
        if (days > 0 && a.arrears < 100) a.arrears = 100;
        if (days == 0) { a.clearArrears(); a.wanted = false; }
        EconomyData.get(p.level().getServer()).setDirty();
        BankService.msg(p, "§aПросрочка банка: " + days + " дн., " + Finance.money(a.arrears));
        return 1;
    }

    private static int mfoOverdue(ServerPlayer p, int days) {
        Account a = acc(p);
        if (!a.hasMfo()) { a.mfoDebt = 500; a.mfoPrincipal = 500; }
        a.mfoDays = 0;
        a.mfoOverdueDays = days;
        if (days == 0) a.clearMfo();
        EconomyData.get(p.level().getServer()).setDirty();
        BankService.msg(p, "§aПросрочка МФО: " + days + " дн., долг " + Finance.money(a.mfoDebt));
        return 1;
    }

    private static int build(ServerPlayer p, boolean bank) {
        Direction f = p.getDirection();
        if (bank) Buildings.buildBank(p.level(), p.blockPosition().relative(f, 2), f);
        else Buildings.buildMfo(p.level(), p.blockPosition().relative(f, 2), f);
        return 1;
    }

    private static int free(ServerPlayer p) {
        EconomyData data = EconomyData.get(p.level().getServer());
        Account a = data.get(p);
        if (a.captivity != Account.FREE) DebtManager.release(p.level().getServer(), data, p, a);
        return 1;
    }

    private static int reset(ServerPlayer p) {
        EconomyData.get(p.level().getServer()).reset(p.getUUID());
        BankService.msg(p, "§aСчёт сброшен.");
        return 1;
    }
}
