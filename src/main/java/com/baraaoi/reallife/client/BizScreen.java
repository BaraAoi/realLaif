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
        this.addButton(new Button(guiLeft + 8, guiTop + 94, 72, 16, new StringTextComponent("§aНанять (+1)"), b -> send(Actions.BIZ_HIRE)));
        this.addButton(new Button(guiLeft + 84, guiTop + 94, 56, 16, new StringTextComponent("§cУволить"), b -> send(Actions.BIZ_FIRE)));
        this.addButton(new Button(guiLeft + 144, guiTop + 94, 78, 16, new StringTextComponent("§eДокументы"), b -> send(Actions.BIZ_DOCS)));
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
        int workers = t.getInt("workers"), allowed = t.getInt("allowed"), sets = t.getInt("sets");
        String reason = t.getString("reason");
        GuiUtil.small(font, ms, (reason.isEmpty() ? "§a● Открыто" : "§c● Закрыто: " + reason) + "§f   работники §e" + workers + "§f/"
                + allowed + " §7(потолок " + type.maxWorkers + ")", 8, 19);
        GuiUtil.small(font, ms, "Оборудование в зоне ±8 — комплектов: §e" + sets + "§7 (" + (sets < type.maxWorkers ? "для " + (sets + 1)
                + "-го работника нужно:" : "максимум") + ")", 8, 28);
        net.minecraft.nbt.ListNBT eq = t.getList("equip", 10);
        int target = Math.min(type.maxWorkers, sets + 1);
        for (int i = 0; i < eq.size(); i++) {
            CompoundNBT q = eq.getCompound(i);
            int have = q.getInt("h"), need = q.getInt("b") + q.getInt("q") * target;
            GuiUtil.small(font, ms, (have >= need ? "§a" : "§c") + q.getString("n") + " " + have + "/" + need, 8 + (i % 2) * 110, 37 + (i / 2) * 9);
        }
        int stock = t.getInt("stock"), needS = t.getInt("need");
        GuiUtil.small(font, ms, "Сырьё («" + type.supplyName + "»): " + (stock >= needS && stock > 0 ? "§a" : "§c") + stock + "§f, расход "
                + needS + "/день §7(коробки по 1 в слоте; склад = запас)", 8, 56);
        double lp = t.getDouble("lastProfit");
        GuiUtil.small(font, ms, "Вчера: " + (lp >= 0 ? "§a+" : "§c") + Finance.money(lp) + " §7" + t.getString("lastNote"), 8, 65);
        GuiUtil.small(font, ms, "Всего: §a" + Finance.money(t.getDouble("total")) + "§f за " + t.getInt("days") + " раб. дн.  §7выручка "
                + Finance.money(type.base) + "/раб., удача влияет", 8, 74);
        GuiUtil.small(font, ms, "Найм " + Finance.money(type.hireCost()) + ", зарплата " + Finance.money(type.wage()) + "/день каждому. "
                + (t.getInt("undoc") > 0 ? "§cБез документов: " + t.getInt("undoc") + " чел.!" : t.getInt("workers") > 0
                ? "§aДокументы до дня " + t.getLong("docsUntil") : "§7Сырьё: телефон → Опт."), 8, 83);
        double safe = t.getDouble("safe");
        double done = t.getDouble("laundered");
        GuiUtil.small(font, ms, "Отмыто сегодня: " + (done > safe ? "§c" : "§a") + Finance.money(done) + "§f / безопасно " + Finance.money(safe)
                + " §7(15% вчерашней прибыли, налог " + Finance.percent(all.getDouble("launderTax")) + ")", 8, 132);
        GuiUtil.small(font, ms, "Грязные", BizContainer.SLOT_X - 6, BizContainer.SLOT_Y - 9);
    }
}
