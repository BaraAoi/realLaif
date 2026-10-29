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
                .then(Commands.literal("admin").requires(s -> s.hasPermissionLevel(2))
                        .then(Commands.literal("money").then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(c -> money(c.getSource().asPlayer(), IntegerArgumentType.getInteger(c, "amount")))))
                        .then(Commands.literal("nextday").executes(c -> nextDay(c.getSource())))
                        .then(Commands.literal("overdue").then(Commands.argument("days", IntegerArgumentType.integer(0, 100))
                                .executes(c -> overdue(c.getSource().asPlayer(), IntegerArgumentType.getInteger(c, "days")))))
                        .then(Commands.literal("mfooverdue").then(Commands.argument("days", IntegerArgumentType.integer(0, 100))
                                .executes(c -> mfoOverdue(c.getSource().asPlayer(), IntegerArgumentType.getInteger(c, "days")))))
                        .then(Commands.literal("build")
                                .then(Commands.literal("bank").executes(c -> build(c.getSource().asPlayer(), true)))
                                .then(Commands.literal("mfo").executes(c -> build(c.getSource().asPlayer(), false))))
                        .then(Commands.literal("free").executes(c -> free(c.getSource().asPlayer()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> free(EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("reset").executes(c -> reset(c.getSource().asPlayer()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> reset(EntityArgument.getPlayer(c, "player")))))));
    }

    private static Account acc(ServerPlayerEntity p) {
        return EconomyData.get(p.getServer()).get(p);
    }

    private static int info(ServerPlayerEntity p) throws CommandSyntaxException {
        Account a = acc(p);
        long today = BankService.today(p.getServer());
        int score = CreditRating.score(a, today);
        BankService.msg(p, "§6===== Реал Банк: " + p.getGameProfile().getName() + " =====");
        BankService.msg(p, "Баланс: §e" + Finance.money(a.balance) + "§f, рейтинг: §b" + score + " " + CreditRating.category(score));
        BankService.msg(p, "Доступный кредит: §a" + Finance.money(CreditRating.maxLoan(a, today)) + "§f под " + Finance.percent(CreditRating.loanRate(score)) + "/день");
        if (a.hasLoan()) BankService.msg(p, "Кредит: " + Finance.money(a.loanPrincipal) + ", платёж " + Finance.money(a.loanPayment()) + "/день, осталось " + a.loanDays + " дн.");
        if (a.hasMortgage()) BankService.msg(p, "Ипотека: " + Finance.money(a.mortPrincipal) + ", платёж " + Finance.money(a.mortPayment()) + "/день, осталось " + a.mortDays + " дн.");
        if (a.arrears > 0.01) BankService.msg(p, "§cПросрочка: " + Finance.money(a.arrears) + " (" + a.overdueDays + " дн.)");
        if (a.hasMfo()) BankService.msg(p, "§2МФО: §cдолг " + Finance.money(a.mfoDebt) + (a.mfoDays > 0 ? ", срок " + a.mfoDays + " дн." : ", просрочка " + a.mfoOverdueDays + " дн."));
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
