package com.baraaoi.reallife.client;

import com.baraaoi.reallife.economy.BizType;
import com.baraaoi.reallife.entity.CustomerEntity;
import com.baraaoi.reallife.entity.VisitorEntity;
import com.baraaoi.reallife.entity.VisitorKind;
import com.baraaoi.reallife.entity.WorkerEntity;
import com.baraaoi.reallife.registry.ModContainers;
import com.baraaoi.reallife.registry.ModEntities;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Клиентская регистрация: окна и рендеры сущностей. */
public class ClientSetup {
    private static final Identifier[] CUSTOMER = new Identifier[CustomerEntity.VARIANTS];
    private static final Identifier[] VISITOR = new Identifier[VisitorKind.values().length];
    private static final Identifier[] WORKER = new Identifier[BizType.values().length];

    static {
        for (int i = 0; i < CUSTOMER.length; i++) CUSTOMER[i] = NpcRenderer.tex("customer/c" + i);
        for (VisitorKind k : VisitorKind.values()) VISITOR[k.ordinal()] = NpcRenderer.tex("visitor/" + k.id());
        for (BizType t : BizType.values()) WORKER[t.ordinal()] = NpcRenderer.tex("worker/" + t.id());
    }

    public static void register(net.minecraftforge.eventbus.api.bus.BusGroup modBus) {
        FMLClientSetupEvent.getBus(modBus).addListener(ClientSetup::init);
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(ClientSetup::renderers);
    }

    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModContainers.BANK.get(), FinanceScreen::new);
            MenuScreens.register(ModContainers.MFO.get(), FinanceScreen::new);
            MenuScreens.register(ModContainers.CASINO.get(), CasinoScreen::new);
            MenuScreens.register(ModContainers.PRINTER.get(), PrinterScreen::new);
            MenuScreens.register(ModContainers.BIZ.get(), BizScreen::new);
            MenuScreens.register(ModContainers.CRYPTO.get(), CryptoScreen::new);
        });
    }

    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.ZHORA.get(), m -> new NpcRenderer<>(m, "zhora"));
        e.registerEntityRenderer(ModEntities.ALEXANDER.get(), m -> new NpcRenderer<>(m, "alexander"));
        e.registerEntityRenderer(ModEntities.POLICE.get(), m -> new NpcRenderer<>(m, "police"));
        e.registerEntityRenderer(ModEntities.VOVAN.get(), m -> new NpcRenderer<>(m, "vovan"));
        e.registerEntityRenderer(ModEntities.BRATOK.get(), m -> new NpcRenderer<>(m, "bratok"));
        e.registerEntityRenderer(ModEntities.INSPECTOR.get(), m -> new NpcRenderer<>(m, "inspector"));
        e.registerEntityRenderer(ModEntities.SELLER.get(), m -> new NpcRenderer<>(m, "seller"));
        e.registerEntityRenderer(ModEntities.CASHIER.get(), m -> new NpcRenderer<>(m, "cashier"));
        e.registerEntityRenderer(ModEntities.AMBAL.get(), m -> new NpcRenderer<>(m, "ambal"));
        e.registerEntityRenderer(ModEntities.SNOOP.get(), m -> new NpcRenderer<>(m, "visitor/snoop"));
        e.registerEntityRenderer(ModEntities.VISITOR.get(), m -> new NpcRenderer<VisitorEntity>(m, v -> VISITOR[v.getKind().ordinal()]));
        e.registerEntityRenderer(ModEntities.WORKER.get(), m -> new NpcRenderer<WorkerEntity>(m, w -> WORKER[w.getBizType().ordinal()]));
        e.registerEntityRenderer(ModEntities.CUSTOMER.get(), m -> new NpcRenderer<CustomerEntity>(m, c -> CUSTOMER[c.getVariant()]));
    }
}
