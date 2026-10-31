package com.baraaoi.reallife.client;

import com.baraaoi.reallife.container.FinanceContainer;
import com.baraaoi.reallife.economy.Actions;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.economy.HouseType;
import com.baraaoi.reallife.network.ActionPacket;
import com.baraaoi.reallife.network.Network;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

/**
 * Интерфейс банка (вкладки «Счёт», «Кредит», «Ипотека») и МФО.
 */
public class FinanceScreen extends AbstractContainerScreen<FinanceContainer> {
    private static final int[] TERMS = {5, 10, 20};
    private static int tab = 0;
    private static int loanAmount = 500;
    private static int termIdx = 1;
    private static int house = 0;
    private static int mfoAmount = 1000;
    private static int mfoTab = 0;

    private final boolean mfo;

    public FinanceScreen(FinanceContainer container, Inventory inv, Component title) {
        super(container, inv, title, FinanceContainer.WIDTH, FinanceContainer.HEIGHT);
        this.mfo = menu.isMfo();
    }

    private static CompoundTag d() {
        return ClientAccountData.get();
    }

    private void send(int action, int a, int b) {
        Network.toServer(new ActionPacket(action, a, b));
    }

    private Button btn(int x, int y, int w, String text, Button.OnPress onPress) {
        return this.addRenderableWidget(Button.builder(Component.literal(text), onPress).bounds(leftPos + x, topPos + y, w, 14).build());
    }

    private void rebuild() {
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        super.init();
        if (mfo) {
            initMfo();
            return;
        }
        String[] names = {"Счёт", "Кредит", "Ипотека"};
        for (int i = 0; i < names.length; i++) {
            final int t = i;
            btn(6 + i * 62, 4, 60, (tab == i ? "§e" : "") + names[i], b -> { tab = t; rebuild(); });
        }
        if (tab == 0) initAccount();
        else if (tab == 1) initCredit();
        else initMortgage();
    }

    private void initAccount() {
        btn(8, 106, 110, "Внести из слота →", b -> send(Actions.DEPOSIT, 0, 0));
        int[] notes = {10, 100, 500, 1000, 5000};
        for (int i = 0; i < notes.length; i++) {
            final int n = notes[i];
            btn(8 + i * 42, 124, 40, "−" + n, b -> send(Actions.WITHDRAW, n, 0));
        }
    }

    private void initCredit() {
        btn(8, 38, 36, "-1000", b -> { loanAmount = Math.max(50, loanAmount - 1000); });
        btn(46, 38, 30, "-100", b -> { loanAmount = Math.max(50, loanAmount - 100); });
        btn(144, 38, 30, "+100", b -> { loanAmount += 100; });
        btn(176, 38, 36, "+1000", b -> { loanAmount += 1000; });
        for (int i = 0; i < TERMS.length; i++) {
            final int t = i;
            btn(8 + i * 44, 56, 42, (termIdx == i ? "§e" : "") + TERMS[i] + " дн.", b -> { termIdx = t; rebuild(); });
        }
        btn(144, 56, 68, "§aОформить", b -> send(Actions.TAKE_LOAN, loanAmount, termIdx));
        btn(8, 124, 66, "Погасить 100", b -> send(Actions.REPAY_LOAN, 100, 0));
        btn(76, 124, 70, "Погасить 1000", b -> send(Actions.REPAY_LOAN, 1000, 0));
        btn(148, 124, 66, "Погасить всё", b -> send(Actions.REPAY_LOAN, 0, 0));
    }

