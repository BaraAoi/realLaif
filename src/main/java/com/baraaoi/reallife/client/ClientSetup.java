package com.baraaoi.reallife.client;

import com.baraaoi.reallife.registry.ModContainers;
import com.baraaoi.reallife.registry.ModEntities;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientSetup {
    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ScreenManager.registerFactory(ModContainers.BANK.get(), FinanceScreen::new);
            ScreenManager.registerFactory(ModContainers.MFO.get(), FinanceScreen::new);
            ScreenManager.registerFactory(ModContainers.CASINO.get(), CasinoScreen::new);
            ScreenManager.registerFactory(ModContainers.PRINTER.get(), PrinterScreen::new);
            ScreenManager.registerFactory(ModContainers.BIZ.get(), BizScreen::new);
            ScreenManager.registerFactory(ModContainers.CRYPTO.get(), CryptoScreen::new);
        });
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.ZHORA.get(), m -> new NpcRenderer<>(m, "zhora"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.ALEXANDER.get(), m -> new NpcRenderer<>(m, "alexander"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.POLICE.get(), m -> new NpcRenderer<>(m, "police"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.VOVAN.get(), m -> new NpcRenderer<>(m, "vovan"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.BRATOK.get(), m -> new NpcRenderer<>(m, "bratok"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.INSPECTOR.get(), m -> new NpcRenderer<>(m, "inspector"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.SELLER.get(), m -> new NpcRenderer<>(m, "seller"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.CASHIER.get(), m -> new NpcRenderer<>(m, "cashier"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.AMBAL.get(), m -> new NpcRenderer<>(m, "ambal"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.VISITOR.get(), VisitorRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.WORKER.get(), WorkerRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.CUSTOMER.get(), CustomerRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.SNOOP.get(), m -> new NpcRenderer<>(m, "visitor/snoop"));
    }
}
