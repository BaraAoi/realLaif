package com.baraaoi.reallife.client;

import com.baraaoi.reallife.container.BizContainer;
import com.baraaoi.reallife.economy.Actions;
import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.network.ActionPacket;
import com.baraaoi.reallife.network.Network;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Касса легального заведения: найм работника, статистика, отмыв выручки. */
public class BizScreen extends AbstractContainerScreen<BizContainer> {
    public BizScreen(BizContainer container, Inventory inv, Component title) {
        super(container, inv, title, BizContainer.WIDTH, BizContainer.HEIGHT);
    }

    private void send(int action) {
        Network.toServer(new ActionPacket(action, 0, 0));
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("§aНанять (+1)"), b -> send(Actions.BIZ_HIRE)).bounds(leftPos + 8, topPos + 94, 72, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("§cУволить"), b -> send(Actions.BIZ_FIRE)).bounds(leftPos + 84, topPos + 94, 56, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("§eДокументы"), b -> send(Actions.BIZ_DOCS)).bounds(leftPos + 144, topPos + 94, 78, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("§2Провести выручку из слота"), b -> send(Actions.BIZ_LAUNDER)).bounds(leftPos + 8, topPos + 114, 150, 16).build());
    }


    @Override
    public void extractBackground(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(ms, mouseX, mouseY, partialTicks);
        GuiUtil.panel(ms, leftPos, topPos, imageWidth, imageHeight, 0xFF3C8D40, 0xFF18241A);
        GuiUtil.slot(ms, leftPos + BizContainer.SLOT_X, topPos + BizContainer.SLOT_Y);
        GuiUtil.inventory(ms, leftPos + BizContainer.INV_X, topPos + BizContainer.INV_Y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ms, int mouseX, int mouseY) {
        CompoundTag all = ClientAccountData.get();
        CompoundTag t = all.getCompoundOrEmpty("openBiz");
        BizType type = BizType.byId(t.getIntOr("type", 0));
        String title = type == null ? this.title.getString() : type.title;
        GuiUtil.text(ms, this.font, "§a§l" + title, 8, 7, 0xFFFFFF);
        if (type == null) return;
        int workers = t.getIntOr("workers", 0), allowed = t.getIntOr("allowed", 0), sets = t.getIntOr("sets", 0);
        String reason = t.getStringOr("reason", "");
        GuiUtil.small(font, ms, (reason.isEmpty() ? "§a● Открыто" : "§c● Закрыто: " + reason) + "§f   работники §e" + workers + "§f/"
                + allowed + " §7(потолок " + type.maxWorkers + ")", 8, 19);
        GuiUtil.small(font, ms, (t.getBooleanOr("mall", false) ? "Оборудование в помещении — комплектов: §e" : "Оборудование в зоне ±8 — комплектов: §e") + sets + "§7 (" + (sets < type.maxWorkers ? "для " + (sets + 1)
                + "-го работника нужно:" : "максимум") + ")", 8, 28);
        net.minecraft.nbt.ListTag eq = t.getListOrEmpty("equip");
        int target = Math.min(type.maxWorkers, sets + 1);
        for (int i = 0; i < eq.size(); i++) {
            CompoundTag q = eq.getCompoundOrEmpty(i);
            int have = q.getIntOr("h", 0), need = q.getIntOr("b", 0) + q.getIntOr("q", 0) * target;
            GuiUtil.small(font, ms, (have >= need ? "§a" : "§c") + q.getStringOr("n", "") + " " + have + "/" + need, 8 + (i % 2) * 110, 37 + (i / 2) * 9);
        }
        int stock = t.getIntOr("stock", 0), needS = t.getIntOr("need", 0);
        GuiUtil.small(font, ms, "Сырьё («" + type.supplyName + "»): " + (stock >= needS && stock > 0 ? "§a" : "§c") + stock + "§f, расход "
                + needS + "/день §7(коробки по 1 в слоте; склад = запас)", 8, 56);
        double lp = t.getDoubleOr("lastProfit", 0.0);
        GuiUtil.small(font, ms, "Вчера: " + (lp >= 0 ? "§a+" : "§c") + Finance.money(lp) + " §7" + t.getStringOr("lastNote", ""), 8, 65);
        GuiUtil.small(font, ms, "Всего: §a" + Finance.money(t.getDoubleOr("total", 0.0)) + "§f за " + t.getIntOr("days", 0) + " раб. дн.  §7выручка "
                + Finance.money(type.revenue() * (t.getBooleanOr("mall", false) ? 1.5 : 1.0) * (t.contains("rivalFactor") ? t.getDoubleOr("rivalFactor", 0.0) : 1.0)) + "/раб."
                + (t.getBooleanOr("mall", false) ? " §d(ТЦ +50%)" : "")
                + (t.getIntOr("rivals", 0) > 0 ? " §c(конкуренты: " + t.getIntOr("rivals", 0) + ", −" + Math.round((1 - t.getDoubleOr("rivalFactor", 0.0)) * 100) + "%)" : t.getBooleanOr("mall", false) ? "" : ", удача влияет"), 8, 74);
        GuiUtil.small(font, ms, "Найм " + Finance.money(type.hireCost()) + ", зарплата " + Finance.money(type.wage()) + "/день каждому. "
                + (t.getIntOr("undoc", 0) > 0 ? "§cБез документов: " + t.getIntOr("undoc", 0) + " чел.!" : t.getIntOr("workers", 0) > 0
                ? "§aДокументы до дня " + t.getLongOr("docsUntil", 0L) : "§7Сырьё: телефон → Опт."), 8, 83);
        double safe = t.getDoubleOr("safe", 0.0);
        double done = t.getDoubleOr("laundered", 0.0);
        GuiUtil.small(font, ms, "Отмыто сегодня: " + (done > safe ? "§c" : "§a") + Finance.money(done) + "§f / безопасно " + Finance.money(safe)
                + " §7(" + Math.round((t.contains("safeShare") ? t.getDoubleOr("safeShare", 0.0) : 0.15) * 100) + "% вчерашней прибыли" + (t.getBooleanOr("mall", false) ? " — ТЦ" : "") + ", налог " + Finance.percent(all.getDoubleOr("launderTax", 0.0)) + ")", 8, 132);
        GuiUtil.small(font, ms, "Грязные", BizContainer.SLOT_X - 6, BizContainer.SLOT_Y - 9);
    }
}
