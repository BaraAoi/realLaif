package com.baraaoi.reallife.client;

import com.baraaoi.reallife.container.FinanceContainer;
import com.baraaoi.reallife.economy.Actions;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.economy.HouseType;
import com.baraaoi.reallife.network.ActionPacket;
import com.baraaoi.reallife.network.Network;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

/**
 * Интерфейс банка (вкладки «Счёт», «Кредит», «Ипотека») и МФО.
 */
public class FinanceScreen extends ContainerScreen<FinanceContainer> {
    private static final int[] TERMS = {5, 10, 20};
    private static int tab = 0;
    private static int loanAmount = 500;
    private static int termIdx = 1;
    private static int house = 0;
    private static int mfoAmount = 1000;

    private final boolean mfo;

    public FinanceScreen(FinanceContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.mfo = container.isMfo();
        this.xSize = FinanceContainer.WIDTH;
        this.ySize = FinanceContainer.HEIGHT;
    }

    private static CompoundNBT d() {
        return ClientAccountData.get();
    }

    private void send(int action, int a, int b) {
        Network.CHANNEL.sendToServer(new ActionPacket(action, a, b));
    }

    private Button btn(int x, int y, int w, String text, Button.IPressable onPress) {
        return this.addButton(new Button(guiLeft + x, guiTop + y, w, 14, new StringTextComponent(text), onPress));
    }

