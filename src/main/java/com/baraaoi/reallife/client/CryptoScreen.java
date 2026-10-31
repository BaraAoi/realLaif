package com.baraaoi.reallife.client;

import com.baraaoi.reallife.container.CryptoContainer;
import com.baraaoi.reallife.economy.Actions;
import com.baraaoi.reallife.economy.Finance;
import com.baraaoi.reallife.network.ActionPacket;
import com.baraaoi.reallife.network.Network;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Майнинг-ферма: корпус со слотами комплектующих. */
public class CryptoScreen extends AbstractContainerScreen<CryptoContainer> {
    private static final String[] LABELS = {"Плата", "CPU", "RAM", "БП", "Кулер", "GPU", "GPU", "GPU", "GPU"};

    public CryptoScreen(CryptoContainer container, Inventory inv, Component title) {
        super(container, inv, title, 176, CryptoContainer.INV_Y + 82);
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("§eПеревести в кошелёк"), b -> Network.toServer(new ActionPacket(Actions.CRYPTO_WITHDRAW, 0, 0))).bounds(leftPos + 8, topPos + 88, 160, 16).build());
    }


    @Override
    public void extractBackground(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(ms, mouseX, mouseY, partialTicks);
        GuiUtil.panel(ms, leftPos, topPos, imageWidth, imageHeight, 0xFF707070, 0xFF202226);
        // корпус: материнка слева, видеокарты справа
        ms.fill(leftPos + 12, topPos + 20, leftPos + 90, topPos + 80, 0xFF1F4D2E);
        ms.fill(leftPos + 94, topPos + 20, leftPos + 148, topPos + 80, 0xFF2B2B40);
        for (int[] s : CryptoContainer.SLOTS) GuiUtil.slot(ms, leftPos + s[0], topPos + s[1]);
        GuiUtil.inventory(ms, leftPos + 8, topPos + CryptoContainer.INV_Y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ms, int mouseX, int mouseY) {
        CompoundTag all = ClientAccountData.get();
        CompoundTag r = all.getCompoundOrEmpty("rig");
        GuiUtil.text(ms, this.font, "§eМайнинг-ферма", 8, 6, 0xFFFFFF);
        for (int i = 0; i < LABELS.length; i++) {
            GuiUtil.small(font, ms, "§7" + LABELS[i], CryptoContainer.SLOTS[i][0] - 1, CryptoContainer.SLOTS[i][1] - 7);
        }
        boolean ok = r.getBooleanOr("complete", false);
        int gpus = r.getIntOr("gpus", 0);
        GuiUtil.small(font, ms, ok ? "§a● Майнит" : "§c● Стоит", 152, 24);
        GuiUtil.small(font, ms, "§7GPU: " + gpus + "/4", 152, 34);
        if (!ok && !r.getStringOr("missing", "").isEmpty()) {
            GuiUtil.small(font, ms, "§cНе хватает: " + r.getStringOr("missing", ""), 8, 82 - 1);
        } else {
            double perHour = r.getDoubleOr("rate", 0.0) * 3600;
            GuiUtil.small(font, ms, String.format(Locale.ROOT, "§7%.6f ₿/час ≈ ", perHour) + Finance.money(perHour * all.getDoubleOr("btcRate", 0.0)) + "/час", 8, 81);
        }
        double mined = r.getDoubleOr("mined", 0.0);
        GuiUtil.small(font, ms, String.format(Locale.ROOT, "Намайнено: §e%.6f ₿ §f≈ ", mined) + Finance.money(mined * all.getDoubleOr("btcRate", 0.0)), 8, 106);
    }
}
