package com.baraaoi.reallife.client;

import com.baraaoi.reallife.container.CasinoContainer;
import com.baraaoi.reallife.economy.Actions;
import com.baraaoi.reallife.economy.CasinoService;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.network.ActionPacket;
import com.baraaoi.reallife.network.Network;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Random;

/**
 * Экран игры в казино: ставка, кнопка «Крутить» (или «Орёл»/«Решка»), анимация и результат.
 */
public class CasinoScreen extends AbstractContainerScreen<CasinoContainer> {
    private static final int SPIN_TICKS = 64;
    /** Через сколько тиков до конца анимации останавливается каждый барабан. */
    private static final int[] REEL_STOP = {34, 17, 0};
    private static int bet = 100;

    private final int kind;
    private final Random fx = new Random();
    private long seenId = Long.MIN_VALUE;
    private int spin;
    private int ticks;
    /** Символы на крутящихся барабанах / подсветка анархии — меняются по тикам, а не каждый кадр. */
    private final int[] reelSym = {0, 1, 2};
    private int anarchyIdx;

    public CasinoScreen(CasinoContainer container, Inventory inv, Component title) {
        super(container, inv, title, 240, 176);
        this.kind = menu.getKind();
        CompoundTag last = ClientAccountData.get().getCompoundOrEmpty("casino");
        if (last.contains("id")) seenId = last.getLongOr("id", 0L);
    }

    private static CompoundTag d() {
        return ClientAccountData.get();
    }

    private Button btn(int x, int y, int w, String text, Button.OnPress onPress) {
        return this.addRenderableWidget(Button.builder(Component.literal(text), onPress).bounds(leftPos + x, topPos + y, w, 16).build());
    }

    private int maxBet() {
        int m = d().getIntOr("casinoMaxBet", 0);
        return m > 0 ? m : 1000;
    }

    private void play(int choice) {
        if (spin > 0) return;
        Network.toServer(new ActionPacket(Actions.CASINO_PLAY, bet, choice));
    }

    @Override
    protected void init() {
        super.init();
        int[] steps = {-1000, -100, -10, 10, 100, 1000};
        for (int i = 0; i < steps.length; i++) {
            final int st = steps[i];
            btn(8 + i * 38, 112, 36, (st > 0 ? "+" : "") + st, b -> bet = Math.max(10, Math.min(maxBet(), bet + st)));
        }
        if (kind == CasinoService.COIN) {
            btn(8, 134, 108, "§e§lОРЁЛ", b -> play(0));
            btn(124, 134, 108, "§f§lРЕШКА", b -> play(1));
        } else {
            btn(8, 134, 224, kind == CasinoService.ROULETTE ? "§6§lКРУТИТЬ РУЛЕТКУ" : kind == CasinoService.ANARCHY
                    ? "§4§lДА ЗДРАВСТВУЕТ АНАРХИЯ!" : "§c§lДЁРНУТЬ РЫЧАГ", b -> play(0));
        }
        btn(8, 154, 70, "Ставка: макс", b -> bet = maxBet());
        btn(82, 154, 70, "Ставка: 100", b -> bet = 100);
    }

    @Override
    protected void containerTick() {
        ticks++;
        CompoundTag last = d().getCompoundOrEmpty("casino");
        if (last.contains("id") && last.getLongOr("id", 0L) != seenId && last.getIntOr("kind", 0) == kind) {
            seenId = last.getLongOr("id", 0L);
            spin = SPIN_TICKS;
        }
        if (spin > 0) {
            spin--;
            for (int i = 0; i < 3; i++) {
                int left = spin - REEL_STOP[i];
                if (left <= 0) continue;
                int every = left > 14 ? 3 : left > 6 ? 5 : 7; // барабан замедляется перед остановкой
                if (ticks % every == 0) reelSym[i] = (reelSym[i] + 1 + fx.nextInt(CasinoService.SYMBOLS.length - 1)) % CasinoService.SYMBOLS.length;
            }
            int every = spin > 30 ? 4 : spin > 12 ? 6 : 9;
            if (ticks % every == 0) anarchyIdx = (anarchyIdx + 1 + fx.nextInt(CasinoService.ANARCHY_MULT.length - 1)) % CasinoService.ANARCHY_MULT.length;
        }
    }


