package com.baraaoi.reallife;

import com.baraaoi.reallife.client.ClientSetup;
import com.baraaoi.reallife.event.ForgeEvents;
import com.baraaoi.reallife.network.Network;
import com.baraaoi.reallife.registry.ModBlocks;
import com.baraaoi.reallife.registry.ModContainers;
import com.baraaoi.reallife.registry.ModEntities;
import com.baraaoi.reallife.registry.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Real Life — суровая реальная жизнь: банк, кредиты, ипотека, коллекторы, полиция, МФО.
 */
@Mod(RealLife.MODID)
public class RealLife {
    public static final String MODID = "reallife";

    public RealLife() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModContainers.CONTAINERS.register(bus);
        bus.addListener(this::commonSetup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(ClientSetup::init));

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
        MinecraftForge.EVENT_BUS.register(new SelfTest());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        Network.register();
        event.enqueueWork(ModEntities::registerAttributes);
    }
}
