package com.baraaoi.reallife.client;

import com.baraaoi.reallife.container.BizContainer;
import com.baraaoi.reallife.economy.Actions;
import com.baraaoi.reallife.economy.BizType;
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

/** Касса легального заведения: найм работника, статистика, отмыв выручки. */
public class BizScreen extends ContainerScreen<BizContainer> {
    public BizScreen(BizContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.xSize = BizContainer.WIDTH;
        this.ySize = BizContainer.HEIGHT;
    }

    private void send(int action) {
        Network.CHANNEL.sendToServer(new ActionPacket(action, 0, 0));
    }

    @Override
    protected void init() {
        super.init();
        this.addButton(new Button(guiLeft + 8, guiTop + 92, 110, 16, new StringTextComponent("Нанять работника"), b -> send(Actions.BIZ_HIRE)));
        this.addButton(new Button(guiLeft + 8, guiTop + 114, 150, 16, new StringTextComponent("§2Провести выручку из слота"), b -> send(Actions.BIZ_LAUNDER)));
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);
        super.render(ms, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(ms, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack ms, float partialTicks, int mouseX, int mouseY) {
        GuiUtil.panel(ms, guiLeft, guiTop, xSize, ySize, 0xFF3C8D40, 0xFF18241A);
        GuiUtil.slot(ms, guiLeft + BizContainer.SLOT_X, guiTop + BizContainer.SLOT_Y);
        GuiUtil.inventory(ms, guiLeft + BizContainer.INV_X, guiTop + BizContainer.INV_Y);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack ms, int mouseX, int mouseY) {
        CompoundNBT all = ClientAccountData.get();
        CompoundNBT t = all.getCompound("openBiz");
        BizType type = BizType.byId(t.getInt("type"));
        String title = type == null ? this.title.getString() : type.title;
        this.font.drawString(ms, "§a§l" + title, 8, 7, 0xFFFFFF);
        if (type == null) return;
        boolean worker = t.getBoolean("worker");
        GuiUtil.small(font, ms, worker ? "§a● Открыто — " + type.workerName + " на смене" : "§c● Закрыто — нет работника", 8, 20);
        GuiUtil.small(font, ms, "Базовая прибыль: §a" + Finance.money(type.base) + "§f/день, зарплата " + Finance.money(type.wage()) + "/день", 8, 30);
        double lp = t.getDouble("lastProfit");
        GuiUtil.small(font, ms, "Вчера: " + (lp >= 0 ? "§a+" : "§c") + Finance.money(lp) + " §7" + t.getString("lastNote"), 8, 40);
        GuiUtil.small(font, ms, "Всего заработано: §a" + Finance.money(t.getDouble("total")) + "§f за " + t.getInt("days") + " дн.", 8, 50);
        GuiUtil.small(font, ms, "§7Прибыль зависит от удачи (зелье удачи помогает) и падает до нуля без работника.", 8, 60);
        GuiUtil.small(font, ms, "Найм: " + Finance.money(type.hireCost()) + ". Если работника убьют — нанимай снова.", 8, 80);
        double safe = t.getDouble("safe");
        double done = t.getDouble("laundered");
        GuiUtil.small(font, ms, "Отмыто сегодня: " + (done > safe ? "§c" : "§a") + Finance.money(done) + "§f / безопасно " + Finance.money(safe)
                + " §7(налог " + Finance.percent(all.getDouble("launderTax")) + ")", 8, 134 - 2);
        if (done >= safe) GuiUtil.small(font, ms, "§cСверх лимита — шанс налоговой проверки растёт!", 120, 98);
        GuiUtil.small(font, ms, "Грязные", BizContainer.SLOT_X - 6, BizContainer.SLOT_Y - 9);
    }
}
