package com.baraaoi.reallife.registry;

import com.baraaoi.reallife.container.CasinoContainer;

import com.baraaoi.reallife.RealLife;
import com.baraaoi.reallife.container.FinanceContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModContainers {
    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, RealLife.MODID);

    public static final RegistryObject<ContainerType<FinanceContainer>> BANK = CONTAINERS.register("bank",
            () -> IForgeContainerType.create((id, inv, buf) -> new FinanceContainer(id, inv, buf.readBlockPos(), false)));

    public static final RegistryObject<ContainerType<FinanceContainer>> MFO = CONTAINERS.register("mfo",
            () -> IForgeContainerType.create((id, inv, buf) -> new FinanceContainer(id, inv, buf.readBlockPos(), true)));

    public static final RegistryObject<ContainerType<CasinoContainer>> CASINO = CONTAINERS.register("casino",
            () -> IForgeContainerType.create((id, inv, buf) -> new CasinoContainer(id, inv, buf.readBlockPos(), buf.readVarInt())));
}