    private void rebuild() {
        this.init(this.minecraft, this.width, this.height);
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
        int[] notes = {10, 100, 1000, 5000};
        for (int i = 0; i < notes.length; i++) {
            final int n = notes[i];
            btn(8 + i * 52, 124, 50, "Снять " + n, b -> send(Actions.WITHDRAW, n, 0));
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
        btn(8, 124, 66, "Внести 1000", b -> send(Actions.REPAY_MORTGAGE, 1000, 0));
        btn(76, 124, 66, "Погасить всё", b -> send(Actions.REPAY_MORTGAGE, 0, 0));
        btn(144, 124, 70, "Новый ордер", b -> send(Actions.REISSUE_ORDER, 0, 0));
    }

    private void initMfo() {
        btn(8, 38, 36, "-1000", b -> { mfoAmount = Math.max(100, mfoAmount - 1000); });
        btn(46, 38, 30, "-100", b -> { mfoAmount = Math.max(100, mfoAmount - 100); });
        btn(144, 38, 30, "+100", b -> { mfoAmount = Math.min(d().getInt("mfoMax") > 0 ? d().getInt("mfoMax") : 3000, mfoAmount + 100); });
        btn(176, 38, 36, "+1000", b -> { mfoAmount = Math.min(d().getInt("mfoMax") > 0 ? d().getInt("mfoMax") : 3000, mfoAmount + 1000); });
        btn(8, 82, 110, "§2§lВЗЯТЬ ДЕНЬГИ!", b -> send(Actions.MFO_TAKE, mfoAmount, 0));
        btn(8, 124, 100, "Оплатить из слота", b -> send(Actions.MFO_PAY_SLOT, 0, 0));
        btn(110, 124, 104, "Перевод из банка", b -> send(Actions.MFO_PAY_BANK, 0, 0));
    }

    // ------------------------------------------------------------------ отрисовка

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);
        super.render(ms, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(ms, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack ms, float partialTicks, int mouseX, int mouseY) {
        int x = guiLeft, y = guiTop;
        int border = mfo ? 0xFF2E7D32 : 0xFFC9A227;
        int bg = mfo ? 0xFF16201A : 0xFF1B1D2A;
        fill(ms, x - 2, y - 2, x + xSize + 2, y + ySize + 2, border);
        fill(ms, x, y, x + xSize, y + ySize, bg);
        fill(ms, x + 4, y + 21, x + xSize - 4, y + 142, mfo ? 0xFF1F2D24 : 0xFF252838);
        slotBg(ms, x + FinanceContainer.SLOT_X, y + FinanceContainer.SLOT_Y);
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                slotBg(ms, x + FinanceContainer.INV_X + c * 18, y + FinanceContainer.INV_Y + r * 18);
        for (int c = 0; c < 9; c++) slotBg(ms, x + FinanceContainer.INV_X + c * 18, y + FinanceContainer.INV_Y + 58);
    }

    private void slotBg(MatrixStack ms, int x, int y) {
        fill(ms, x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        fill(ms, x, y, x + 17, y + 17, 0xFFFFFFFF);
        fill(ms, x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    private void text(MatrixStack ms, String s, int x, int y) {
        this.font.drawString(ms, s, x, y, 0xFFFFFF);
    }

    private void small(MatrixStack ms, String s, int x, int y, int color) {
        ms.push();
        ms.translate(x, y, 0);
        ms.scale(0.75F, 0.75F, 1.0F);
        this.font.drawString(ms, s, 0, 0, color);
        ms.pop();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack ms, int mouseX, int mouseY) {
        CompoundNBT t = d();
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
        if (t.getBoolean("wanted")) {
            text(ms, "§4§lВЫ В РОЗЫСКЕ! Полиция уже едет.", 8, 144 - 0);
        } else if (t.getDouble("arrears") > 0.01) {
            String extra = t.getInt("laborLeft") > 0 ? " §6копать: " + t.getInt("laborLeft") : "";
            small(ms, "§cПРОСРОЧКА " + Finance.money(t.getDouble("arrears")) + " (" + t.getInt("overdue") + " дн.)" + extra, 8, 143, 0xFFFFFF);
        }
    }

    private void drawAccount(MatrixStack ms, CompoundNBT t) {
        int score = t.getInt("score");
        text(ms, "Баланс: §e" + Finance.money(t.getDouble("balance")), 8, 26);
        text(ms, "Кредитный рейтинг: §b" + score + " " + t.getString("category"), 8, 38);
        small(ms, "Средний доход в день: " + Finance.money(t.getDouble("income"))
                + "   Вклад: " + Finance.percent(t.getDouble("depositRate")) + "/день", 8, 51, 0xCCCCCC);
        small(ms, "Платежей вовремя: " + t.getInt("onTime") + "   Дней просрочки: " + t.getInt("lateDays")
                + "   Дефолтов: " + t.getInt("defaults"), 8, 61, 0xCCCCCC);
        double due = t.getDouble("loanPayment") + t.getDouble("mortPayment");
        small(ms, "Ежедневное списание: " + (due > 0 ? "§e" + Finance.money(due) : "нет")
                + "§r   Допустимая нагрузка: " + Finance.money(t.getDouble("burdenLimit")) + "/день", 8, 71, 0xCCCCCC);
        small(ms, "§7Принимаем ресурсы: алмазы, изумруды, золото, железо, уголь,", 8, 83, 0x999999);
        small(ms, "§7редстоун, лазурит, незерит, еду, брёвна и купюры.", 8, 91, 0x999999);
        small(ms, "§7Платежи списываются со счёта каждое утро — держите деньги в банке!", 8, 99, 0x999999);
    }

    private void drawCredit(MatrixStack ms, CompoundNBT t) {
        double rate = t.getDouble("loanRate");
        double max = t.getDouble("maxLoan");
        text(ms, "Лимит: §a" + Finance.money(max) + "§f   Ставка: §e" + Finance.percent(rate) + "/день", 8, 26);
        String amt = Finance.money(loanAmount);
        text(ms, (loanAmount > max ? "§c" : "§f") + amt, 110 - this.font.getStringWidth(amt) / 2, 41);
        double pay = Finance.annuity(loanAmount, rate, TERMS[termIdx]);
        small(ms, "Платёж: §e" + Finance.money(pay) + "/день§r, переплата: §c" + Finance.money(pay * TERMS[termIdx] - loanAmount), 8, 74, 0xFFFFFF);
        fill(ms, 8, 84, 212, 85, 0xFF555555);
        if (t.getDouble("loanPrincipal") > 0) {
            text(ms, "Кредит: остаток §e" + Finance.money(t.getDouble("loanPrincipal")), 8, 90);
            small(ms, "Платёж " + Finance.money(t.getDouble("loanPayment")) + "/день, осталось " + t.getInt("loanDays")
                    + " дн., ставка " + Finance.percent(t.getDouble("loanActiveRate")), 8, 102, 0xCCCCCC);
        } else {
            text(ms, "§7Активного кредита нет.", 8, 90);
            small(ms, "§7Лимит растёт с рейтингом, доходом и накоплениями.", 8, 102, 0x999999);
        }
    }

    private void drawMortgage(MatrixStack ms, CompoundNBT t) {
        HouseType h = HouseType.values()[house];
        String title = "§e" + h.title;
        text(ms, title, 110 - this.font.getStringWidth(title) / 2, 27);
        small(ms, "§7" + h.desc, 8, 41, 0xAAAAAA);
        ListNBT list = t.getList("houses", 10);
        CompoundNBT ht = house < list.size() ? list.getCompound(house) : new CompoundNBT();
        small(ms, "Цена: §e" + Finance.money(h.price) + "§r   Срок: " + h.termDays + " дн.   Мин. рейтинг: " + h.minScore, 8, 51, 0xFFFFFF);
        small(ms, "Взнос " + Finance.percent(t.getDouble("downPct")) + ": §e" + Finance.money(ht.getDouble("down"))
                + "§r   Ставка: " + Finance.percent(ht.getDouble("rate")) + "/день", 8, 60, 0xFFFFFF);
        small(ms, "Платёж: §e" + Finance.money(ht.getDouble("pay")) + "/день", 8, 69, 0xFFFFFF);
        String reason = ht.getString("reason");
        small(ms, reason.isEmpty() ? "§a✔ Предварительно одобрено" : "§c✘ " + reason, 8, 78, 0xFFFFFF);
        fill(ms, 8, 102, 212, 103, 0xFF555555);
        int mh = t.getInt("mortHouse");
        if (mh >= 0 && mh < HouseType.values().length) {
            small(ms, "Ипотека: §e" + HouseType.values()[mh].title + "§r, остаток §e" + Finance.money(t.getDouble("mortPrincipal")), 8, 106, 0xFFFFFF);
            small(ms, "Платёж " + Finance.money(t.getDouble("mortPayment")) + "/день, осталось " + t.getInt("mortDays") + " дн."
                    + (t.getBoolean("houseBuilt") ? "" : " §d(дом не построен)"), 8, 115, 0xCCCCCC);
        } else {
            small(ms, "§7Ипотеки нет. Дом строит бригада банка — бесплатно.", 8, 106, 0x999999);
            small(ms, "§7Не платите — придут Жора, Александр, потом полиция.", 8, 115, 0x999999);
        }
    }

    private void drawMfo(MatrixStack ms, CompoundNBT t) {
        text(ms, "§2§lШАЙКИН И КО §r§a— займы без отказа!", 8, 7);
        text(ms, "§fДеньги за 5 минут! Без проверок!", 8, 25);
        String amt = Finance.money(mfoAmount);
        text(ms, "§e" + amt, 110 - this.font.getStringWidth(amt) / 2, 41);
        double rate = t.getDouble("mfoRate");
        double comm = t.getDouble("mfoCommission");
        int term = t.getInt("mfoTerm");
        double cash = Math.floor(mfoAmount * (1 - comm) / 10.0) * 10.0;
        double back = mfoAmount * Math.pow(1 + rate, term) + 5 * term;
        small(ms, "На руки: §a" + Finance.money(cash) + "§r   Вернуть через " + term + " дн.: §c~" + Finance.money(back), 8, 56, 0xFFFFFF);
        small(ms, "§8*комиссия и страховка " + Finance.percent(comm) + ", " + Finance.percent(rate)
                + "/день сложный %, СМС 5₽/день,", 8, 64, 0x777777);
        small(ms, "§8просрочка " + Finance.percent(t.getDouble("mfoOverdueRate")) + "/день + штраф 50₽. Коллекторы действуют.", 8, 71, 0x777777);
        fill(ms, 8, 100, 212, 101, 0xFF335533);
        double debt = t.getDouble("mfoDebt");
        if (debt > 0.009) {
            text(ms, "Ваш долг: §c" + Finance.money(debt), 8, 104);
            int days = t.getInt("mfoDays");
            small(ms, days > 0 ? "Вернуть в течение " + days + " дн." : "§4ПРОСРОЧКА " + t.getInt("mfoOverdue") + " дн.! Мы едем.", 120, 106, 0xFFFFFF);
            small(ms, "§7Купюры по номиналу, ценности по 60%. Сдачи нет.", 8, 115, 0x999999);
        } else {
            text(ms, "§7Долгов нет. Пока что.", 8, 104);
            small(ms, "§7Банковский счёт: " + Finance.money(t.getDouble("balance")), 8, 115, 0x999999);
        }
    }
}
