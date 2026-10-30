package com.baraaoi.reallife.client;

import com.baraaoi.reallife.economy.Actions;
import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.economy.Wholesale;
import com.baraaoi.reallife.network.ActionPacket;
import com.baraaoi.reallife.network.Network;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.StringTextComponent;

import java.util.List;
import java.util.Locale;

/** Экран смартфона: приложение «Реал Банк». */
public class PhoneScreen extends Screen {
    private static final int W = 248, H = 214;
    private static final String[] TABS = {"Счёт", "Снять", "Кредит", "Опт", "Бизнес", "Крипта"};
    private static final int[] TERMS = {5, 10, 20};
    private static final int[] NOTES = {10, 100, 500, 1000, 5000};
    private static final int PAGE = 6;

    private static int tab;
    private static int loanAmount = 1000;
    private static int termIdx = 1;
    private static int page;
    private static int bizType;

    private int left, top;
    private int ticks;

    public PhoneScreen() {
        super(new StringTextComponent("Телефон"));
    }

    private static CompoundNBT d() {
        return ClientAccountData.get();
    }

    private void send(int action, int a, int b) {
        Network.CHANNEL.sendToServer(new ActionPacket(action, a, b));
    }

    private Button btn(int x, int y, int w, String text, Button.IPressable onPress) {
        return this.addButton(new Button(left + x, top + y, w, 16, new StringTextComponent(text), onPress));
    }

    private void rebuild() {
        this.init(this.minecraft, this.width, this.height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        left = (this.width - W) / 2;
        top = (this.height - H) / 2;
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            btn(6 + i * 40, 18, 38, (tab == i ? "§e" : "") + TABS[i], b -> { tab = t; page = 0; rebuild(); });
        }
        switch (tab) {
            case 0:
                btn(8, 188, 110, "Обновить", b -> send(Actions.PHONE_SYNC, 0, 0));
                break;
            case 1:
                for (int i = 0; i < NOTES.length; i++) {
                    final int n = NOTES[i];
                    btn(8, 64 + i * 20, 70, "Снять " + n + "₽", b -> send(Actions.PHONE_WITHDRAW, n, 1));
                    btn(82, 64 + i * 20, 70, "10 × " + n + "₽", b -> send(Actions.PHONE_WITHDRAW, n, 10));
                }
                break;
            case 2: {
                int[] steps = {-10000, -1000, -100, 100, 1000, 10000};
                for (int i = 0; i < steps.length; i++) {
                    final int st = steps[i];
                    btn(8 + i * 39, 82, 37, (st > 0 ? "+" : "") + st, b -> loanAmount = Math.max(100, Math.min(10000000, loanAmount + st)));
                }
                for (int i = 0; i < TERMS.length; i++) {
                    final int t = i;
                    btn(8 + i * 52, 102, 50, (termIdx == i ? "§e" : "") + TERMS[i] + " дн.", b -> { termIdx = t; rebuild(); });
                }
                btn(166, 102, 74, "Макс.", b -> loanAmount = Math.max(100, (int) (d().getDouble("maxLoan") / 100) * 100));
                btn(8, 124, 150, "§aОформить кредит", b -> send(Actions.PHONE_LOAN, loanAmount, termIdx));
                btn(8, 170, 110, "Внести 1000₽", b -> send(Actions.PHONE_REPAY, 1000, 0));
                btn(122, 170, 118, "Погасить всё", b -> send(Actions.PHONE_REPAY, 0, 0));
                break;
            }
            case 3: {
                List<Wholesale.Lot> lots = Wholesale.lots();
                int pages = Math.max(1, (lots.size() + PAGE - 1) / PAGE);
                page = Math.min(page, pages - 1);
                for (int i = 0; i < PAGE; i++) {
                    final int idx = page * PAGE + i;
                    if (idx >= lots.size()) break;
                    btn(190, 52 + i * 20, 50, "Купить", b -> send(Actions.PHONE_WHOLESALE, idx, 0));
                }
                btn(8, 188, 40, "<", b -> { page = Math.max(0, page - 1); rebuild(); });
                btn(52, 188, 40, ">", b -> { page = Math.min(pages - 1, page + 1); rebuild(); });
                break;
            }
            case 4: {
                int n = BizType.values().length;
                btn(8, 132, 20, "<", b -> bizType = (bizType + n - 1) % n);
                btn(220, 132, 20, ">", b -> bizType = (bizType + 1) % n);
                btn(8, 188, 232, "§aКупить разрешение на стройку", b -> send(Actions.PHONE_BUY_BIZ, bizType, 0));
                break;
            }
            default:
                btn(8, 130, 74, "Продать 25%", b -> send(Actions.PHONE_SELL_BTC, 25, 0));
                btn(86, 130, 74, "Продать 50%", b -> send(Actions.PHONE_SELL_BTC, 50, 0));
                btn(164, 130, 76, "§cПродать всё", b -> send(Actions.PHONE_SELL_BTC, 100, 0));
                break;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks % 40 == 0) send(Actions.PHONE_SYNC, 0, 0);
    }