    @Override
    public void extractBackground(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(ms, mouseX, mouseY, partialTicks);
        int x = leftPos, y = topPos;
        int border = kind == CasinoService.ANARCHY ? 0xFFB71C1C : 0xFFC9A227;
        ms.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, border);
        ms.fill(x, y, x + imageWidth, y + imageHeight, 0xFF140B1E);
        ms.fill(x + 4, y + 30, x + imageWidth - 4, y + 92, kind == CasinoService.ANARCHY ? 0xFF2A0A0A : 0xFF0D3B1E);
    }

    private void text(GuiGraphicsExtractor ms, String s, int x, int y, int color) {
        GuiUtil.text(ms, this.font, s, x, y, color);
    }

    private void center(GuiGraphicsExtractor ms, String s, int cx, int y, int color) {
        GuiUtil.text(ms, this.font, s, cx - this.font.width(s) / 2, y, color);
    }

    private void small(GuiGraphicsExtractor ms, String s, int x, int y, int color) {
        ms.pose().pushMatrix();
        ms.pose().translate((float) (x), (float) (y));
        ms.pose().scale(0.75F, 0.75F);
        GuiUtil.text(ms, this.font, s, 0, 0, color);
        ms.pose().popMatrix();
    }

    private void box(GuiGraphicsExtractor ms, int x, int y, int w, int h, int color) {
        ms.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFFC9A227);
        ms.fill(x, y, x + w, y + h, color);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ms, int mouseX, int mouseY) {
        CompoundTag t = d();
        CompoundTag last = t.getCompoundOrEmpty("casino");
        boolean mine = last.contains("id") && last.getIntOr("kind", 0) == kind;
        center(ms, "§6§l" + CasinoService.TITLES[Math.max(0, Math.min(3, kind))], imageWidth / 2, 6, 0xFFFFFF);
        small(ms, "Счёт: §e" + Finance.money(t.getDoubleOr("balance", 0.0)) + "§r   Наличные: §e" + Finance.money(t.getLongOr("cash", 0L))
                + "§r   Сегодня: " + (t.getDoubleOr("casinoProfit", 0.0) >= 0 ? "§a+" : "§c") + Finance.money(t.getDoubleOr("casinoProfit", 0.0)), 8, 19, 0xCCCCCC);
        int cx = imageWidth / 2;
        switch (kind) {
            case CasinoService.COIN: drawCoin(ms, last, mine, cx); break;
            case CasinoService.SLOTS: drawSlots(ms, last, mine, cx); break;
            case CasinoService.ANARCHY: drawAnarchy(ms, last, mine, cx); break;
            default: drawRoulette(ms, last, mine, cx); break;
        }
        // итог последней игры
        if (mine && spin == 0) {
            double mult = last.getDoubleOr("mult", 0.0);
            String res = mult > 1 ? "§a§lВЫИГРЫШ " + Finance.money(last.getDoubleOr("win", 0.0)) + " (x" + fmt(mult) + ")"
                    : mult == 1 ? "§eСтавка возвращена" : "§cПроигрыш " + Finance.money(last.getIntOr("bet", 0));
            center(ms, res + (last.getBooleanOr("clover", false) ? " §2(клевер)" : ""), cx, 94, 0xFFFFFF);
        } else if (spin > 0) {
            center(ms, "§7. . .", cx, 94, 0xFFFFFF);
        }
        String b = "Ставка: §e" + Finance.money(bet);
        text(ms, b, 8, 103, 0xFFFFFF);
        small(ms, "§8макс " + Finance.money(maxBet()), 170, 105, 0x777777);
        small(ms, "§8Везёт — играй дальше!", 158, 158, 0x777777);
    }

    private static String fmt(double m) {
        return m == Math.floor(m) ? String.valueOf((int) m) : String.valueOf(m);
    }

    // ------------------------------------------------------------------ игры

    private void drawCoin(GuiGraphicsExtractor ms, CompoundTag last, boolean mine, int cx) {
        int side;
        int elapsed = SPIN_TICKS - spin;
        if (spin > 0) side = ((int) (Math.sqrt(elapsed) * 1.7)) % 2; // монета вертится всё медленнее
        else side = mine ? last.getIntOr("side", 0) : -1;
        int color = side == 0 ? 0xFFD4AF37 : side == 1 ? 0xFFC0C0C0 : 0xFF555555;
        int r = spin > 0 ? 14 + (int) (8 * Math.abs(Math.cos(Math.sqrt(elapsed) * 1.7 * Math.PI))) : 22;
        box(ms, cx - r, 40, r * 2, 44, color);
        center(ms, side == 0 ? "§0§lОРЁЛ" : side == 1 ? "§0§lРЕШКА" : "§7?", cx, 58, 0xFFFFFF);
        small(ms, "§7Угадал — x2. Шанс 49%", 8, 84, 0xAAAAAA);
    }

    private void drawSlots(GuiGraphicsExtractor ms, CompoundTag last, boolean mine, int cx) {
        int[] reels = mine ? last.getIntArray("reels").orElse(new int[0]) : new int[0];
        for (int i = 0; i < 3; i++) {
            int bx = cx - 75 + i * 52;
            box(ms, bx, 38, 46, 40, 0xFF1A1A1A);
            int sym;
            boolean rolling = spin > REEL_STOP[i];
            if (rolling) sym = reelSym[i];
            else sym = reels.length == 3 ? reels[i] : -1;
            if (sym >= 0) {
                String s = CasinoService.SYMBOLS[sym];
                center(ms, (sym == 0 ? "§l" : "") + s, bx + 23, 54, CasinoService.SYMBOL_COLORS[sym]);
            } else {
                center(ms, "§8?", bx + 23, 54, 0xFFFFFF);
            }
        }
        small(ms, "§7777 — x25, три одинаковых — x8, два — ставка. Троек 7%!", 8, 84, 0xAAAAAA);
    }

    private void drawAnarchy(GuiGraphicsExtractor ms, CompoundTag last, boolean mine, int cx) {
        int n = CasinoService.ANARCHY_MULT.length;
        int idx = spin > 0 ? anarchyIdx : (mine ? last.getIntOr("sector", 0) : -1);
        for (int i = 0; i < n; i++) {
            int bx = cx - 108 + i * 36;
            boolean on = i == idx;
            box(ms, bx, 40, 32, 22, on ? 0xFFB71C1C : 0xFF301010);
            center(ms, (on ? "§f§l" : "§7") + "x" + fmt(CasinoService.ANARCHY_MULT[i]), bx + 16, 47, 0xFFFFFF);
        }
        if (spin > 0) {
            String[] chaos = {"Х А О С", "Б У Н Т", "А Н А Р Х И Я", "!!!"};
            center(ms, "§c§l" + chaos[(ticks / 8) % chaos.length], cx, 70, 0xFFFFFF);
        } else if (mine) {
            center(ms, "§c" + CasinoService.ANARCHY_TEXT[Math.max(0, Math.min(n - 1, idx))], cx, 70, 0xFFFFFF);
        } else {
            center(ms, "§7Хаос решает: x0 / x1 / x1.5 / x2 / x5 / x8", cx, 70, 0xFFFFFF);
        }
        small(ms, "§8Мама — анархия. Казино — папа.", 8, 84, 0x777777);
    }

    private void drawRoulette(GuiGraphicsExtractor ms, CompoundTag last, boolean mine, int cx) {
        int len = CasinoService.WHEEL.length;
        int sector = mine ? last.getIntOr("sector", 0) : 0;
        // колесо плавно тормозит: путь до остановки = spin² / 64 секторов (старт ~2 сектора за тик)
        int offset = spin > 0 ? sector - (spin * spin) / 64 : sector;
        for (int k = -3; k <= 3; k++) {
            int idx = ((offset + k) % len + len) % len;
            int m = CasinoService.WHEEL[idx];
            int bx = cx - 14 + k * 31;
            int col = m == 0 ? 0xFF222222 : m >= 10 ? 0xFFD4AF37 : m >= 3 ? 0xFF1565C0 : 0xFFB71C1C;
            box(ms, bx, 44, 28, 26, col);
            center(ms, (k == 0 ? "§f§l" : "§7") + "x" + m, bx + 14, 53, 0xFFFFFF);
        }
        center(ms, "§e§lV", cx, 33, 0xFFFFFF);
        small(ms, "§7100 секторов: x0 — 64, x1 — 10, x2 — 20, x3 — 3, x5, x10, x25 — по 1", 8, 84, 0xAAAAAA);
    }
}
