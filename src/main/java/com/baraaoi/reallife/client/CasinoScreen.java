package com.baraaoi.reallife.client;

import com.baraaoi.reallife.container.CasinoContainer;
import com.baraaoi.reallife.economy.Actions;
import com.baraaoi.reallife.economy.CasinoService;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.network.ActionPacket;
import com.baraaoi.reallife.network.Network;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.Random;

/**
 * Экран игры в казино: ставка, кнопка «Крутить» (или «Орёл»/«Решка»), анимация и результат.
 */
public class CasinoScreen extends ContainerScreen<CasinoContainer> {
    private static final int SPIN_TICKS = 28;
    private static int bet = 100;

    private final int kind;
    private final Random fx = new Random();
    private long seenId = Long.MIN_VALUE;
    private int spin;
    private int ticks;

    public CasinoScreen(CasinoContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.kind = container.getKind();
        this.xSize = 240;
        this.ySize = 176;
        CompoundNBT last = ClientAccountData.get().getCompound("casino");
        if (last.contains("id")) seenId = last.getLong("id");
    }

    private static CompoundNBT d() {
        return ClientAccountData.get();
    }

    private Button btn(int x, int y, int w, String text, Button.IPressable onPress) {
        return this.addButton(new Button(guiLeft + x, guiTop + y, w, 16, new StringTextComponent(text), onPress));
    }

    private int maxBet() {
        int m = d().getInt("casinoMaxBet");
        return m > 0 ? m : 1000;
    }

