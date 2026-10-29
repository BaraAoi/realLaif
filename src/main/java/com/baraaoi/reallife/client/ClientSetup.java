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
        });
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.ZHORA.get(), m -> new NpcRenderer<>(m, "zhora"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.ALEXANDER.get(), m -> new NpcRenderer<>(m, "alexander"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.POLICE.get(), m -> new NpcRenderer<>(m, "police"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.VOVAN.get(), m -> new NpcRenderer<>(m, "vovan"));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.BRATOK.get(), m -> new NpcRenderer<>(m, "bratok"));
    }
}