    private void initMortgage() {
        int n = HouseType.values().length;
        btn(8, 24, 16, "<", b -> { house = (house + n - 1) % n; });
        btn(196, 24, 16, ">", b -> { house = (house + 1) % n; });
        btn(8, 86, 120, "§aОформить ипотеку", b -> send(Actions.MORTGAGE, house, 0));
        btn(132, 86, 80, "§eКупить сразу", b -> send(Actions.BUY_HOUSE, house, 0));
        btn(8, 124, 66, "Внести 1000", b -> send(Actions.REPAY_MORTGAGE, 1000, 0));
        btn(76, 124, 66, "Погасить всё", b -> send(Actions.REPAY_MORTGAGE, 0, 0));
        btn(144, 124, 70, "Новый ордер", b -> send(Actions.REISSUE_ORDER, 0, 0));
    }

    private void initMfo() {
        String[] names = {"Займ", "Бизнес"};
        for (int i = 0; i < names.length; i++) {
            final int t = i;
            btn(150 + i * 52, 4, 50, (mfoTab == i ? "§e" : "") + names[i], b -> { mfoTab = t; rebuild(); });
        }
        if (mfoTab == 1) {
            btn(8, 52, 120, "§aКупить станок", b -> send(Actions.MFO_BUY_PRINTER, 0, 0));
            btn(8, 124, 140, "§2Отмыть из слота (−50%)", b -> send(Actions.MFO_LAUNDER, 0, 0));
            return;
        }
        btn(8, 38, 36, "-1000", b -> { mfoAmount = Math.max(100, mfoAmount - 1000); });
        btn(46, 38, 30, "-100", b -> { mfoAmount = Math.max(100, mfoAmount - 100); });
        btn(144, 38, 30, "+100", b -> { mfoAmount = Math.min(d().getIntOr("mfoMax", 0) > 0 ? d().getIntOr("mfoMax", 0) : 3000, mfoAmount + 100); });
        btn(176, 38, 36, "+1000", b -> { mfoAmount = Math.min(d().getIntOr("mfoMax", 0) > 0 ? d().getIntOr("mfoMax", 0) : 3000, mfoAmount + 1000); });
        btn(8, 82, 110, "§2§lВЗЯТЬ ДЕНЬГИ!", b -> send(Actions.MFO_TAKE, mfoAmount, 0));
        btn(8, 124, 100, "Оплатить из слота", b -> send(Actions.MFO_PAY_SLOT, 0, 0));
        btn(110, 124, 104, "Перевод из банка", b -> send(Actions.MFO_PAY_BANK, 0, 0));
    }

    // ------------------------------------------------------------------ отрисовка