    // ------------------------------------------------------------------ отрисовка

    private void text(MatrixStack ms, String s, int x, int y) {
        this.font.drawString(ms, s, left + x, top + y, 0xFFFFFF);
    }

    private void small(MatrixStack ms, String s, int x, int y) {
        ms.push();
        ms.translate(left + x, top + y, 0);
        ms.scale(0.75F, 0.75F, 1.0F);
        this.font.drawString(ms, s, 0, 0, 0xFFFFFF);
        ms.pop();
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);
        // корпус телефона
        fill(ms, left - 4, top - 4, left + W + 4, top + H + 4, 0xFF111111);
        fill(ms, left - 2, top - 2, left + W + 2, top + H + 2, 0xFF2B2B2B);
        fill(ms, left, top, left + W, top + H, 0xFF0E1A2B);
        fill(ms, left, top, left + W, top + 14, 0xFF1F3A5F);
        CompoundNBT t = d();
        long time = this.minecraft != null && this.minecraft.world != null ? this.minecraft.world.getDayTime() % 24000L : 0;
        int hh = (int) ((time / 1000 + 6) % 24), mm = (int) (time % 1000 * 60 / 1000);
        small(ms, String.format(Locale.ROOT, "§f%02d:%02d   §6Реал Банк §7онлайн", hh, mm), 6, 4);
        small(ms, "§a▮▮▮▮ §f100%", W - 40, 4);
        fill(ms, left + 4, top + 38, left + W - 4, top + 39, 0xFF335577);
        switch (tab) {
            case 0: drawAccount(ms, t); break;
            case 1: drawWithdraw(ms, t); break;
            case 2: drawLoan(ms, t); break;
            case 3: drawWholesale(ms, t); break;
            case 4: drawBusiness(ms, t); break;
            default: drawCrypto(ms, t); break;
        }
        super.render(ms, mouseX, mouseY, partialTicks);
    }

    private void drawAccount(MatrixStack ms, CompoundNBT t) {
        text(ms, "§6§lСчёт", 8, 44);
        text(ms, "Баланс: §a" + Finance.money(t.getDouble("balance")), 8, 58);
        text(ms, "Наличные в карманах: §e" + Finance.money(t.getLong("cash")), 8, 70);
        text(ms, "Рейтинг: §e" + t.getInt("score") + " §7(" + t.getString("category") + ")", 8, 82);
        int y = 98;
        if (t.getDouble("loanPrincipal") > 0) {
            small(ms, "Кредит: §c" + Finance.money(t.getDouble("loanPrincipal")) + "§f, платёж " + Finance.money(t.getDouble("loanPayment"))
                    + "/день, осталось " + t.getInt("loanDays") + " дн.", 8, y);
            y += 10;
        }
        if (t.getDouble("mortPrincipal") > 0) {
            small(ms, "Ипотека: §c" + Finance.money(t.getDouble("mortPrincipal")) + "§f, платёж " + Finance.money(t.getDouble("mortPayment")) + "/день", 8, y);
            y += 10;
        }
        if (t.getDouble("arrears") > 0.01) {
            small(ms, "§cПросрочка: " + Finance.money(t.getDouble("arrears")) + " (" + t.getInt("overdue") + " дн.)", 8, y);
            y += 10;
        }
        if (t.getDouble("mfoDebt") > 0.01) {
            small(ms, "§2МФО «Шайкин и Ко»: §c" + Finance.money(t.getDouble("mfoDebt")), 8, y);
            y += 10;
        }
        double btc = t.getDouble("crypto");
        if (btc > 0) {
            small(ms, "§eКриптокошелёк: " + String.format(Locale.ROOT, "%.6f ₿", btc) + " ≈ " + Finance.money(btc * t.getDouble("btcRate")), 8, y);
            y += 10;
        }
        ListNBT biz = t.getList("bizList", 10);
        if (!biz.isEmpty()) small(ms, "§aЗаведений: " + biz.size(), 8, y);
        small(ms, "§8Вклад: +" + Finance.percent(t.getDouble("depositRate")) + " в день на остаток.", 8, 176);
    }

    private void drawWithdraw(MatrixStack ms, CompoundNBT t) {
        text(ms, "§6§lСнять наличные", 8, 44);
        small(ms, "На счёте: §a" + Finance.money(t.getDouble("balance")) + "§f. Купюры придут в инвентарь.", 8, 54);
        small(ms, "§7Банкомат в кармане: без комиссии.", 160, 70);
        small(ms, "§7Грабители любят наличку —", 160, 84);
        small(ms, "§7не носите всё с собой.", 160, 94);
    }

    private void drawLoan(MatrixStack ms, CompoundNBT t) {
        text(ms, "§6§lКредит онлайн", 8, 44);
        double max = t.getDouble("maxLoan");
        double rate = t.getDouble("loanRate");
        small(ms, "Одобрено до: §a" + Finance.money(max) + "§f, ставка " + Finance.percent(rate) + "/день (рейтинг " + t.getInt("score") + ")", 8, 54);
        double pay = Finance.annuity(loanAmount, rate, TERMS[termIdx]);
        text(ms, "Сумма: §e" + Finance.money(loanAmount) + "§f на " + TERMS[termIdx] + " дн.", 8, 66);
        small(ms, "Платёж ≈ §c" + Finance.money(pay) + "§f/день, переплата " + Finance.money(pay * TERMS[termIdx] - loanAmount), 132, 128);
        if (t.getDouble("loanPrincipal") > 0) {
            small(ms, "Текущий кредит: §c" + Finance.money(t.getDouble("loanPrincipal")) + "§f, платёж " + Finance.money(t.getDouble("loanPayment"))
                    + "/день, " + t.getInt("loanDays") + " дн.", 8, 150);
        } else {
            small(ms, "§7Кредита нет. Платёж списывается со счёта каждое утро.", 8, 150);
        }
        small(ms, "§8Не платите — придут Жора, Александр, потом полиция.", 8, 192);
    }

    private void drawWholesale(MatrixStack ms, CompoundNBT t) {
        text(ms, "§3§lОпт для магазинов", 8, 44);
        if (!t.getBoolean("ownsBiz")) {
            small(ms, "§cТолько для владельцев заведений. Купите заведение во вкладке «Бизнес».", 8, 54);
        } else {
            small(ms, "§7Дешевле розницы. Доставка сразу в инвентарь. Оплата: счёт, потом наличные.", 8, 54);
        }
        List<Wholesale.Lot> lots = Wholesale.lots();
        for (int i = 0; i < PAGE; i++) {
            int idx = page * PAGE + i;
            if (idx >= lots.size()) break;
            Wholesale.Lot l = lots.get(idx);
            int y = 52 + i * 20;
            small(ms, "§f" + l.count + "× " + l.name(), 8, y + 1);
            small(ms, "§a" + Finance.money(l.price) + (l.retail > 0 ? "  §8§m" + Finance.money(l.retail) : "  §8только опт"), 8, y + 9);
        }
        int pages = Math.max(1, (lots.size() + PAGE - 1) / PAGE);
        small(ms, "§7стр. " + (page + 1) + "/" + pages, 100, 193);
    }

    private void drawBusiness(MatrixStack ms, CompoundNBT t) {
        text(ms, "§a§lМой бизнес", 8, 44);
        ListNBT biz = t.getList("bizList", 10);
        long day = t.getLong("today");
        if (biz.isEmpty()) {
            small(ms, "§7Заведений пока нет. Купите разрешение — бригада построит.", 8, 56);
        }
        for (int i = 0; i < biz.size() && i < 6; i++) {
            CompoundNBT b = biz.getCompound(i);
            BizType bt = BizType.byId(b.getInt("type"));
            String name = bt == null ? "?" : bt.title;
            double lp = b.getDouble("lastProfit");
            small(ms, (b.getBoolean("worker") ? "§a● " : "§c● ") + "§f" + name + " §7вчера: " + (lp >= 0 ? "§a+" : "§c") + Finance.money(lp)
                    + " §7отмыто: " + Finance.money(b.getDouble("laundered")) + "/" + Finance.money(b.getDouble("safe")), 8, 54 + i * 10);
        }
        fill(ms, left + 4, top + 118, left + W - 4, top + 119, 0xFF335577);
        BizType bt = BizType.values()[bizType % BizType.values().length];
        text(ms, "§e" + bt.title, 32, 136);
        small(ms, "Разрешение: §e" + Finance.money(bt.price) + "§f   базовая прибыль: §a" + Finance.money(bt.base) + "§f/день", 8, 156);
        small(ms, "Работник: " + bt.workerName + ", найм " + Finance.money(bt.hireCost()) + ", зарплата " + Finance.money(bt.wage()) + "/день", 8, 166);
        small(ms, "§7Через кассу можно отмыть без риска до " + Finance.money(bt.base * t.getDouble("safeShare")) + "/день.", 8, 176);
    }

    private void drawCrypto(MatrixStack ms, CompoundNBT t) {
        text(ms, "§e§lКриптобиржа", 8, 44);
        double rate = t.getDouble("btcRate");
        double prev = t.getDouble("btcBase");
        double btc = t.getDouble("crypto");
        text(ms, "Курс BTC: §e" + Finance.money(rate), 8, 60);
        if (prev > 0) small(ms, (rate >= prev ? "§a▲ " : "§c▼ ") + String.format(Locale.ROOT, "%+.1f%%", (rate / prev - 1) * 100) + " §7к среднему", 8, 72);
        text(ms, "Кошелёк: §e" + String.format(Locale.ROOT, "%.6f ₿", btc), 8, 88);
        text(ms, "≈ §a" + Finance.money(btc * rate), 8, 100);
        small(ms, "§7Комиссия биржи 1%. Деньги — на банковский счёт.", 8, 152);
        small(ms, "§7Майнить: майнинг-ферма + комплектующие (материнка, процессор,", 8, 164);
        small(ms, "§7память, блок питания, кулер и 1–4 видеокарты). Курс меняется раз в минуту.", 8, 174);
    }
}