    private void play(int choice) {
        if (spin > 0) return;
        Network.CHANNEL.sendToServer(new ActionPacket(Actions.CASINO_PLAY, bet, choice));
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
    public void tick() {
        super.tick();
        ticks++;
        CompoundNBT last = d().getCompound("casino");
        if (last.contains("id") && last.getLong("id") != seenId && last.getInt("kind") == kind) {
            seenId = last.getLong("id");
            spin = SPIN_TICKS;
        }
        if (spin > 0) spin--;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);
        super.render(ms, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(ms, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack ms, float partialTicks, int mouseX, int mouseY) {
        int x = guiLeft, y = guiTop;
        int border = kind == CasinoService.ANARCHY ? 0xFFB71C1C : 0xFFC9A227;
        fill(ms, x - 2, y - 2, x + xSize + 2, y + ySize + 2, border);
        fill(ms, x, y, x + xSize, y + ySize, 0xFF140B1E);
        fill(ms, x + 4, y + 30, x + xSize - 4, y + 92, kind == CasinoService.ANARCHY ? 0xFF2A0A0A : 0xFF0D3B1E);
    }

    private void text(MatrixStack ms, String s, int x, int y, int color) {
        this.font.drawString(ms, s, x, y, color);
    }

    private void center(MatrixStack ms, String s, int cx, int y, int color) {
        this.font.drawString(ms, s, cx - this.font.getStringWidth(s) / 2f, y, color);
    }

    private void small(MatrixStack ms, String s, int x, int y, int color) {
        ms.push();
        ms.translate(x, y, 0);
        ms.scale(0.75F, 0.75F, 1.0F);
        this.font.drawString(ms, s, 0, 0, color);
        ms.pop();
    }

    private void box(MatrixStack ms, int x, int y, int w, int h, int color) {
        fill(ms, x - 1, y - 1, x + w + 1, y + h + 1, 0xFFC9A227);
        fill(ms, x, y, x + w, y + h, color);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack ms, int mouseX, int mouseY) {
        CompoundNBT t = d();
        CompoundNBT last = t.getCompound("casino");
        boolean mine = last.contains("id") && last.getInt("kind") == kind;
        center(ms, "§6§l" + CasinoService.TITLES[Math.max(0, Math.min(3, kind))], xSize / 2, 6, 0xFFFFFF);
        small(ms, "Счёт: §e" + Finance.money(t.getDouble("balance")) + "§r   Наличные: §e" + Finance.money(t.getLong("cash"))
                + "§r   Сегодня: " + (t.getDouble("casinoProfit") >= 0 ? "§a+" : "§c") + Finance.money(t.getDouble("casinoProfit")), 8, 19, 0xCCCCCC);
        int cx = xSize / 2;
        switch (kind) {
            case CasinoService.COIN: drawCoin(ms, last, mine, cx); break;
            case CasinoService.SLOTS: drawSlots(ms, last, mine, cx); break;
            case CasinoService.ANARCHY: drawAnarchy(ms, last, mine, cx); break;
            default: drawRoulette(ms, last, mine, cx); break;
        }
        // итог последней игры
        if (mine && spin == 0) {
            double mult = last.getDouble("mult");
            String res = mult > 1 ? "§a§lВЫИГРЫШ " + Finance.money(last.getDouble("win")) + " (x" + fmt(mult) + ")"
                    : mult == 1 ? "§eСтавка возвращена" : "§cПроигрыш " + Finance.money(last.getInt("bet"));
            center(ms, res + (last.getBoolean("clover") ? " §2(клевер)" : ""), cx, 94, 0xFFFFFF);
        } else if (spin > 0) {
            center(ms, "§7. . .", cx, 94, 0xFFFFFF);
        }
        String b = "Ставка: §e" + Finance.money(bet);
        text(ms, b, 8, 103, 0xFFFFFF);
        small(ms, "§8макс " + Finance.money(maxBet()), 170, 105, 0x777777);
        if (t.getBoolean("casinoBanned")) small(ms, "§4Охрана закрыла вам вход", 158, 158, 0xFFFFFF);
        else small(ms, "§8Охрана следит за везунчиками", 158, 158, 0x777777);
    }

    private static String fmt(double m) {
        return m == Math.floor(m) ? String.valueOf((int) m) : String.valueOf(m);
    }

    // ------------------------------------------------------------------ игры

    private void drawCoin(MatrixStack ms, CompoundNBT last, boolean mine, int cx) {
        int side;
        if (spin > 0) side = (ticks / 3) % 2;
        else side = mine ? last.getInt("side") : -1;
        int color = side == 0 ? 0xFFD4AF37 : side == 1 ? 0xFFC0C0C0 : 0xFF555555;
        int r = spin > 0 ? 14 + (int) (8 * Math.abs(Math.sin(ticks * 0.6))) : 22;
        box(ms, cx - r, 40, r * 2, 44, color);
        center(ms, side == 0 ? "§0§lОРЁЛ" : side == 1 ? "§0§lРЕШКА" : "§7?", cx, 58, 0xFFFFFF);
        small(ms, "§7Угадал — x2. Шанс 47.5%", 8, 84, 0xAAAAAA);
    }

    private void drawSlots(MatrixStack ms, CompoundNBT last, boolean mine, int cx) {
        int[] reels = mine ? last.getIntArray("reels") : new int[0];
        for (int i = 0; i < 3; i++) {
            int bx = cx - 75 + i * 52;
            box(ms, bx, 38, 46, 40, 0xFF1A1A1A);
            int sym;
            boolean rolling = spin > 0 && spin > (2 - i) * 6;
            if (rolling) sym = fx.nextInt(CasinoService.SYMBOLS.length);
            else sym = reels.length == 3 ? reels[i] : -1;
            if (sym >= 0) {
                String s = CasinoService.SYMBOLS[sym];
                center(ms, (sym == 0 ? "§l" : "") + s, bx + 23, 54, CasinoService.SYMBOL_COLORS[sym]);
            } else {
                center(ms, "§8?", bx + 23, 54, 0xFFFFFF);
            }
        }
        small(ms, "§7777 — x25, три одинаковых — x8, два — ставка назад", 8, 84, 0xAAAAAA);
    }

    private void drawAnarchy(MatrixStack ms, CompoundNBT last, boolean mine, int cx) {
        int n = CasinoService.ANARCHY_MULT.length;
        int idx = spin > 0 ? fx.nextInt(n) : (mine ? last.getInt("sector") : -1);
        for (int i = 0; i < n; i++) {
            int bx = cx - 108 + i * 36;
            boolean on = i == idx;
            box(ms, bx, 40, 32, 22, on ? 0xFFB71C1C : 0xFF301010);
            center(ms, (on ? "§f§l" : "§7") + "x" + fmt(CasinoService.ANARCHY_MULT[i]), bx + 16, 47, 0xFFFFFF);
        }
        if (spin > 0) {
            String[] chaos = {"Х А О С", "Б У Н Т", "А Н А Р Х И Я", "!!!"};
            center(ms, "§c§l" + chaos[(ticks / 4) % chaos.length], cx, 70, 0xFFFFFF);
        } else if (mine) {
            center(ms, "§c" + CasinoService.ANARCHY_TEXT[Math.max(0, Math.min(n - 1, idx))], cx, 70, 0xFFFFFF);
        } else {
            center(ms, "§7Хаос решает: x0 / x1 / x1.5 / x2 / x5 / x8", cx, 70, 0xFFFFFF);
        }
        small(ms, "§8Мама — анархия. Казино — папа.", 8, 84, 0x777777);
    }

    private void drawRoulette(MatrixStack ms, CompoundNBT last, boolean mine, int cx) {
        int len = CasinoService.WHEEL.length;
        int sector = mine ? last.getInt("sector") : 0;
        int offset = spin > 0 ? sector - spin * 2 : sector;
        for (int k = -3; k <= 3; k++) {
            int idx = ((offset + k) % len + len) % len;
            int m = CasinoService.WHEEL[idx];
            int bx = cx - 14 + k * 31;
            int col = m == 0 ? 0xFF222222 : m >= 10 ? 0xFFD4AF37 : m >= 3 ? 0xFF1565C0 : 0xFFB71C1C;
            box(ms, bx, 44, 28, 26, col);
            center(ms, (k == 0 ? "§f§l" : "§7") + "x" + m, bx + 14, 53, 0xFFFFFF);
        }
        center(ms, "§e§lV", cx, 33, 0xFFFFFF);
        small(ms, "§7100 секторов: x0 — 64, x1 — 20, x2 — 8, x3 — 4, x5 — 2, x10 и x25 — по 1", 8, 84, 0xAAAAAA);
    }
}
