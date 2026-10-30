package com.baraaoi.reallife.client;

import com.baraaoi.reallife.container.PrinterContainer;
import com.baraaoi.reallife.economy.Actions;
import com.baraaoi.reallife.network.ActionPacket;
import com.baraaoi.reallife.network.Network;
import com.baraaoi.reallife.tile.PrinterTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

/** Печатный станок: выбор номинала, прогресс, лоток. */
public class PrinterScreen extends ContainerScreen<PrinterContainer> {
    private static final String[] TIMES = {"5 с", "1 мин", "5 мин", "10 мин", "50 мин"};

    public PrinterScreen(PrinterContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.xSize = 176;
        this.ySize = PrinterContainer.INV_Y + 82;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < PrinterTileEntity.DENOMS.length; i++) {
            final int idx = i;
            this.addButton(new Button(guiLeft + 6 + i * 33, guiTop + 18, 32, 20,
                    new StringTextComponent(PrinterTileEntity.DENOMS[i] + "₽"),
                    b -> Network.CHANNEL.sendToServer(new ActionPacket(Actions.PRINTER_SET, idx, 0))));
        }
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);
        super.render(ms, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(ms, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack ms, float partialTicks, int mouseX, int mouseY) {
        GuiUtil.panel(ms, guiLeft, guiTop, xSize, ySize, 0xFF2E7D32, 0xFF16201A);
        int sel = Math.max(0, Math.min(PrinterTileEntity.DENOMS.length - 1, container.denomIdx()));
        fill(ms, guiLeft + 5 + sel * 33, guiTop + 17, guiLeft + 39 + sel * 33, guiTop + 39, 0xFFFFD54F);
        // прогресс-бар
        int bx = guiLeft + 8, by = guiTop + 52, bw = 160;
        fill(ms, bx - 1, by - 1, bx + bw + 1, by + 7, 0xFF000000);
        fill(ms, bx, by, bx + bw, by + 6, 0xFF333333);
        int filled = bw * Math.max(0, Math.min(1000, container.permille())) / 1000;
        int color = container.status() == PrinterTileEntity.WORKING ? 0xFF4CAF50 : container.status() == PrinterTileEntity.FULL ? 0xFFFF9800 : 0xFF777777;
        fill(ms, bx, by, bx + filled, by + 6, color);
        for (int i = 0; i < PrinterTileEntity.SIZE; i++) GuiUtil.slot(ms, guiLeft + 8 + i * 18, guiTop + PrinterContainer.SLOT_Y);
        GuiUtil.inventory(ms, guiLeft + 8, guiTop + PrinterContainer.INV_Y);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack ms, int mouseX, int mouseY) {
        this.font.drawString(ms, "§2Печатный станок", 8, 6, 0xFFFFFF);
        int sel = Math.max(0, Math.min(PrinterTileEntity.DENOMS.length - 1, container.denomIdx()));
        GuiUtil.small(font, ms, "§7" + PrinterTileEntity.DENOMS[sel] + "₽ за " + TIMES[sel], 100, 6);
        String st;
        switch (container.status()) {
            case PrinterTileEntity.WORKING: {
                int s = container.secondsLeft();
                st = "§aПечатает… §f" + (s >= 60 ? s / 60 + " мин " + s % 60 + " с" : s + " с");
                break;
            }
            case PrinterTileEntity.FULL: st = "§6Некуда класть — освободи сундук цеха / лоток"; break;
            default: st = "§cНе подключён — ставь дома или на своём участке"; break;
        }
        GuiUtil.small(font, ms, st, 8, 42);
        GuiUtil.small(font, ms, "§7Напечатано всего: " + container.printedHundreds() * 100 + "₽+   Лоток:", 8, 62);
        GuiUtil.small(font, ms, "§8Поставлен «Сундук цеха» — печать идёт в него.", 8, PrinterContainer.SLOT_Y + 20);
    }
}
