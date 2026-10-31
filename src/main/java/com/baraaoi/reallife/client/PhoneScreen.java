package com.baraaoi.reallife.client;

import com.baraaoi.reallife.economy.Actions;
import com.baraaoi.reallife.economy.BizEquip;
import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.Crypto;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.economy.Wholesale;
import com.baraaoi.reallife.network.ActionPacket;
import com.baraaoi.reallife.network.Network;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

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
    private static int bizSub;
    private static int coin;
    private static final String[] SHORT = {"шаурма", "маркет", "мойка", "карьер", "ломбард", "АЗС", "техника"};
    private static final int EQ_PAGE = 6;

    private int left, top;
    private int ticks;

    public PhoneScreen() {
        super(Component.literal("Телефон"));
    }

    private static CompoundTag d() {
        return ClientAccountData.get();
    }

    private void send(int action, int a, int b) {
        Network.toServer(new ActionPacket(action, a, b));
    }

    private Button btn(int x, int y, int w, String text, Button.OnPress onPress) {
        return this.addRenderableWidget(Button.builder(Component.literal(text), onPress).bounds(left + x, top + y, w, 16).build());
    }

    private void rebuild() {
        this.rebuildWidgets();
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
                btn(166, 102, 74, "Макс.", b -> loanAmount = Math.max(100, (int) (d().getDoubleOr("maxLoan", 0.0) / 100) * 100));
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
                btn(8, 42, 76, (bizSub == 0 ? "§e" : "") + "Заведения", b -> { bizSub = 0; page = 0; rebuild(); });
                btn(86, 42, 76, (bizSub == 1 ? "§e" : "") + "Оборудование", b -> { bizSub = 1; page = 0; rebuild(); });
                btn(164, 42, 76, (bizSub == 2 ? "§e" : "") + "ТЦ", b -> { bizSub = 2; page = 0; rebuild(); });
                if (bizSub == 0) {
                    int n = BizType.values().length;
                    btn(8, 126, 20, "<", b -> bizType = (bizType + n - 1) % n);
                    btn(220, 126, 20, ">", b -> bizType = (bizType + 1) % n);
                    btn(8, 188, 112, "§aЛицензия", b -> send(Actions.PHONE_BUY_BIZ, bizType, 0));
                    btn(124, 188, 116, "§6Под ключ", b -> send(Actions.PHONE_BUY_TURNKEY, bizType, 0));
                } else if (bizSub == 2) {
                    btn(8, 188, 232, "§6Купить разрешение на ТЦ", b -> send(Actions.PHONE_BUY_MALL, 0, 0));
                } else {
                    BizEquip[] eq = BizEquip.values();
                    int pages = Math.max(1, (eq.length + EQ_PAGE - 1) / EQ_PAGE);
                    page = Math.min(page, pages - 1);
                    for (int i = 0; i < EQ_PAGE; i++) {
                        final int idx = page * EQ_PAGE + i;
                        if (idx >= eq.length) break;
                        btn(160, 64 + i * 20, 38, "×1", b -> send(Actions.PHONE_BUY_EQUIP, idx, 1));
                        btn(202, 64 + i * 20, 38, "×4", b -> send(Actions.PHONE_BUY_EQUIP, idx, 4));
                    }
                    btn(8, 188, 40, "<", b -> { page = Math.max(0, page - 1); rebuild(); });
                    btn(52, 188, 40, ">", b -> { page = Math.min(pages - 1, page + 1); rebuild(); });
                }
                break;
            }
            default: {
                Crypto.Coin[] cs = Crypto.Coin.values();
                coin = Math.max(0, Math.min(cs.length - 1, coin));
                for (int i = 0; i < cs.length; i++) {
                    final int ci = i;
                    btn(8 + i * 58, 38, 56, (coin == i ? "§e" : "") + cs[i].ticker, b -> { coin = ci; rebuild(); });
                }
                int[] buys = {1000, 10000, 100000};
                String[] bl = {"+1 000₽", "+10 000₽", "+100 000₽"};
                for (int i = 0; i < buys.length; i++) {
                    final int r = buys[i];
                    btn(8 + i * 58, 118, 56, "§a" + bl[i], b -> send(Actions.PHONE_BUY_CRYPTO, coin, r));
                }
                btn(182, 118, 58, "§aНа всё", b -> send(Actions.PHONE_BUY_CRYPTO, coin, 0));
                btn(8, 138, 74, "Продать 25%", b -> send(Actions.PHONE_SELL_BTC, 25, coin));
                btn(86, 138, 74, "Продать 50%", b -> send(Actions.PHONE_SELL_BTC, 50, coin));
                btn(164, 138, 76, "§cПродать всё", b -> send(Actions.PHONE_SELL_BTC, 100, coin));
                break;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks % 40 == 0) send(Actions.PHONE_SYNC, 0, 0);
    }

    // ------------------------------------------------------------------ отрисовка

    private void text(GuiGraphicsExtractor ms, String s, int x, int y) {
        GuiUtil.text(ms, this.font, s, left + x, top + y, 0xFFFFFF);
    }

    private void small(GuiGraphicsExtractor ms, String s, int x, int y) {
        ms.pose().pushMatrix();
        ms.pose().translate((float) (left + x), (float) (top + y));
        ms.pose().scale(0.75F, 0.75F);
        GuiUtil.text(ms, this.font, s, 0, 0, 0xFFFFFF);
        ms.pose().popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
        // корпус телефона
        ms.fill(left - 4, top - 4, left + W + 4, top + H + 4, 0xFF111111);
        ms.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF2B2B2B);
        ms.fill(left, top, left + W, top + H, 0xFF0E1A2B);
        ms.fill(left, top, left + W, top + 14, 0xFF1F3A5F);
        CompoundTag t = d();
        long time = this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getOverworldClockTime() % 24000L : 0;
        int hh = (int) ((time / 1000 + 6) % 24), mm = (int) (time % 1000 * 60 / 1000);
        small(ms, String.format(Locale.ROOT, "§f%02d:%02d   §6Реал Банк §7онлайн", hh, mm), 6, 4);
        small(ms, "§8v" + com.baraaoi.reallife.RealLife.version() + "  §a▮▮▮▮ §f100%", W - 72, 4);
        ms.fill(left + 4, top + 38, left + W - 4, top + 39, 0xFF335577);
        switch (tab) {
            case 0: drawAccount(ms, t); break;
            case 1: drawWithdraw(ms, t); break;
            case 2: drawLoan(ms, t); break;
            case 3: drawWholesale(ms, t); break;
            case 4: drawBusiness(ms, t); break;
            default: drawCrypto(ms, t); break;
        }
        super.extractRenderState(ms, mouseX, mouseY, partialTicks);
    }

    private void drawAccount(GuiGraphicsExtractor ms, CompoundTag t) {
        text(ms, "§6§lСчёт", 8, 44);
        text(ms, "Баланс: §a" + Finance.money(t.getDoubleOr("balance", 0.0)), 8, 58);
        text(ms, "Наличные в карманах: §e" + Finance.money(t.getLongOr("cash", 0L)), 8, 70);
        text(ms, "Рейтинг: §e" + t.getIntOr("score", 0) + " §7(" + t.getStringOr("category", "") + ")", 8, 82);
        int y = 98;
        if (t.getDoubleOr("loanPrincipal", 0.0) > 0) {
            small(ms, "Кредит: §c" + Finance.money(t.getDoubleOr("loanPrincipal", 0.0)) + "§f, платёж " + Finance.money(t.getDoubleOr("loanPayment", 0.0))
                    + "/день, осталось " + t.getIntOr("loanDays", 0) + " дн.", 8, y);
            y += 10;
        }
        if (t.getDoubleOr("mortPrincipal", 0.0) > 0) {
            small(ms, "Ипотека: §c" + Finance.money(t.getDoubleOr("mortPrincipal", 0.0)) + "§f, платёж " + Finance.money(t.getDoubleOr("mortPayment", 0.0)) + "/день", 8, y);
            y += 10;
        }
        if (t.getDoubleOr("arrears", 0.0) > 0.01) {
            small(ms, "§cПросрочка: " + Finance.money(t.getDoubleOr("arrears", 0.0)) + " (" + t.getIntOr("overdue", 0) + " дн.)", 8, y);
            y += 10;
        }
        if (t.getDoubleOr("mfoDebt", 0.0) > 0.01) {
            small(ms, "§2МФО «Шайкин и Ко»: §c" + Finance.money(t.getDoubleOr("mfoDebt", 0.0)), 8, y);
            y += 10;
        }
        double cv = t.getDoubleOr("cryptoValue", 0.0);
        if (cv > 0.5) {
            small(ms, "§eКриптопортфель: ≈ " + Finance.money(cv), 8, y);
            y += 10;
        }
        ListTag biz = t.getListOrEmpty("bizList");
        if (!biz.isEmpty()) small(ms, "§aЗаведений: " + biz.size(), 8, y);
        small(ms, "§8Вклад: +" + Finance.percent(t.getDoubleOr("depositRate", 0.0)) + " в день на остаток.", 8, 176);
    }

    private void drawWithdraw(GuiGraphicsExtractor ms, CompoundTag t) {
        text(ms, "§6§lСнять наличные", 8, 44);
        small(ms, "На счёте: §a" + Finance.money(t.getDoubleOr("balance", 0.0)) + "§f. Купюры придут в инвентарь.", 8, 54);
        small(ms, "§7Банкомат в кармане: без комиссии.", 160, 70);
        small(ms, "§7Грабители любят наличку —", 160, 84);
        small(ms, "§7не носите всё с собой.", 160, 94);
    }

    private void drawLoan(GuiGraphicsExtractor ms, CompoundTag t) {
        text(ms, "§6§lКредит онлайн", 8, 44);
        double max = t.getDoubleOr("maxLoan", 0.0);
        double rate = t.getDoubleOr("loanRate", 0.0);
        small(ms, "Одобрено до: §a" + Finance.money(max) + "§f, ставка " + Finance.percent(rate) + "/день (рейтинг " + t.getIntOr("score", 0) + ")", 8, 54);
        double pay = Finance.annuity(loanAmount, rate, TERMS[termIdx]);
        text(ms, "Сумма: §e" + Finance.money(loanAmount) + "§f на " + TERMS[termIdx] + " дн.", 8, 66);
        small(ms, "Платёж ≈ §c" + Finance.money(pay) + "§f/день, переплата " + Finance.money(pay * TERMS[termIdx] - loanAmount), 132, 128);
        if (t.getDoubleOr("loanPrincipal", 0.0) > 0) {
            small(ms, "Текущий кредит: §c" + Finance.money(t.getDoubleOr("loanPrincipal", 0.0)) + "§f, платёж " + Finance.money(t.getDoubleOr("loanPayment", 0.0))
                    + "/день, " + t.getIntOr("loanDays", 0) + " дн.", 8, 150);
        } else {
            small(ms, "§7Кредита нет. Платёж списывается со счёта каждое утро.", 8, 150);
        }
        small(ms, "§8Не платите — придут Жора, Александр, потом полиция.", 8, 192);
    }

    private void drawWholesale(GuiGraphicsExtractor ms, CompoundTag t) {
        text(ms, "§3§lОпт для магазинов", 8, 44);
        if (!t.getBooleanOr("ownsBiz", false)) {
            small(ms, "§cТолько для владельцев заведений. Купите заведение во вкладке «Бизнес».", 8, 54);
        } else {
            small(ms, "§7Сырьё едет прямо на полки заведения, остальное — в инвентарь.", 8, 54);
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

    private void drawBusiness(GuiGraphicsExtractor ms, CompoundTag t) {
        if (bizSub == 1) { drawEquip(ms, t); return; }
        if (bizSub == 2) { drawMall(ms, t); return; }
        ListTag biz = t.getListOrEmpty("bizList");
        if (biz.isEmpty()) {
            small(ms, "§7Заведений пока нет. Купите лицензию, поставьте кассу на своей земле", 8, 64);
            small(ms, "§7и расставьте вокруг неё оборудование (вкладка «Оборудование»).", 8, 73);
        }
        for (int i = 0; i < biz.size() && i < 5; i++) {
            CompoundTag b = biz.getCompoundOrEmpty(i);
            BizType bt = BizType.byId(b.getIntOr("type", 0));
            String name = bt == null ? "?" : bt.title;
            double lp = b.getDoubleOr("lastProfit", 0.0);
            String reason = b.getStringOr("reason", "");
            small(ms, (reason.isEmpty() ? "§a● " : "§c● ") + "§f" + name + " §7раб. " + b.getIntOr("workers", 0) + "/" + b.getIntOr("allowed", 0)
                    + " сырьё " + b.getIntOr("stock", 0) + " вчера " + (lp >= 0 ? "§a+" : "§c") + Finance.money(lp)
                    + (reason.isEmpty() ? "" : " §c(" + reason + ")"), 8, 64 + i * 10);
        }
        ms.fill(left + 4, top + 116, left + W - 4, top + 117, 0xFF335577);
        BizType bt = BizType.values()[bizType % BizType.values().length];
        text(ms, "§e" + bt.title, 32, 130);
        small(ms, "Лицензия: §e" + Finance.money(bt.price) + "§f  выручка §a" + Finance.money(bt.revenue()) + "§f/день на работника, до §e"
                + bt.maxWorkers + "§f раб.", 8, 142);
        small(ms, "Комплект на 1 работника (≈" + Finance.money(bt.kitPrice()) + "):", 8, 150);
        small(ms, "§e" + com.baraaoi.reallife.economy.LegalBiz.kitText(bt) + " §f+ склад: §e" + com.baraaoi.reallife.economy.LegalBiz.baseText(bt), 8, 158);
        small(ms, "Сырьё: " + bt.supplyName + " §a" + Finance.money(bt.supplyPrice()) + "§f/шт, " + BizType.SUPPLY_PER_WORKER
                + " кор./день на раб.; зарплата " + Finance.money(bt.wage()), 8, 166);
        small(ms, "§6Под ключ: §e" + Finance.money(bt.turnkeyPrice()) + "§7 — лицензия + здание + всё на " + bt.maxWorkers + " раб.", 8, 175);
    }

    private void drawMall(GuiGraphicsExtractor ms, CompoundTag t) {
        text(ms, "§6Торговый центр", 8, 62);
        small(ms, "Разрешение со стройкой: §e" + Finance.money(com.baraaoi.reallife.economy.Mall.PRICE), 8, 74);
        small(ms, "§7Атриум с фонтаном и 12 помещений от киоска до большого зала.", 8, 84);
        small(ms, "§7Помещения занимаете сами или продаёте игрокам: цена + §aаренда/день§7 вам.", 8, 93);
        small(ms, "§7Внутри — что угодно: жильё или любое заведение. §aПрибыль в ТЦ +50%§7,", 8, 102);
        small(ms, "§7отмыв §a25%§7. Свободные места занимают боты — платят, но конкурируют.", 8, 111);
        ListTag malls = t.getListOrEmpty("malls");
        ms.fill(left + 4, top + 122, left + W - 4, top + 123, 0xFF335577);
        if (malls.isEmpty()) {
            small(ms, "§7Своих ТЦ и помещений нет. Для ТЦ нужна ровная площадка ~73×62,", 8, 128);
            small(ms, "§7вся земля — ваша или ничья. Помещение в чужом ТЦ: ПКМ по табличке.", 8, 137);
        }
        int y = 127;
        for (int i = 0; i < malls.size() && y < 186; i++) {
            CompoundTag m = malls.getCompoundOrEmpty(i);
            if (m.getBooleanOr("own", false)) {
                small(ms, "§fТЦ №" + m.getIntOr("id", 0) + " §7(" + m.getIntOr("x", 0) + ", " + m.getIntOr("y", 0) + ", " + m.getIntOr("z", 0) + ") §fзанято §e"
                        + m.getIntOr("used", 0) + "/" + m.getIntOr("total", 0) + "§f, свободно " + m.getIntOr("free", 0), 8, y);
                small(ms, "§7аренда в день: §a" + Finance.money(m.getIntOr("rent", 0)) + (m.getIntOr("bots", 0) > 0 ? " §c(ботов: " + m.getIntOr("bots", 0) + ")" : ""), 14, y + 9);
                y += 19;
            } else {
                String biz = m.getStringOr("biz", "");
                small(ms, "§fПомещение №" + m.getIntOr("unit", 0) + " §7в ТЦ «" + m.getStringOr("mall", "") + "» " + m.getStringOr("label", "")
                        + (biz.isEmpty() ? "" : " §e" + biz), 8, y);
                small(ms, "§7аренда §c" + Finance.money(m.getIntOr("rent", 0)) + "/день" + (m.getIntOr("unpaid", 0) > 0 ? " §c§lдолг " + m.getIntOr("unpaid", 0) + " дн.!" : ""), 14, y + 9);
                y += 19;
            }
        }
    }

    private void drawEquip(GuiGraphicsExtractor ms, CompoundTag t) {
        BizEquip[] eq = BizEquip.values();
        for (int i = 0; i < EQ_PAGE; i++) {
            int idx = page * EQ_PAGE + i;
            if (idx >= eq.length) break;
            BizEquip e = eq[idx];
            int y = 64 + i * 20;
            small(ms, "§f" + e.title + "  §a" + Finance.money(e.price), 8, y + 1);
            StringBuilder sb = new StringBuilder();
            if (e == BizEquip.RACK) {
                small(ms, "§8склад: 1 на любое заведение, больше — запас сырья", 8, y + 9);
                continue;
            }
            for (BizType bt : BizType.values()) {
                int n = bt.need(e);
                if (n <= 0) continue;
                if (sb.length() > 0) sb.append(", ");
                sb.append(SHORT[bt.ordinal()]).append(n > 1 ? "×" + n : "");
            }
            small(ms, "§8для: " + sb, 8, y + 9);
        }
        int pages = Math.max(1, (eq.length + EQ_PAGE - 1) / EQ_PAGE);
        small(ms, "§7стр. " + (page + 1) + "/" + pages + "  §8ставить в зоне ±8 от главной кассы", 100, 193);
    }

    private void drawCrypto(GuiGraphicsExtractor ms, CompoundTag t) {
        ListTag coins = t.getListOrEmpty("coins");
        Crypto.Coin c = Crypto.Coin.byId(coin);
        if (c == null || coin >= coins.size()) {
            text(ms, "§7Биржа загружается...", 8, 62);
            return;
        }
        CompoundTag ct = coins.getCompoundOrEmpty(coin);
        double rate = ct.getDoubleOr("rate", 0.0), base = ct.getDoubleOr("base", 0.0), held = ct.getDoubleOr("held", 0.0);
        ListTag hist = ct.getListOrEmpty("hist");
        double prev = hist.size() >= 2 ? hist.getDoubleOr(hist.size() - 2, 0.0) : rate;
        text(ms, "§e" + c.title + ": §f" + Crypto.price(rate), 8, 60);
        small(ms, (rate >= prev ? "§a▲ " : "§c▼ ") + String.format(Locale.ROOT, "%+.2f%%", prev > 0 ? (rate / prev - 1) * 100 : 0) + " §7за 20 с", 8, 72);
        if (base > 0) small(ms, "§7к среднему " + (rate >= base ? "§a" : "§c") + String.format(Locale.ROOT, "%+.1f%%", (rate / base - 1) * 100), 8, 80);
        small(ms, "Кошелёк: §e" + Crypto.fmt(c, held), 8, 92);
        small(ms, "≈ §a" + Finance.money(held * rate), 8, 100);
        // график последних ~13 минут
        int gx = 118, gy = 58, gw = 122, gh = 54;
        ms.fill(left + gx - 1, top + gy - 1, left + gx + gw + 1, top + gy + gh + 1, 0xFF33475F);
        ms.fill(left + gx, top + gy, left + gx + gw, top + gy + gh, 0xFF0A1220);
        int n = hist.size();
        if (n >= 2) {
            double mn = Double.MAX_VALUE, mx = -Double.MAX_VALUE;
            for (int i = 0; i < n; i++) { mn = Math.min(mn, hist.getDoubleOr(i, 0.0)); mx = Math.max(mx, hist.getDoubleOr(i, 0.0)); }
            if (mx - mn < mx * 1e-4) { mx += mx * 0.01; mn -= mn * 0.01; }
            int px = -1, py = -1;
            for (int i = 0; i < n; i++) {
                int x = gx + 1 + (int) Math.round(i * (gw - 3) / (double) (n - 1));
                int y = gy + gh - 2 - (int) Math.round((hist.getDoubleOr(i, 0.0) - mn) / (mx - mn) * (gh - 4));
                if (px >= 0) {
                    int col = hist.getDoubleOr(i, 0.0) < hist.getDoubleOr(i - 1, 0.0) ? 0xFFE05050 : 0xFF50E070;
                    int last = py;
                    for (int xx = px + 1; xx <= x; xx++) {
                        int yy = py + (y - py) * (xx - px) / Math.max(1, x - px);
                        ms.fill(left + xx, top + Math.min(yy, last), left + xx + 1, top + Math.max(yy, last) + 1, col);
                        last = yy;
                    }
                }
                px = x;
                py = y;
            }
            small(ms, "§7" + Crypto.price(mx), gx + 2, gy + 2);
            small(ms, "§7" + Crypto.price(mn), gx + 2, gy + gh - 8);
        } else {
            small(ms, "§7график копится...", gx + 4, gy + gh / 2 - 3);
        }
        small(ms, "§7Купить (со счёта):", 8, 110);
        double total = t.getDoubleOr("cryptoValue", 0.0);
        small(ms, "Весь портфель: §a" + Finance.money(total) + "§f  на счёте §e" + Finance.money(t.getDoubleOr("balance", 0.0)), 8, 160);
        small(ms, "§7Комиссия биржи 1% на покупку и продажу. Курс меняется каждые 20 с,", 8, 170);
        small(ms, "§7бывают новости со скачками ±15–40%. BTC ещё и майнится на ферме", 8, 179);
        small(ms, "§7(материнка, процессор, память, БП, кулер, 1–4 видеокарты).", 8, 188);
    }
}