    @Override
    public void extractBackground(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(ms, mouseX, mouseY, partialTicks);
        int x = leftPos, y = topPos;
        int border = mfo ? 0xFF2E7D32 : 0xFFC9A227;
        int bg = mfo ? 0xFF16201A : 0xFF1B1D2A;
        ms.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, border);
        ms.fill(x, y, x + imageWidth, y + imageHeight, bg);
        ms.fill(x + 4, y + 21, x + imageWidth - 4, y + 142, mfo ? 0xFF1F2D24 : 0xFF252838);
        slotBg(ms, x + FinanceContainer.SLOT_X, y + FinanceContainer.SLOT_Y);
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                slotBg(ms, x + FinanceContainer.INV_X + c * 18, y + FinanceContainer.INV_Y + r * 18);
        for (int c = 0; c < 9; c++) slotBg(ms, x + FinanceContainer.INV_X + c * 18, y + FinanceContainer.INV_Y + 58);
    }

    private void slotBg(GuiGraphicsExtractor ms, int x, int y) {
        ms.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        ms.fill(x, y, x + 17, y + 17, 0xFFFFFFFF);
        ms.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    private void text(GuiGraphicsExtractor ms, String s, int x, int y) {
        GuiUtil.text(ms, this.font, s, x, y, 0xFFFFFF);
    }

    private void small(GuiGraphicsExtractor ms, String s, int x, int y, int color) {
        ms.pose().pushMatrix();
        ms.pose().translate((float) (x), (float) (y));
        ms.pose().scale(0.75F, 0.75F);
        GuiUtil.text(ms, this.font, s, 0, 0, color);
        ms.pose().popMatrix();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ms, int mouseX, int mouseY) {
        CompoundTag t = d();
        small(ms, "Слот", FinanceContainer.SLOT_X, FinanceContainer.SLOT_Y - 9, 0xAAAAAA);
        if (mfo) {
            drawMfo(ms, t);
            return;
        }
        text(ms, "§6«Реал Банк»", 194, 7);
        if (tab == 0) drawAccount(ms, t);
        else if (tab == 1) drawCredit(ms, t);
        else drawMortgage(ms, t);
        // строка состояния
        if (t.getBooleanOr("wanted", false)) {
            text(ms, "§4§lВЫ В РОЗЫСКЕ! Полиция уже едет.", 8, 144 - 0);
        } else if (t.getDoubleOr("arrears", 0.0) > 0.01) {
            String extra = t.getIntOr("laborLeft", 0) > 0 ? " §6копать: " + t.getIntOr("laborLeft", 0) : "";
            small(ms, "§cПРОСРОЧКА " + Finance.money(t.getDoubleOr("arrears", 0.0)) + " (" + t.getIntOr("overdue", 0) + " дн.)" + extra, 8, 143, 0xFFFFFF);
        }
    }

    private void drawAccount(GuiGraphicsExtractor ms, CompoundTag t) {
        int score = t.getIntOr("score", 0);
        text(ms, "Баланс: §e" + Finance.money(t.getDoubleOr("balance", 0.0)), 8, 26);
        text(ms, "Кредитный рейтинг: §b" + score + " " + t.getStringOr("category", ""), 8, 38);
        small(ms, "Средний доход в день: " + Finance.money(t.getDoubleOr("income", 0.0))
                + "   Вклад: " + Finance.percent(t.getDoubleOr("depositRate", 0.0)) + "/день", 8, 51, 0xCCCCCC);
        small(ms, "Платежей вовремя: " + t.getIntOr("onTime", 0) + "   Дней просрочки: " + t.getIntOr("lateDays", 0)
                + "   Дефолтов: " + t.getIntOr("defaults", 0), 8, 61, 0xCCCCCC);
        double due = t.getDoubleOr("loanPayment", 0.0) + t.getDoubleOr("mortPayment", 0.0);
        small(ms, "Ежедневное списание: " + (due > 0 ? "§e" + Finance.money(due) : "нет")
                + "§r   Допустимая нагрузка: " + Finance.money(t.getDoubleOr("burdenLimit", 0.0)) + "/день", 8, 71, 0xCCCCCC);
        small(ms, "§7Принимаем ресурсы: алмазы, изумруды, золото, железо, уголь,", 8, 83, 0x999999);
        small(ms, "§7редстоун, лазурит, незерит, еду, брёвна и купюры.", 8, 91, 0x999999);
        small(ms, "§7Платежи списываются со счёта каждое утро — держите деньги в банке!", 8, 99, 0x999999);
    }

    private void drawCredit(GuiGraphicsExtractor ms, CompoundTag t) {
        double rate = t.getDoubleOr("loanRate", 0.0);
        double max = t.getDoubleOr("maxLoan", 0.0);
        text(ms, "Лимит: §a" + Finance.money(max) + "§f   Ставка: §e" + Finance.percent(rate) + "/день", 8, 26);
        String amt = Finance.money(loanAmount);
        text(ms, (loanAmount > max ? "§c" : "§f") + amt, 110 - this.font.width(amt) / 2, 41);
        double pay = Finance.annuity(loanAmount, rate, TERMS[termIdx]);
        small(ms, "Платёж: §e" + Finance.money(pay) + "/день§r, переплата: §c" + Finance.money(pay * TERMS[termIdx] - loanAmount), 8, 74, 0xFFFFFF);
        ms.fill(8, 84, 212, 85, 0xFF555555);
        if (t.getDoubleOr("loanPrincipal", 0.0) > 0) {
            text(ms, "Кредит: остаток §e" + Finance.money(t.getDoubleOr("loanPrincipal", 0.0)), 8, 90);
            small(ms, "Платёж " + Finance.money(t.getDoubleOr("loanPayment", 0.0)) + "/день, осталось " + t.getIntOr("loanDays", 0)
                    + " дн., ставка " + Finance.percent(t.getDoubleOr("loanActiveRate", 0.0)), 8, 102, 0xCCCCCC);
        } else {
            text(ms, "§7Активного кредита нет.", 8, 90);
            small(ms, "§7Лимит растёт с рейтингом, доходом и накоплениями.", 8, 102, 0x999999);
        }
    }

    private void drawMortgage(GuiGraphicsExtractor ms, CompoundTag t) {
        HouseType h = HouseType.values()[house];
        String title = "§e" + h.title;
        text(ms, title, 110 - this.font.width(title) / 2, 27);
        small(ms, "§7" + h.desc, 8, 41, 0xAAAAAA);
        ListTag list = t.getListOrEmpty("houses");
        CompoundTag ht = house < list.size() ? list.getCompoundOrEmpty(house) : new CompoundTag();
        small(ms, "Цена: §e" + Finance.money(h.price) + "§r   Срок: " + h.termDays + " дн.   Мин. рейтинг: " + h.minScore, 8, 51, 0xFFFFFF);
        small(ms, "Взнос " + Finance.percent(t.getDoubleOr("downPct", 0.0)) + ": §e" + Finance.money(ht.getDoubleOr("down", 0.0))
                + "§r   Ставка: " + Finance.percent(ht.getDoubleOr("rate", 0.0)) + "/день", 8, 60, 0xFFFFFF);
        small(ms, "Платёж: §e" + Finance.money(ht.getDoubleOr("pay", 0.0)) + "/день§r   Без ипотеки: §a" + Finance.money(h.price), 8, 69, 0xFFFFFF);
        String reason = ht.getStringOr("reason", "");
        small(ms, reason.isEmpty() ? "§a✔ Предварительно одобрено" : "§c✘ " + reason, 8, 78, 0xFFFFFF);
        ms.fill(8, 102, 212, 103, 0xFF555555);
        int mh = t.getIntOr("mortHouse", 0);
        if (mh >= 0 && mh < HouseType.values().length) {
            small(ms, "Ипотека: §e" + HouseType.values()[mh].title + "§r, остаток §e" + Finance.money(t.getDoubleOr("mortPrincipal", 0.0)), 8, 106, 0xFFFFFF);
            small(ms, "Платёж " + Finance.money(t.getDoubleOr("mortPayment", 0.0)) + "/день, осталось " + t.getIntOr("mortDays", 0) + " дн."
                    + (t.getBooleanOr("houseBuilt", false) ? "" : " §d(дом не построен)"), 8, 115, 0xCCCCCC);
        } else {
            small(ms, "§7Ипотеки нет. Дом строит бригада банка — бесплатно.", 8, 106, 0x999999);
            small(ms, "§7«Купить сразу» — полная цена (счёт + наличка), без рейтинга.", 8, 115, 0x999999);
        }
    }

    private void drawBusiness(GuiGraphicsExtractor ms, CompoundTag t) {
        text(ms, "§2§lШАЙКИН И КО", 8, 7);
        text(ms, "§fПечатные станки §7(ставишь сам — дома или на участке)", 8, 25);
        small(ms, "§7Станок — §e" + Finance.money(t.getIntOr("printerPrice", 0)) + "§7. Номинал выбираешь в станке: 10₽/5с, 100₽/1м,", 8, 36, 0xFFFFFF);
        small(ms, "§7500₽/5м, 1000₽/10м, 5000₽/50м. Печать — в «Сундук цеха» или в лоток.", 8, 44, 0xFFFFFF);
        if (t.getBooleanOr("hasBusiness", false)) {
            small(ms, "§a✔ Куплено станков: " + t.getIntOr("printers", 0) + ", подключено: " + t.getIntOr("printersOn", 0), 132, 56, 0xFFFFFF);
            small(ms, t.getBooleanOr("chestLinked", false) ? "§a✔ Сундук цеха на месте" : "§c✘ Сундук цеха не поставлен", 132, 64, 0xFFFFFF);
        } else {
            small(ms, "§7Первая покупка = станок + «Сундук цеха».", 132, 56, 0xFFFFFF);
        }
        ms.fill(8, 72, 212, 73, 0xFF335533);
        small(ms, "§cСтанки прячь! §7Увидит прохожий — настучит, придёт ОБЭП", 8, 78, 0xFFFFFF);
        small(ms, "§7и прокопает к станку даже сквозь стены. Чем больше станков,", 8, 86, 0xFFFFFF);
        small(ms, "§7тем чаще ходят «подозрительные типы».", 8, 94, 0xFFFFFF);
        small(ms, "§7Отмыв здесь: −" + Finance.percent(t.getDoubleOr("mfoLaunderCommission", 0.0)) + ". Дешевле — через кассу своего заведения.", 8, 104, 0xFFFFFF);
        small(ms, "§8Банк палит грязные в " + Finance.percent(t.getDoubleOr("dirtyChance", 0.0)) + " случаев: изымет и снизит рейтинг.", 8, 113, 0x777777);
    }

    private void drawMfo(GuiGraphicsExtractor ms, CompoundTag t) {
        if (mfoTab == 1) {
            drawBusiness(ms, t);
            return;
        }
        text(ms, "§2§lШАЙКИН И КО §r§a— займы!", 8, 7);
        text(ms, "§fДеньги за 5 минут! Без проверок!", 8, 25);
        String amt = Finance.money(mfoAmount);
        text(ms, "§e" + amt, 110 - this.font.width(amt) / 2, 41);
        double rate = t.getDoubleOr("mfoRate", 0.0);
        double comm = t.getDoubleOr("mfoCommission", 0.0);
        int term = t.getIntOr("mfoTerm", 0);
        double cash = Math.floor(mfoAmount * (1 - comm) / 10.0) * 10.0;
        double back = mfoAmount * Math.pow(1 + rate, term) + 5 * term;
        small(ms, "На руки: §a" + Finance.money(cash) + "§r   Вернуть через " + term + " дн.: §c~" + Finance.money(back), 8, 56, 0xFFFFFF);
        small(ms, "§8*комиссия и страховка " + Finance.percent(comm) + ", " + Finance.percent(rate)
                + "/день сложный %, СМС 5₽/день,", 8, 64, 0x777777);
        small(ms, "§8просрочка " + Finance.percent(t.getDoubleOr("mfoOverdueRate", 0.0)) + "/день + штраф 50₽. Коллекторы действуют.", 8, 71, 0x777777);
        ms.fill(8, 100, 212, 101, 0xFF335533);
        double debt = t.getDoubleOr("mfoDebt", 0.0);
        if (debt > 0.009) {
            text(ms, "Ваш долг: §c" + Finance.money(debt), 8, 104);
            int days = t.getIntOr("mfoDays", 0);
            small(ms, days > 0 ? "Вернуть в течение " + days + " дн." : "§4ПРОСРОЧКА " + t.getIntOr("mfoOverdue", 0) + " дн.! Мы едем.", 120, 106, 0xFFFFFF);
            small(ms, "§7Купюры по номиналу, ценности по 60%. Сдачи нет.", 8, 115, 0x999999);
        } else {
            text(ms, "§7Долгов нет. Пока что.", 8, 104);
            small(ms, "§7Банковский счёт: " + Finance.money(t.getDoubleOr("balance", 0.0)), 8, 115, 0x999999);
        }
    }
}
