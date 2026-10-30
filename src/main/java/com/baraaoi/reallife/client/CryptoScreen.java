package com.baraaoi.reallife.client;

import com.baraaoi.reallife.container.CryptoContainer;
import com.baraaoi.reallife.economy.Actions;
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

import java.util.Locale;

/** Майнинг-ферма: корпус со слотами комплектующих. */
public class CryptoScreen extends ContainerScreen<CryptoContainer> {
    private static final String[] LABELS = {"Плата", "CPU", "RAM", "БП", "Кулер", "GPU", "GPU", "GPU", "GPU"};

    public CryptoScreen(CryptoContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.xSize = 176;
        this.ySize = CryptoContainer.INV_Y + 82;
    }

    @Override
    protected void init() {
        super.init();
        this.addButton(new Button(guiLeft + 8, guiTop + 88, 160, 16, new StringTextComponent("§eПеревести в кошелёк"),
                b -> Network.CHANNEL.sendToServer(new ActionPacket(Actions.CRYPTO_WITHDRAW, 0, 0))));
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);
        super.render(ms, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(ms, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack ms, float partialTicks, int mouseX, int mouseY) {
        GuiUtil.panel(ms, guiLeft, guiTop, xSize, ySize, 0xFF707070, 0xFF202226);
        // корпус: материнка слева, видеокарты справа
        fill(ms, guiLeft + 12, guiTop + 20, guiLeft + 90, guiTop + 80, 0xFF1F4D2E);
        fill(ms, guiLeft + 94, guiTop + 20, guiLeft + 148, guiTop + 80, 0xFF2B2B40);
        for (int[] s : CryptoContainer.SLOTS) GuiUtil.slot(ms, guiLeft + s[0], guiTop + s[1]);
        GuiUtil.inventory(ms, guiLeft + 8, guiTop + CryptoContainer.INV_Y);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack ms, int mouseX, int mouseY) {
        CompoundNBT all = ClientAccountData.get();
        CompoundNBT r = all.getCompound("rig");
        this.font.drawString(ms, "§eМайнинг-ферма", 8, 6, 0xFFFFFF);
        for (int i = 0; i < LABELS.length; i++) {
            GuiUtil.small(font, ms, "§7" + LABELS[i], CryptoContainer.SLOTS[i][0] - 1, CryptoContainer.SLOTS[i][1] - 7);
        }
        boolean ok = r.getBoolean("complete");
        int gpus = r.getInt("gpus");
        GuiUtil.small(font, ms, ok ? "§a● Майнит" : "§c● Стоит", 152, 24);
        GuiUtil.small(font, ms, "§7GPU: " + gpus + "/4", 152, 34);
        if (!ok && !r.getString("missing").isEmpty()) {
            GuiUtil.small(font, ms, "§cНе хватает: " + r.getString("missing"), 8, 82 - 1);
        } else {
            double perHour = r.getDouble("rate") * 3600;
            GuiUtil.small(font, ms, String.format(Locale.ROOT, "§7%.6f ₿/час ≈ ", perHour) + Finance.money(perHour * all.getDouble("btcRate")) + "/час", 8, 81);
        }
        double mined = r.getDouble("mined");
        GuiUtil.small(font, ms, String.format(Locale.ROOT, "Намайнено: §e%.6f ₿ §f≈ ", mined) + Finance.money(mined * all.getDouble("btcRate")), 8, 106);
    }
}
