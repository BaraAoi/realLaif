package com.baraaoi.reallife;

import com.baraaoi.reallife.event.ForgeEvents;
import com.baraaoi.reallife.network.Network;
import com.baraaoi.reallife.registry.ModBlocks;
import com.baraaoi.reallife.registry.ModContainers;
import com.baraaoi.reallife.registry.ModEntities;
import com.baraaoi.reallife.registry.ModItems;
import com.baraaoi.reallife.registry.ModTileEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Real Life — суровая реальная жизнь: банк, кредиты, ипотека, коллекторы, полиция, МФО.
 * Версия 2.x — порт на Minecraft 26.3 / Forge 26.3.
 */
@Mod(RealLife.MODID)
public final class RealLife {
    public static final String MODID = "reallife";
    public static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("RealLife");

    /** Версия мода (из mods.toml / gradle.properties: mod_version). */
    public static String version() {
        return net.minecraftforge.fml.ModList.getModContainerById(MODID)
                .map(c -> c.getModInfo().getVersion().toString()).orElse("?");
    }

    public RealLife(FMLJavaModLoadingContext context) {
        var modBus = context.getModBusGroup();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModTileEntities.TILES.register(modBus);
        ModContainers.CONTAINERS.register(modBus);
        Network.register();
        EntityAttributeCreationEvent.BUS.addListener(ModEntities::registerAttributes);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            com.baraaoi.reallife.client.ClientSetup.register(modBus);
        }
        ForgeEvents.register();
        SelfTest.register();
    }
}
