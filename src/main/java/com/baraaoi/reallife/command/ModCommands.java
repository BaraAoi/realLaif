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
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.text.StringTextComponent;

/**
 * /reallife info — сводка по счёту.
 * /reallife admin ... — команды для операторов (тестирование).
 */
public class ModCommands {

    public static void register(CommandDispatcher<CommandSource> d) {
        d.register(Commands.literal("reallife")
                .then(Commands.literal("info").executes(c -> info(c.getSource().asPlayer())))
                .then(Commands.literal("land").executes(c -> land(c.getSource().asPlayer()))
                        .then(Commands.literal("buy").executes(c -> landBuy(c.getSource().asPlayer()))))
                .then(Commands.literal("npc")
                        .then(Commands.literal("yes").executes(c -> { com.baraaoi.reallife.event.Visitors.answer(c.getSource().asPlayer(), true); return 1; }))
                        .then(Commands.literal("no").executes(c -> { com.baraaoi.reallife.event.Visitors.answer(c.getSource().asPlayer(), false); return 1; })))
                .then(Commands.literal("admin").requires(s -> s.hasPermissionLevel(2))
                        .then(Commands.literal("event").then(Commands.argument("kind", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.command.ISuggestionProvider.suggest(com.baraaoi.reallife.event.Visitors.eventIds(), b))
                                .executes(c -> event(c.getSource().asPlayer(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "kind")))))
                        .then(Commands.literal("events").executes(c -> {
                            ServerPlayerEntity ep = c.getSource().asPlayer();
                            String act = com.baraaoi.reallife.event.Visitors.activeKind(ep);
                            long t = com.baraaoi.reallife.event.Visitors.ticksToNext(ep);
                            BankService.msg(ep, act != null ? "§eСейчас у вас гость: " + act
                                    : "§eСледующий случайный гость примерно через §f" + (t / 1200) + " мин " + (t % 1200 / 20) + " с"
                                    + (com.baraaoi.reallife.Config.EVENTS_ENABLED.get() ? "" : " §c(события выключены в конфиге!)"));
                            return 1;
                        }))
                        .then(Commands.literal("money").then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(c -> money(c.getSource().asPlayer(), IntegerArgumentType.getInteger(c, "amount")))))
                        .then(Commands.literal("rating").then(Commands.argument("value", IntegerArgumentType.integer(300, 850))
                                .executes(c -> rating(c.getSource(), c.getSource().asPlayer(), IntegerArgumentType.getInteger(c, "value")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> rating(c.getSource(), EntityArgument.getPlayer(c, "player"), IntegerArgumentType.getInteger(c, "value"))))))
                        .then(Commands.literal("land").then(Commands.literal("reset").executes(c -> landReset(c.getSource().asPlayer()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> landReset(EntityArgument.getPlayer(c, "player"))))))
                        .then(Commands.literal("nextday").executes(c -> nextDay(c.getSource())))
                        .then(Commands.literal("overdue").then(Commands.argument("days", IntegerArgumentType.integer(0, 100))
                                .executes(c -> overdue(c.getSource().asPlayer(), IntegerArgumentType.getInteger(c, "days")))))
                        .then(Commands.literal("mfooverdue").then(Commands.argument("days", IntegerArgumentType.integer(0, 100))
                                .executes(c -> mfoOverdue(c.getSource().asPlayer(), IntegerArgumentType.getInteger(c, "days")))))
                        .then(Commands.literal("build")
                                .then(Commands.literal("bank").executes(c -> build(c.getSource().asPlayer(), true)))
                                .then(Commands.literal("mfo").executes(c -> build(c.getSource().asPlayer(), false)))
                                .then(Commands.literal("casino").executes(c -> {
                                    ServerPlayerEntity cp = c.getSource().asPlayer();
                                    net.minecraft.util.Direction cf = cp.getHorizontalFacing();
                                    Buildings.buildCasino(cp.world, cp.getPosition().offset(cf, 2), cf, false);
                                    return 1;
                                })))
                        .then(Commands.literal("free").executes(c -> free(c.getSource().asPlayer()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> free(EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("reset").executes(c -> reset(c.getSource().asPlayer()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> reset(EntityArgument.getPlayer(c, "player")))))));
    }

    private static int event(ServerPlayerEntity p, String kind) {
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

    private static Account acc(ServerPlayerEntity p) {
        return EconomyData.get(p.getServer()).get(p);
    }

    private static int info(ServerPlayerEntity p) throws CommandSyntaxException {
        Account a = acc(p);
        long today = BankService.today(p.getServer());
        int score = CreditRating.score(a, today);
        BankService.msg(p, "§6===== Реал Банк: " + p.getGameProfile().getName() + " ===== §8(Real Life v" + com.baraaoi.reallife.RealLife.version() + ")");
        BankService.msg(p, "Баланс: §e" + Finance.money(a.balance) + "§f, рейтинг: §b" + score + " " + CreditRating.category(score));
        BankService.msg(p, "Доступный кредит: §a" + Finance.money(CreditRating.maxLoan(a, today)) + "§f под " + Finance.percent(CreditRating.loanRate(score)) + "/день");
        if (a.hasLoan()) BankService.msg(p, "Кредит: " + Finance.money(a.loanPrincipal) + ", платёж " + Finance.money(a.loanPayment()) + "/день, осталось " + a.loanDays + " дн.");
        if (a.hasMortgage()) BankService.msg(p, "Ипотека: " + Finance.money(a.mortPrincipal) + ", платёж " + Finance.money(a.mortPayment()) + "/день, осталось " + a.mortDays + " дн.");
        if (a.arrears > 0.01) BankService.msg(p, "§cПросрочка: " + Finance.money(a.arrears) + " (" + a.overdueDays + " дн.)");
        if (a.hasBusiness) BankService.msg(p, "§2Подпольный цех: §fработает" + (a.hasLaundering ? ", ООО «Отмыв» оформлено" : ""));
        int land = EconomyData.get(p.getServer()).ownedCount(p.getUniqueID());
        if (land > 0) BankService.msg(p, "§3Участков в собственности: §f" + land);
        if (a.landCase) BankService.msg(p, "§cОткрыто дело о самострое! §7/reallife land");
        if (a.hasMfo()) BankService.msg(p, "§2МФО: §cдолг " + Finance.money(a.mfoDebt) + (a.mfoDays > 0 ? ", срок " + a.mfoDays + " дн." : ", просрочка " + a.mfoOverdueDays + " дн."));
        return 1;
    }

    private static int rating(CommandSource src, ServerPlayerEntity p, int value) {
        EconomyData data = EconomyData.get(p.getServer());
        Account a = data.get(p);
        long today = BankService.today(p.getServer());
        a.ratingBonus = CreditRating.bonusFor(a, today, value);
        data.markDirty();
        int now = CreditRating.score(a, today);
        src.sendFeedback(new StringTextComponent("§aРейтинг " + p.getGameProfile().getName() + ": §b" + now + " " + CreditRating.category(now)
                + " §7(поправка " + (a.ratingBonus >= 0 ? "+" : "") + a.ratingBonus + ")"), true);
        if (src.getEntity() != p) BankService.msg(p, BankService.BANK + "Ваш кредитный рейтинг пересмотрен: §b" + now);
        return 1;
    }

    private static int land(ServerPlayerEntity p) {
        EconomyData data = EconomyData.get(p.getServer());
        Account a = data.get(p);
        long chunk = EconomyData.chunkKey(p.getPosition());
        java.util.UUID owner = data.owner(chunk);
        net.minecraft.util.math.ChunkPos cp = new net.minecraft.util.math.ChunkPos(chunk);
        String who;
        if (owner == null) who = "§7ничей (купить участок 3×3 чанка вокруг: §f/reallife land buy§7, "
                + Finance.money(DebtManager.plotPrice(DebtManager.plotChunks(data, p.getUniqueID(), chunk).size())) + ")";
        else if (owner.equals(p.getUniqueID())) {
            int free = DebtManager.plotChunks(data, p.getUniqueID(), chunk).size();
            who = "§aваш" + (free > 0 ? " §7(докупить соседние " + free + " чанк(ов) 3×3: §f/reallife land buy§7, " + Finance.money(DebtManager.plotPrice(free)) + ")" : "");
        }
        else if (owner.equals(Buildings.CITY)) who = "§6городской (банк/МФО)";
        else {
            Account o = data.get(owner);
            who = "§cчужой" + (o != null && o.name != null && !o.name.isEmpty() ? " (" + o.name + ")" : "");
        }
        BankService.msg(p, "§3===== Участок " + cp.x + ", " + cp.z + " (X " + cp.getXStart() + ".." + cp.getXEnd() + ", Z " + cp.getZStart() + ".." + cp.getZEnd() + ") =====");
        BankService.msg(p, "Владелец: " + who);
        if (!p.getUniqueID().equals(owner)) {
            BankService.msg(p, "Ваших блоков здесь: §e" + data.placedCount(chunk, p.getUniqueID()) + "§f из §e"
                    + com.baraaoi.reallife.Config.LAND_THRESHOLD.get() + "§f до визита инспектора.");
        }
        BankService.msg(p, "Ваших участков всего: §e" + data.ownedCount(p.getUniqueID()));
        if (a.landCase) {
            net.minecraft.util.math.ChunkPos lc = new net.minecraft.util.math.ChunkPos(a.landChunk);
            long left = Math.max(0, a.landDeadline - p.world.getGameTime()) / 20;
            BankService.msg(p, String.format("§cОткрыто дело о самострое: чанк %d, %d. До сноса %d:%02d.", lc.x, lc.z, left / 60, left % 60));
        }
        return 1;
    }

    private static int landBuy(ServerPlayerEntity p) {
        if (p.world.getDimensionKey() != net.minecraft.world.World.OVERWORLD) {
            BankService.msg(p, "§cУчастки продаются только в обычном мире.");
            return 0;
        }
        EconomyData data = EconomyData.get(p.getServer());
        return DebtManager.buyLand(p, data.get(p), EconomyData.chunkKey(p.getPosition())) ? 1 : 0;
    }

    private static int landReset(ServerPlayerEntity p) {
        EconomyData data = EconomyData.get(p.getServer());
        Account a = data.get(p);
        a.landCase = false;
        for (java.util.Map<java.util.UUID, java.util.Set<Long>> m : data.placed.values()) m.remove(p.getUniqueID());
        data.markDirty();
        BankService.msg(p, "§aДела о самострое и счётчики блоков сброшены.");
        return 1;
    }

    private static int money(ServerPlayerEntity p, int amount) {
        Account a = acc(p);
        a.balance += amount;
        EconomyData.get(p.getServer()).markDirty();
        BankService.msg(p, "§aБаланс изменён: " + Finance.money(a.balance));
        return 1;
    }

    private static int nextDay(CommandSource src) {
        MinecraftServer server = src.getServer();
        BankService.processDay(server, EconomyData.get(server));
        src.sendFeedback(new StringTextComponent("§aОбработан один банковский день."), true);
        return 1;
    }

    private static int overdue(ServerPlayerEntity p, int days) {
        Account a = acc(p);
        a.overdueDays = days;
        if (days > 0 && a.arrears < 100) a.arrears = 100;
        if (days == 0) { a.clearArrears(); a.wanted = false; }
        EconomyData.get(p.getServer()).markDirty();
        BankService.msg(p, "§aПросрочка банка: " + days + " дн., " + Finance.money(a.arrears));
        return 1;
    }

    private static int mfoOverdue(ServerPlayerEntity p, int days) {
        Account a = acc(p);
        if (!a.hasMfo()) { a.mfoDebt = 500; a.mfoPrincipal = 500; }
        a.mfoDays = 0;
        a.mfoOverdueDays = days;
        if (days == 0) a.clearMfo();
        EconomyData.get(p.getServer()).markDirty();
        BankService.msg(p, "§aПросрочка МФО: " + days + " дн., долг " + Finance.money(a.mfoDebt));
        return 1;
    }

    private static int build(ServerPlayerEntity p, boolean bank) {
        Direction f = p.getHorizontalFacing();
        if (bank) Buildings.buildBank(p.world, p.getPosition().offset(f, 2), f);
        else Buildings.buildMfo(p.world, p.getPosition().offset(f, 2), f);
        return 1;
    }

    private static int free(ServerPlayerEntity p) {
        EconomyData data = EconomyData.get(p.getServer());
        Account a = data.get(p);
        if (a.captivity != Account.FREE) DebtManager.release(p.getServer(), data, p, a);
        return 1;
    }

    private static int reset(ServerPlayerEntity p) {
        EconomyData.get(p.getServer()).reset(p.getUniqueID());
        BankService.msg(p, "§aСчёт сброшен.");
        return 1;
    }
}
