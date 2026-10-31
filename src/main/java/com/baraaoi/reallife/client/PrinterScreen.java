package com.baraaoi.reallife.client;

import com.baraaoi.reallife.container.PrinterContainer;
import com.baraaoi.reallife.economy.Actions;
import com.baraaoi.reallife.network.ActionPacket;
import com.baraaoi.reallife.network.Network;
import com.baraaoi.reallife.tile.PrinterTileEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

/** Печатный станок: выбор номинала, прогресс, лоток. */
public class PrinterScreen extends AbstractContainerScreen<PrinterContainer> {
    private static final String[] TIMES = {"5 с", "1 мин", "5 мин", "10 мин", "50 мин"};

    public PrinterScreen(PrinterContainer container, Inventory inv, Component title) {
        super(container, inv, title, 176, PrinterContainer.INV_Y + 82);
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < PrinterTileEntity.DENOMS.length; i++) {
            final int idx = i;
            this.addRenderableWidget(Button.builder(Component.literal(PrinterTileEntity.DENOMS[i] + "₽"), b -> Network.toServer(new ActionPacket(Actions.PRINTER_SET, idx, 0))).bounds(leftPos + 6 + i * 33, topPos + 18, 32, 20).build());
        }
    }


    @Override
    public void extractBackground(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(ms, mouseX, mouseY, partialTicks);
        GuiUtil.panel(ms, leftPos, topPos, imageWidth, imageHeight, 0xFF2E7D32, 0xFF16201A);
        int sel = Math.max(0, Math.min(PrinterTileEntity.DENOMS.length - 1, menu.denomIdx()));
        ms.fill(leftPos + 5 + sel * 33, topPos + 17, leftPos + 39 + sel * 33, topPos + 39, 0xFFFFD54F);
        // прогресс-бар
        int bx = leftPos + 8, by = topPos + 52, bw = 160;
        ms.fill(bx - 1, by - 1, bx + bw + 1, by + 7, 0xFF000000);
        ms.fill(bx, by, bx + bw, by + 6, 0xFF333333);
        int filled = bw * Math.max(0, Math.min(1000, menu.permille())) / 1000;
        int color = menu.status() == PrinterTileEntity.WORKING ? 0xFF4CAF50 : menu.status() == PrinterTileEntity.FULL ? 0xFFFF9800 : 0xFF777777;
        ms.fill(bx, by, bx + filled, by + 6, color);
        for (int i = 0; i < PrinterTileEntity.SIZE; i++) GuiUtil.slot(ms, leftPos + 8 + i * 18, topPos + PrinterContainer.SLOT_Y);
        GuiUtil.inventory(ms, leftPos + 8, topPos + PrinterContainer.INV_Y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ms, int mouseX, int mouseY) {
        GuiUtil.text(ms, this.font, "§2Печатный станок", 8, 6, 0xFFFFFF);
        int sel = Math.max(0, Math.min(PrinterTileEntity.DENOMS.length - 1, menu.denomIdx()));
        GuiUtil.small(font, ms, "§7" + PrinterTileEntity.DENOMS[sel] + "₽ за " + TIMES[sel], 100, 6);
        String st;
        switch (menu.status()) {
            case PrinterTileEntity.WORKING: {
                int s = menu.secondsLeft();
                st = "§aПечатает… §f" + (s >= 60 ? s / 60 + " мин " + s % 60 + " с" : s + " с");
                break;
            }
            case PrinterTileEntity.FULL: st = "§6Некуда класть — освободи сундук цеха / лоток"; break;
            default: st = "§cНе подключён — ставь дома или на своём участке"; break;
        }
        GuiUtil.small(font, ms, st, 8, 42);
        GuiUtil.small(font, ms, "§7Напечатано всего: " + menu.printedHundreds() * 100 + "₽+   Лоток:", 8, 62);
        GuiUtil.small(font, ms, "§8Поставлен «Сундук цеха» — печать идёт в него.", 8, PrinterContainer.SLOT_Y + 20);
    }
}
